package com.streamflix.extension

import android.content.Context
import com.google.gson.Gson
import com.streamflix.extension.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Extension Loader - Manages loading and caching of extensions
 * 
 * Supports loading from:
 * - GitHub repositories
 * - Local files
 * - Built-in assets
 */
class ExtensionLoaderImpl(
    private val context: Context,
    private val gson: Gson
) : ExtensionLoader {

    private val loadedExtensions = mutableMapOf<String, LoadedExtension>()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val EXTENSION_CACHE_DIR = "extensions"
        private const val MANIFEST_FILE = "manifest.json"
    }

    override fun loadExtension(source: ExtensionSource): LoadedExtension {
        return when (source) {
            is ExtensionSource.GitHub -> loadFromGitHub(source)
            is ExtensionSource.Local -> loadFromLocal(source)
            is ExtensionSource.BuiltIn -> loadFromAssets(source)
        }
    }

    override fun unloadExtension(extensionId: String) {
        loadedExtensions.remove(extensionId)
        Timber.d("Unloaded extension: $extensionId")
    }

    override fun getLoadedExtensions(): List<LoadedExtension> {
        return loadedExtensions.values.toList()
    }

    /**
     * Load extension from GitHub repository
     */
    private fun loadFromGitHub(source: ExtensionSource.GitHub): LoadedExtension {
        // For GitHub, we expect a raw JSON manifest URL or repo structure
        val manifestUrl = if (source.repoUrl.endsWith(".json")) {
            source.repoUrl
        } else {
            // Convert GitHub repo URL to raw manifest URL
            val rawBase = source.repoUrl
                .replace("github.com", "raw.githubusercontent.com")
                .replace("/blob/", "/")
                .replace("/tree/", "/")
            "$rawBase/main/$MANIFEST_FILE"
        }

        val request = Request.Builder()
            .url(manifestUrl)
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to load extension manifest: ${response.code}")
        }

        val manifestJson = response.body?.string()
            ?: throw IOException("Empty response from manifest URL")

        val manifest = gson.fromJson(manifestJson, ExtensionManifest::class.java)
        val api = createExtensionApi(manifest, source)
        
        val loaded = LoadedExtension(manifest, api, source)
        loadedExtensions[manifest.id] = loaded
        
        Timber.d("Loaded extension from GitHub: ${manifest.name}")
        return loaded
    }

    /**
     * Load extension from local file
     */
    private fun loadFromLocal(source: ExtensionSource.Local): LoadedExtension {
        val file = File(source.filePath)
        if (!file.exists()) {
            throw IOException("Extension file not found: ${source.filePath}")
        }

        val manifestJson = file.readText()
        val manifest = gson.fromJson(manifestJson, ExtensionManifest::class.java)
        val api = createExtensionApi(manifest, source)
        
        val loaded = LoadedExtension(manifest, api, source)
        loadedExtensions[manifest.id] = loaded
        
        Timber.d("Loaded extension from local: ${manifest.name}")
        return loaded
    }

    /**
     * Load extension from app assets
     */
    private fun loadFromAssets(source: ExtensionSource.BuiltIn): LoadedExtension {
        val manifestJson = context.assets.open(source.assetPath).bufferedReader().use { it.readText() }
        val manifest = gson.fromJson(manifestJson, ExtensionManifest::class.java)
        val api = createExtensionApi(manifest, source)
        
        val loaded = LoadedExtension(manifest, api, source)
        loadedExtensions[manifest.id] = loaded
        
        Timber.d("Loaded built-in extension: ${manifest.name}")
        return loaded
    }

    /**
     * Create Extension API implementation based on manifest
     */
    private fun createExtensionApi(
        manifest: ExtensionManifest,
        source: ExtensionSource
    ): ExtensionApi {
        return when (source) {
            is ExtensionSource.GitHub -> JsoupExtensionApi(manifest, httpClient)
            is ExtensionSource.Local -> JsoupExtensionApi(manifest, httpClient)
            is ExtensionSource.BuiltIn -> JsoupExtensionApi(manifest, httpClient)
        }
    }

    /**
     * Download and cache extension from remote
     */
    suspend fun downloadExtension(url: String, extensionId: String): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, EXTENSION_CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val extensionFile = File(cacheDir, "$extensionId.json")
        
        val request = Request.Builder()
            .url(url)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download extension: ${response.code}")
            }

            response.body?.byteStream()?.use { input ->
                extensionFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        extensionFile
    }

    /**
     * Clear extension cache
     */
    fun clearCache() {
        val cacheDir = File(context.cacheDir, EXTENSION_CACHE_DIR)
        cacheDir.deleteRecursively()
        loadedExtensions.clear()
        Timber.d("Extension cache cleared")
    }
}

/**
 * Jsoup-based Extension API Implementation
 * 
 * This is a base implementation that extensions can extend.
 * It provides web scraping capabilities using Jsoup.
 */
open class JsoupExtensionApi(
    private val manifest: ExtensionManifest,
    private val httpClient: OkHttpClient
) : ExtensionApi {

    override suspend fun getManifest(): ExtensionManifest = manifest

    override suspend fun getMainPage(): ExtensionHomeResponse {
        // Base implementation - should be overridden by specific extensions
        return ExtensionHomeResponse(
            sections = emptyList(),
            hasMore = false
        )
    }

    override suspend fun search(query: String, page: Int): ExtensionSearchResponse {
        // Base implementation - should be overridden
        return ExtensionSearchResponse(
            items = emptyList(),
            hasMore = false
        )
    }

    override suspend fun loadMovie(url: String): ExtensionMovieResponse {
        // Base implementation - should be overridden
        throw NotImplementedError("loadMovie must be implemented by extension")
    }

    override suspend fun loadLinks(url: String): ExtensionLinksResponse {
        // Base implementation - should be overridden
        throw NotImplementedError("loadLinks must be implemented by extension")
    }

    /**
     * Helper method to fetch and parse HTML document
     */
    protected suspend fun fetchDocument(url: String): org.jsoup.nodes.Document = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to fetch $url: ${response.code}")
        }

        val html = response.body?.string() ?: throw IOException("Empty response")
        Jsoup.parse(html, url)
    }

    /**
     * Helper to resolve relative URLs
     */
    protected fun resolveUrl(base: String, relative: String): String {
        return if (relative.startsWith("http")) {
            relative
        } else {
            java.net.URL(java.net.URL(base), relative).toString()
        }
    }

    /**
     * Helper to extract quality from string
     */
    protected fun extractQuality(text: String): String {
        val patterns = listOf(
            Regex("(\\d{3,4})p"),
            Regex("(\\d{3,4})")
        )
        
        for (pattern in patterns) {
            pattern.find(text)?.let {
                return "${it.groupValues[1]}p"
            }
        }
        
        return "Auto"
    }
}

/**
 * Extension Repository - Manages extension data operations
 */
class ExtensionRepository(
    private val loader: ExtensionLoaderImpl,
    private val context: Context
) {
    private val gson = Gson()

    /**
     * Install extension from GitHub
     */
    suspend fun installFromGitHub(repoUrl: String): Result<LoadedExtension> {
        return try {
            val source = ExtensionSource.GitHub(repoUrl)
            val extension = loader.loadExtension(source)
            Result.success(extension)
        } catch (e: Exception) {
            Timber.e(e, "Failed to install extension from GitHub")
            Result.failure(e)
        }
    }

    /**
     * Install extension from local file
     */
    fun installFromLocal(filePath: String): Result<LoadedExtension> {
        return try {
            val source = ExtensionSource.Local(filePath)
            val extension = loader.loadExtension(source)
            Result.success(extension)
        } catch (e: Exception) {
            Timber.e(e, "Failed to install extension from local")
            Result.failure(e)
        }
    }

    /**
     * Load all built-in extensions
     */
    fun loadBuiltInExtensions(): List<LoadedExtension> {
        val extensions = mutableListOf<LoadedExtension>()
        
        try {
            context.assets.list("extensions")?.forEach { fileName ->
                if (fileName.endsWith(".json")) {
                    try {
                        val source = ExtensionSource.BuiltIn("extensions/$fileName")
                        val extension = loader.loadExtension(source)
                        extensions.add(extension)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load built-in extension: $fileName")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to list built-in extensions")
        }
        
        return extensions
    }

    /**
     * Get all loaded extensions
     */
    fun getAllExtensions(): List<LoadedExtension> {
        return loader.getLoadedExtensions()
    }

    /**
     * Get enabled extensions only
     */
    fun getEnabledExtensions(): List<LoadedExtension> {
        return loader.getLoadedExtensions().filter { it.isEnabled }
    }

    /**
     * Uninstall extension
     */
    fun uninstallExtension(extensionId: String) {
        loader.unloadExtension(extensionId)
    }

    /**
     * Clear all extensions
     */
    fun clearAll() {
        loader.clearCache()
    }
}

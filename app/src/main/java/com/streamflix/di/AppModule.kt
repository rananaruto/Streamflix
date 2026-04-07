package com.streamflix.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.streamflix.data.local.AppDatabase
import com.streamflix.data.local.ExtensionDao
import com.streamflix.data.local.MediaDao
import com.streamflix.data.local.WatchHistoryDao
import com.streamflix.data.repository.MediaRepository
import com.streamflix.extension.ExtensionLoaderImpl
import com.streamflix.extension.ExtensionRepository
import com.streamflix.ui.viewmodel.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Koin Dependency Injection Modules
 * 
 * Organized into separate modules for:
 * - App-level dependencies
 * - Network dependencies
 * - Database dependencies
 * - Repository dependencies
 * - ViewModel dependencies
 */

/**
 * App Module - Core application dependencies
 */
val appModule = module {
    single<Gson> {
        GsonBuilder()
            .setLenient()
            .create()
    }
}

/**
 * Network Module - HTTP client and networking
 */
val networkModule = module {
    single {
        HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://api.example.com/") // Base URL for any API calls
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }
}

/**
 * Database Module - Room database and DAOs
 */
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "streamflix.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<AppDatabase>().mediaDao() }
    single { get<AppDatabase>().watchHistoryDao() }
    single { get<AppDatabase>().extensionDao() }
}

/**
 * Repository Module - Data repositories
 */
val repositoryModule = module {
    single { MediaRepository(get()) }
    single { ExtensionLoaderImpl(androidContext(), get()) }
    single { ExtensionRepository(get(), androidContext()) }
}

/**
 * ViewModel Module - UI ViewModels
 */
val viewModelModule = module {
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { CategoriesViewModel(get()) }
    viewModel { PlayerViewModel(get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { SettingsViewModel(androidContext()) }
    viewModel { ExtensionsViewModel(get()) }
}

/**
 * Extension Module - Extension system dependencies
 */
val extensionModule = module {
    single { ExtensionLoaderImpl(androidContext(), get()) }
    single { ExtensionRepository(get(), androidContext()) }
}

/**
 * Utility function to provide Context
 */
fun provideContext(context: Context): Context = context

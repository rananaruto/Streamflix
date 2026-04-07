package com.streamflix.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.streamflix.data.model.MediaItem
import com.streamflix.data.model.WatchHistoryEntry

/**
 * StreamFlix Room Database
 * 
 * Contains all local data tables:
 * - Media items (favorites, cache)
 * - Watch history
 * - Extension metadata
 */
@Database(
    entities = [
        MediaItem::class,
        WatchHistoryEntry::class,
        ExtensionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mediaDao(): MediaDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun extensionDao(): ExtensionDao

    companion object {
        private const val DATABASE_NAME = "streamflix.db"
        
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate if needed
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

/**
 * Room Type Converters
 */
class Converters {
    
    @androidx.room.TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",")?.map { it.trim() } ?: emptyList()
    }

    @androidx.room.TypeConverter
    fun toStringList(list: List<String>): String {
        return list.joinToString(",")
    }

    @androidx.room.TypeConverter
    fun fromMediaType(value: String?): com.streamflix.data.model.MediaType {
        return value?.let {
            com.streamflix.data.model.MediaType.valueOf(it)
        } ?: com.streamflix.data.model.MediaType.MOVIE
    }

    @androidx.room.TypeConverter
    fun toMediaString(type: com.streamflix.data.model.MediaType): String {
        return type.name
    }
}

/**
 * Extension Entity for Room
 */
@Entity(tableName = "extensions")
data class ExtensionEntity(
    @androidx.room.PrimaryKey
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val sourceUrl: String,
    val language: String = "en",
    val isEnabled: Boolean = true,
    val isInstalled: Boolean = false,
    val installedAt: Long? = null,
    val updatedAt: Long? = null,
    val categories: String = "" // Comma-separated
)

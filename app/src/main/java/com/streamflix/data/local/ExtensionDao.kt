package com.streamflix.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Extension DAO - Manages installed extensions metadata
 */
@Dao
interface ExtensionDao {

    @Query("SELECT * FROM extensions ORDER BY name ASC")
    fun getAll(): Flow<List<ExtensionEntity>>

    @Query("SELECT * FROM extensions WHERE isEnabled = 1 ORDER BY name ASC")
    fun getEnabled(): Flow<List<ExtensionEntity>>

    @Query("SELECT * FROM extensions WHERE id = :id")
    suspend fun getById(id: String): ExtensionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(extension: ExtensionEntity)

    @Update
    suspend fun update(extension: ExtensionEntity)

    @Query("UPDATE extensions SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Delete
    suspend fun delete(extension: ExtensionEntity)

    @Query("DELETE FROM extensions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM extensions")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM extensions WHERE isEnabled = 1")
    fun getEnabledCount(): Flow<Int>
}

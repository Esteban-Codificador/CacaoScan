package com.cacaoscan.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    /** Historial completo, del mas reciente al mas antiguo. */
    @Query("SELECT * FROM scans ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun getById(id: Long): ScanEntity?

    @Insert
    suspend fun insert(scan: ScanEntity): Long

    /** Devuelve el nombre del archivo para poder borrarlo del disco. */
    @Query("SELECT image_file_name FROM scans WHERE id = :id")
    suspend fun getImageFileName(id: Long): String?

    @Query("SELECT image_file_name FROM scans WHERE image_file_name IS NOT NULL")
    suspend fun getAllImageFileNames(): List<String>

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scans")
    suspend fun deleteAll()
}

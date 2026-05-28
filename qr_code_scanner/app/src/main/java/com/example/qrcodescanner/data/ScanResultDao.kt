package com.example.qrcodescanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllScanResults(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScanResults(): Flow<List<ScanResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanResult(scanResult: ScanResultEntity): Long

    @Delete
    suspend fun deleteScanResult(scanResult: ScanResultEntity)

    @Update
    suspend fun updateScanResult(scanResult: ScanResultEntity)

    @Query("DELETE FROM scan_results")
    suspend fun clearHistory()
}

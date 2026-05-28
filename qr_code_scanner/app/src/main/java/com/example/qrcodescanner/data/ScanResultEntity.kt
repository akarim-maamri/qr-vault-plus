package com.example.qrcodescanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val type: String, // "URL", "TEXT", "WIFI", "PHONE", "EMAIL", etc.
    val timestamp: Long,
    val isFavorite: Boolean = false
)

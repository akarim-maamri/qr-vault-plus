package com.example.qrcodescanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secure_notes")
data class SecureNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val contentEncrypted: String, // Encrypted securely using the Master Key
    val timestamp: Long
)

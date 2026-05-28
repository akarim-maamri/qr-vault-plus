package com.example.qrcodescanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val username: String,
    val passwordEncrypted: String, // Encrypted securely using CryptoUtils
    val url: String = "",
    val notes: String = "",
    val timestamp: Long
)

package com.example.qrcodescanner.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.util.Base64

object DatabaseBackupUtils {

    fun getEncryptedBackupFile(context: Context, masterKey: String): File? {
        try {
            val dbFile = context.getDatabasePath("scan_history_database")
            if (!dbFile.exists()) {
                return null
            }

            // Create backups dir in cache
            val backupsDir = File(context.cacheDir, "backups")
            if (!backupsDir.exists()) backupsDir.mkdirs()

            // Output file
            val backupFile = File(backupsDir, "VaultBackup_${System.currentTimeMillis()}.enc")

            // Read database bytes
            val dbBytes = FileInputStream(dbFile).use { it.readBytes() }

            // Encode and encrypt
            val base64Db = Base64.encodeToString(dbBytes, Base64.DEFAULT)
            val encryptedDbString = CryptoUtils.encrypt(base64Db, masterKey)
            
            FileOutputStream(backupFile).use { fos ->
                fos.write(encryptedDbString.toByteArray(Charsets.UTF_8))
            }
            
            return backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportAndShareDatabase(context: Context, masterKey: String) {
        try {
            val backupFile = getEncryptedBackupFile(context, masterKey) ?: return

            // Share file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, backupFile.name)
                putExtra(Intent.EXTRA_SUBJECT, "Vault Database Backup")
                clipData = android.content.ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Save Backup to Google Drive / Email")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreEncryptedBackupFile(context: Context, uri: android.net.Uri, masterKey: String): Boolean {
        try {
            val encryptedBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
            val encryptedString = String(encryptedBytes, Charsets.UTF_8)
            
            val base64Db = CryptoUtils.decrypt(encryptedString, masterKey)
            if (base64Db.isEmpty() || base64Db == encryptedString) return false
            
            val dbBytes = Base64.decode(base64Db, Base64.DEFAULT)
            
            val dbFile = context.getDatabasePath("scan_history_database")
            
            // Clean up any existing journal/wal files
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            val journalFile = File(dbFile.path + "-journal")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
            if (journalFile.exists()) journalFile.delete()
            
            FileOutputStream(dbFile).use { fos ->
                fos.write(dbBytes)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}

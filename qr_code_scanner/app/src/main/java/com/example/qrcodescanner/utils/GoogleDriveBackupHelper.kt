package com.example.qrcodescanner.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.api.client.http.FileContent
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveBackupHelper(private val context: Context, private val account: GoogleSignInAccount) {

    private val driveService: Drive

    init {
        val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("QR Vault Plus")
            .build()
    }

    suspend fun uploadBackupFile(localFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        try {
            val folderId = getOrCreateFolder("QR Vault+_Backup")
            
            // Check if backup file already exists to overwrite it
            val existingFileId = getFileIdByName("QR_Vault_Backup.enc", folderId)
            
            val fileMetadata = File()
            fileMetadata.name = "QR_Vault_Backup.enc"
            
            val mediaContent = FileContent("application/octet-stream", localFile)

            if (existingFileId != null) {
                // Update existing
                driveService.files().update(existingFileId, fileMetadata, mediaContent).execute()
            } else {
                // Create new
                fileMetadata.parents = listOf(folderId)
                driveService.files().create(fileMetadata, mediaContent).execute()
            }
            true
        } catch (e: Exception) {
            Log.e("DriveBackup", "Upload failed", e)
            false
        }
    }

    suspend fun downloadBackupFile(destinationFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        try {
            val folderId = getOrCreateFolder("QR Vault+_Backup")
            val existingFileId = getFileIdByName("QR_Vault_Backup.enc", folderId)
            
            if (existingFileId != null) {
                FileOutputStream(destinationFile).use { outputStream ->
                    driveService.files().get(existingFileId).executeMediaAndDownloadTo(outputStream)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DriveBackup", "Download failed", e)
            false
        }
    }

    private fun getOrCreateFolder(folderName: String): String {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false"
        val result: FileList = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val files = result.files
        if (files.isNotEmpty()) {
            return files[0].id
        }

        val folderMetadata = File()
        folderMetadata.name = folderName
        folderMetadata.mimeType = "application/vnd.google-apps.folder"

        val folder = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()
        return folder.id
    }

    private fun getFileIdByName(fileName: String, parentFolderId: String): String? {
        val query = "name='$fileName' and '$parentFolderId' in parents and trashed=false"
        val result: FileList = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val files = result.files
        return if (files.isNotEmpty()) files[0].id else null
    }
}

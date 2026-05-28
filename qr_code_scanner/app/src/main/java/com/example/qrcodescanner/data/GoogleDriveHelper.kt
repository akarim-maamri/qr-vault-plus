package com.example.qrcodescanner.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * GoogleDriveHelper provides functionality to backup and restore the encrypted Room Database
 * to the user's Google Drive appDataFolder (hidden from the user, accessible only by this app).
 * 
 * Note: Requires Google Sign-In OAuth token with Scope: "https://www.googleapis.com/auth/drive.appdata"
 */
object GoogleDriveHelper {
    private const val TAG = "GoogleDriveHelper"
    private val client = OkHttpClient()

    // Endpoint for uploading files to Drive
    private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

    suspend fun uploadDatabaseBackup(context: Context, accessToken: String, dbFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!dbFile.exists()) return@withContext false

            val metadataJson = """
                {
                  "name": "backup_scan_database.db",
                  "parents": ["appDataFolder"]
                }
            """.trimIndent()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(
                    Headers.Builder().add("Content-Type", "application/json; charset=UTF-8").build(),
                    RequestBody.create("application/json".toMediaTypeOrNull(), metadataJson)
                )
                .addPart(
                    Headers.Builder().add("Content-Type", "application/octet-stream").build(),
                    dbFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                Log.d(TAG, "Upload response: ${response.code} $body")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload DB to Drive", e)
            return@withContext false
        }
    }
}

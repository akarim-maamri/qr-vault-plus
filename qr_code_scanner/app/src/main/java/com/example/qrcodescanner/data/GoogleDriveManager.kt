package com.example.qrcodescanner.data

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

object GoogleDriveManager {

    suspend fun uploadBackupToDrive(context: Context, accountEmail: String, backupFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get OAuth Token for Drive REST API
                val scope = "oauth2:https://www.googleapis.com/auth/drive.file"
                val account = android.accounts.Account(accountEmail, "com.google")
                val token = GoogleAuthUtil.getToken(context, account, scope)

                // 2. Upload file via REST API
                val client = OkHttpClient()

                // Google Drive v3 multipart upload
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "metadata", 
                        null, 
                        okhttp3.RequestBody.create(
                            "application/json; charset=UTF-8".toMediaTypeOrNull(), 
                            JSONObject().apply {
                                put("name", backupFile.name)
                                put("mimeType", "application/octet-stream")
                                put("parents", org.json.JSONArray().put("root")) // Optional: store in root
                            }.toString()
                        )
                    )
                    .addFormDataPart(
                        "file", 
                        backupFile.name, 
                        backupFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val isSuccess = response.isSuccessful
                response.close()
                
                isSuccess
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}

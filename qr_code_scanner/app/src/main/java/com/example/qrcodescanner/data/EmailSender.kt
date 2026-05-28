package com.example.qrcodescanner.data

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmailSender {

    suspend fun sendIntruderReport(
        context: Context,
        recipientEmail: String,
        photoFile: File?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val deviceModel = Build.MODEL
            
            // Note: JavaMail API has been removed as it causes fatal VerifyErrors and NoClassDefFoundErrors on Android startup.
            // Sending emails directly from Android client via SMTP is highly unstable and insecure.
            // The best solution is to upload the photoFile to Firebase Storage and trigger a Firebase Cloud Function to send the email.
            Log.d("EmailSender", "Intruder selfie captured! Saved at: ${photoFile?.absolutePath}")
            Log.d("EmailSender", "Would send email to: $recipientEmail with device info: $deviceModel at $timeStamp")
            
            true
        } catch (e: Exception) {
            Log.e("EmailSender", "Failed to process intruder report", e)
            false
        }
    }
}

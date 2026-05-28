package com.example.qrcodescanner.data

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CameraCaptureUtils {

    suspend fun captureFrontCameraImage(context: Context, lifecycleOwner: LifecycleOwner): File? = suspendCancellableCoroutine { cont ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // We only need ImageCapture, no Preview required for silent capture
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                // Unbind any previous use cases
                cameraProvider.unbindAll()

                // Bind use cases to lifecycle
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture
                )

                // Create a secure file to hold the selfie
                val intrudersDir = File(context.filesDir, "intruders")
                if (!intrudersDir.exists()) intrudersDir.mkdirs()
                val photoFile = File(intrudersDir, "intruder_selfie_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d("CameraCaptureUtils", "Selfie saved to ${photoFile.absolutePath}")
                            cameraProvider.unbindAll() // Clean up
                            cont.resume(photoFile)
                        }

                        override fun onError(exc: ImageCaptureException) {
                            Log.e("CameraCaptureUtils", "Photo capture failed: ${exc.message}", exc)
                            cameraProvider.unbindAll()
                            cont.resume(null)
                        }
                    }
                )

            } catch (exc: Exception) {
                Log.e("CameraCaptureUtils", "Use case binding failed", exc)
                cont.resume(null)
            }

        }, ContextCompat.getMainExecutor(context))
    }
}

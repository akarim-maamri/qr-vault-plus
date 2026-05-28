package com.example.qrcodescanner.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.example.qrcodescanner.data.ScanResultEntity
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n
import com.example.qrcodescanner.ui.components.CameraPreview
import com.example.qrcodescanner.ui.components.QrViewfinder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onSaveResult: (ScanResultEntity) -> Unit,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Camera controller setup
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
        }
    }

    var cameraPermissionGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
    }

    // Request permission on start
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var flashEnabled by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(true) }

    // Scanning states
    var isScanningActive by remember { mutableStateOf(true) }
    var scannedText by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = BarcodeScanning.getClient(options)
                val inputImage = InputImage.fromFilePath(context, uri)
                
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (!barcodes.isNullOrEmpty()) {
                            val qrCode = barcodes[0].rawValue
                            if (qrCode != null) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                scannedText = qrCode
                                isScanningActive = false // Pause scanning until dismissed/saved
                                
                                val type = parseQrType(qrCode)
                                onSaveResult(
                                    ScanResultEntity(
                                        content = qrCode,
                                        type = type,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            } else {
                                Toast.makeText(context, L10n.get("no_qr_found_toast", lang), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, L10n.get("no_qr_found_toast", lang), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, L10n.get("no_qr_found_toast", lang) + ": ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, L10n.get("no_qr_found_toast", lang), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF121212))) {
        if (cameraPermissionGranted) {
            CameraPreview(
                controller = cameraController,
                onQrScanned = { result ->
                    if (isScanningActive && result != scannedText) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        scannedText = result
                        isScanningActive = false // Pause scanning until dismissed/saved
                        
                        // Parse QR code type and save to database
                        val type = parseQrType(result)
                        onSaveResult(
                            ScanResultEntity(
                                content = result,
                                type = type,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Cyber Holographic overlay Viewfinder
            QrViewfinder()

            // Transparent App Name Overlay (Moved below scanner)
            Text(
                text = "+QR Vault",
                color = Color.White.copy(alpha = 0.15f),
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            )

            // Quick Floating Controls on Top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Torch toggle
                IconButton(
                    onClick = {
                        flashEnabled = !flashEnabled
                        cameraController.enableTorch(flashEnabled)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xAA1A1A1A), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (lang == AppLanguage.AR) "تبديل الفلاش" else "Activer/Désactiver le flash",
                        tint = if (flashEnabled) Color(0xFF00F2FE) else Color.White
                    )
                }

                Text(
                    text = L10n.get("tab_scan", lang).uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                // Right-aligned controls (Flip Camera & Gallery)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Import Button
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xAA1A1A1A), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = L10n.get("gallery_pick", lang),
                            tint = Color.White
                        )
                    }

                    // Flip Camera
                    IconButton(
                        onClick = {
                            isBackCamera = !isBackCamera
                            cameraController.cameraSelector = if (isBackCamera) {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            } else {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xAA1A1A1A), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwitchCamera,
                            contentDescription = L10n.get("switch_camera", lang),
                            tint = Color.White
                        )
                    }
                }
            }

            // Beautiful Scanned Result Bottom Sheet Overlay
            AnimatedVisibility(
                visible = scannedText != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                scannedText?.let { content ->
                    val qrType = parseQrType(content)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFC1E1E1E), Color(0xFC121212))
                                )
                            )
                            .border(1.dp, Color(0x4000F2FE), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top accent bar
                        Box(
                            modifier = Modifier
                                .size(40.dp, 4.dp)
                                .background(Color(0xFF333333), RoundedCornerShape(2.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Success Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Scanned",
                                tint = Color(0xFF00F2FE),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L10n.get("scan_success_toast", lang),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Render dynamic type label
                        val categoryKey = when (qrType) {
                            "URL" -> "category_url"
                            "WIFI" -> "category_wifi"
                            "PHONE" -> "category_phone"
                            "EMAIL" -> "category_email"
                            else -> "category_text"
                        }
                        val localizedCategory = L10n.get(categoryKey, lang)
                        Text(
                            text = "${if (lang == AppLanguage.AR) "النوع: " else "Type: "}$localizedCategory",
                            color = Color(0xFF00F2FE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(Color(0x3300F2FE), RoundedCornerShape(100.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable content view
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .background(Color(0xFF121212), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = content,
                                color = Color.LightGray,
                                fontSize = 15.sp,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Copy button
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Scanned QR", content)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, L10n.get("copied_toast", lang), Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF222222)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(L10n.get("btn_copy_text", lang), color = Color.White)
                            }

                            // Smart action button based on type
                            if (qrType == "URL") {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, if (lang == AppLanguage.AR) "لا يمكن فتح المتصفح" else "Impossible d'ouvrir le navigateur", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00F2FE)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open Link", tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(L10n.get("btn_open_link", lang), color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, content)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, L10n.get("btn_share", lang)))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00F2FE)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(L10n.get("btn_share", lang), color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Resume scan button
                        TextButton(
                            onClick = {
                                scannedText = null
                                isScanningActive = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (lang == AppLanguage.AR) "انقر للمسح مجدداً" else "Appuyez pour scanner à nouveau", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            // Camera Access Denied State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (lang == AppLanguage.AR) "إذن الكاميرا مطلوب" else "Autorisation de la caméra requise",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = L10n.get("camera_permission_denied", lang),
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE))
                ) {
                    Text(L10n.get("grant_camera_permission", lang), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun parseQrType(content: String): String {
    return when {
        content.startsWith("http://", ignoreCase = true) || 
        content.startsWith("https://", ignoreCase = true) -> "URL"
        content.startsWith("WIFI:", ignoreCase = true) -> "WIFI"
        content.startsWith("tel:", ignoreCase = true) -> "PHONE"
        content.startsWith("mailto:", ignoreCase = true) -> "EMAIL"
        else -> "TEXT"
    }
}

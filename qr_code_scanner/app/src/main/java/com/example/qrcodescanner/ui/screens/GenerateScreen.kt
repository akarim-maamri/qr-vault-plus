package com.example.qrcodescanner.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = URL/Text, 1 = WiFi

    // Dynamic inputs
    var textInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") }

    // Final QR Content to encode
    val qrContent = remember(activeTab, textInput, wifiSsid, wifiPassword, wifiSecurity) {
        if (activeTab == 0) {
            textInput
        } else {
            if (wifiSsid.isNotEmpty()) {
                "WIFI:S:$wifiSsid;T:$wifiSecurity;P:$wifiPassword;;"
            } else {
                ""
            }
        }
    }

    // Dynamic QR Bitmap generation
    val qrBitmap = remember(qrContent) {
        if (qrContent.isNotEmpty()) {
            generateQrCodeBitmap(qrContent)
        } else {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        L10n.get("generate_title", lang).uppercase(),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF121212)
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color(0xFF121212),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(androidx.compose.ui.graphics.Color(0xFF121212))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Segmented Controls (Tab selection)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF1A1A1A))
                    .border(1.dp, androidx.compose.ui.graphics.Color(0xFF262626), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    selected = activeTab == 0,
                    text = L10n.get("tab_text", lang),
                    icon = Icons.Default.Language,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    selected = activeTab == 1,
                    text = L10n.get("tab_wifi", lang),
                    icon = Icons.Default.Wifi,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Fields Card
            Card(
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, androidx.compose.ui.graphics.Color(0xFF262626), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (activeTab == 0) {
                        Text(
                            text = if (lang == AppLanguage.AR) "أدخل النص أو الرابط:" else "Entrez le texte ou le lien URL:",
                            color = androidx.compose.ui.graphics.Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("https://example.com", color = androidx.compose.ui.graphics.Color.DarkGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = androidx.compose.ui.graphics.Color.White,
                                unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                                focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF00F2FE),
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF333333)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {

                        // SSID Input
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text(L10n.get("ssid_placeholder", lang), color = androidx.compose.ui.graphics.Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = androidx.compose.ui.graphics.Color.White,
                                unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                                focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF00F2FE),
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF333333)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Password Input
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            label = { Text(L10n.get("password_placeholder", lang), color = androidx.compose.ui.graphics.Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = androidx.compose.ui.graphics.Color.White,
                                unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                                focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF00F2FE),
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF333333)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Generated QR Display Container with futuristic glass card
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF1A1A1A))
                    .border(
                        1.dp,
                        if (qrBitmap != null) androidx.compose.ui.graphics.Color(0xFF00F2FE).copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color(0xFF262626),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (lang == AppLanguage.AR) "في انتظار المدخلات..." else "En attente de saisie...",
                            color = androidx.compose.ui.graphics.Color.DarkGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Share Button
            if (qrBitmap != null) {
                Button(
                    onClick = {
                        shareQrCode(context, qrBitmap, lang)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF00F2FE)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = androidx.compose.ui.graphics.Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        L10n.get("btn_share_qr", lang),
                        color = androidx.compose.ui.graphics.Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(
    selected: Boolean,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) androidx.compose.ui.graphics.Color(0xFF262626) else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (selected) androidx.compose.ui.graphics.Color(0xFF00F2FE) else androidx.compose.ui.graphics.Color.Gray

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun generateQrCodeBitmap(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val hints = java.util.EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java)
        hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
        val qrWidth = bitMatrix.width
        val qrHeight = bitMatrix.height
        val qrBitmap = Bitmap.createBitmap(qrWidth, qrHeight, Bitmap.Config.ARGB_8888)
        for (x in 0 until qrWidth) {
            for (y in 0 until qrHeight) {
                qrBitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }

        val paddingX = 160
        val paddingY = 80
        val finalWidth = qrWidth + paddingX * 2
        val finalHeight = qrHeight + paddingY * 2
        val finalBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(finalBitmap)
        canvas.drawColor(android.graphics.Color.parseColor("#EAEAEA"))

        val qrLeft = paddingX.toFloat()
        val qrTop = paddingY.toFloat()
        val paint = android.graphics.Paint()
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, paint)



        val textPaintApp = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 36f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        val textPaintDate = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#555555")
            textSize = 28f
            typeface = android.graphics.Typeface.DEFAULT
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val cx = finalWidth / 2f
        // Top Text: App Name
        canvas.drawText("QR Vault+", cx, 55f, textPaintApp)

        // Bottom Text: Date
        val timeStamp = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText(timeStamp, cx, finalHeight - 35f, textPaintDate)

        finalBitmap
    } catch (e: Exception) {
        null
    }
}

private fun shareQrCode(context: Context, bitmap: Bitmap, lang: AppLanguage) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "generated_qr.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = "image/png"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, L10n.get("btn_share_qr", lang)))
    } catch (e: Exception) {
        Toast.makeText(context, if (lang == AppLanguage.AR) "خطأ في مشاركة رمز الـ QR" else "Erreur de partage du QR Code", Toast.LENGTH_SHORT).show()
    }
}

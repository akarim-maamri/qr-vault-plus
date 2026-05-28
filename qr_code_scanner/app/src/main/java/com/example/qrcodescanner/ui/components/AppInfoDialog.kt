package com.example.qrcodescanner.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n

@Composable
fun AppInfoDialog(
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onShowWalkthrough: () -> Unit,
    registeredEmail: String? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    // Accordion state: 0=none, 1=features, 2=bugReport, 3=developerInfo
    var expandedSection by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF161618)
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E1E22), Color(0xFF121214))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Icon and Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color(0xFF00F2FE),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (lang == AppLanguage.AR) "معلومات التطبيق" else "App Info",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Description text
                    Text(
                        text = L10n.get("app_info_desc", lang),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Feature list
                    val isFeaturesExpanded = expandedSection == 1
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable { expandedSection = if (expandedSection == 1) 0 else 1 }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = L10n.get("app_features_title", lang),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isFeaturesExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF00F2FE)
                            )
                        }
                        if (isFeaturesExpanded) {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = L10n.get("feat_scan", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                Text(text = L10n.get("feat_gen", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                Text(text = L10n.get("feat_vault", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                Text(text = L10n.get("feat_widget", lang), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                        }
                    }

                    // Report Bug Section
                    val isBugReportExpanded = expandedSection == 2
                    var bugTitle by remember { mutableStateOf("") }
                    var bugMessage by remember { mutableStateOf("") }
                    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
                    
                    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri: Uri? -> selectedImageUri = uri }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (expandedSection == 2) 0 else 2 }.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.AR) "إرسال مقترح أو تبليغ عن خطأ" else "Submit Suggestion / Report Bug",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isBugReportExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF00F2FE)
                            )
                        }

                        if (isBugReportExpanded) {
                            OutlinedTextField(
                                value = bugTitle,
                                onValueChange = { bugTitle = it },
                                label = { Text(if (lang == AppLanguage.AR) "عنوان الرسالة" else "Message Title", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00F2FE), unfocusedBorderColor = Color(0xFF333333)
                                )
                            )
                            OutlinedTextField(
                                value = bugMessage,
                                onValueChange = { bugMessage = it },
                                label = { Text(if (lang == AppLanguage.AR) "اكتب مقترحك أو تفاصيل الخطأ" else "Write your suggestion or bug details", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00F2FE), unfocusedBorderColor = Color(0xFF333333)
                                )
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                    Icon(Icons.Filled.Image, contentDescription = null, tint = Color(0xFF00F2FE))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (selectedImageUri != null) (if (lang == AppLanguage.AR) "تم الإرفاق" else "Attached") else (if (lang == AppLanguage.AR) "إرفاق صورة" else "Attach Image"),
                                        color = if (selectedImageUri != null) Color(0xFF00FF87) else Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (bugTitle.isEmpty() || bugMessage.isEmpty()) {
                                            Toast.makeText(context, if (lang == AppLanguage.AR) "الرجاء إدخال العنوان والرسالة" else "Please enter title and message", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val bodyText = if (registeredEmail != null)
                                            "$bugMessage\n\n---\n${if (lang == AppLanguage.AR) "مُرسَل من" else "Sent from"}: $registeredEmail"
                                        else bugMessage
                                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:")
                                            putExtra(Intent.EXTRA_EMAIL, arrayOf("jussor.tech@gmail.com"))
                                            putExtra(Intent.EXTRA_SUBJECT, "+QR Vault - $bugTitle")
                                            putExtra(Intent.EXTRA_TEXT, bodyText)
                                        }
                                        try {
                                            context.startActivity(emailIntent)
                                        } catch (e: Exception) {
                                            val fallback = Intent(Intent.ACTION_SEND).apply {
                                                type = "message/rfc822"
                                                putExtra(Intent.EXTRA_EMAIL, arrayOf("jussor.tech@gmail.com"))
                                                putExtra(Intent.EXTRA_SUBJECT, "+QR Vault - $bugTitle")
                                                putExtra(Intent.EXTRA_TEXT, bodyText)
                                                if (selectedImageUri != null) {
                                                    putExtra(Intent.EXTRA_STREAM, selectedImageUri)
                                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                }
                                            }
                                            context.startActivity(Intent.createChooser(fallback, if (lang == AppLanguage.AR) "إرسال بالبريد" else "Send Email"))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (lang == AppLanguage.AR) "إرسال" else "Send", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Premium Centered Developer Card
                    val isDeveloperInfoExpanded = expandedSection == 3
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable { expandedSection = if (expandedSection == 3) 0 else 3 }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (lang == AppLanguage.AR) "معلومات المطور" else "Developer Info",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isDeveloperInfoExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF00F2FE)
                            )
                        }

                        if (isDeveloperInfoExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${L10n.get("developed_by_label", lang)} :",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "MAAMRI ABDELKARIM",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = L10n.get("dev_email_label", lang),
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Jussor.Tech@gmail.com",
                                color = Color(0xFF00F2FE),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable {
                                    try {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("email", "Jussor.Tech@gmail.com")
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ البريد الإلكتروني!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {}
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Official Links Section
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                // Privacy Policy
                                IconButton(onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/qrcodescanner-pp/home"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }) {
                                    Icon(Icons.Filled.PrivacyTip, contentDescription = "Privacy Policy", tint = Color(0xFFFF007F))
                                }
                                // Email
                                IconButton(onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:jussor.tech@gmail.com"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }) {
                                    Icon(Icons.Filled.Email, contentDescription = "Email", tint = Color.White)
                                }
                                // Facebook
                                IconButton(onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.facebook.com/maamri.abdelkarim.2025"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }) {
                                    Icon(Icons.Filled.Facebook, contentDescription = "Facebook", tint = Color(0xFF1877F2))
                                }
                                // Telegram
                                IconButton(onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/jussor_tech"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }) {
                                    Icon(Icons.Filled.Send, contentDescription = "Telegram", tint = Color(0xFF24A1DE))
                                }
                                // GitHub
                                IconButton(onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/akarim-maamri"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }) {
                                    Icon(Icons.Filled.Code, contentDescription = "GitHub", tint = Color.White)
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            // Disclaimer
                            Text(
                                text = L10n.get("app_disclaimer", lang),
                                color = Color(0xFFFF5252),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Product Walkthrough Launch Button
                    Button(
                        onClick = {
                            onDismiss()
                            onShowWalkthrough()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00F2FE)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Slideshow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = L10n.get("walkthrough_btn", lang),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Close Button
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = L10n.get("btn_close", lang),
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

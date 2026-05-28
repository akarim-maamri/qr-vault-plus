package com.example.qrcodescanner.ui.screens

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Share
import androidx.fragment.app.FragmentActivity
import com.example.qrcodescanner.data.BiometricHelper
import com.example.qrcodescanner.data.DatabaseBackupUtils
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.services.drive.DriveScopes
import com.example.qrcodescanner.utils.GoogleDriveBackupHelper
import android.content.Context
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n
import kotlinx.coroutines.launch

@Composable
fun ExpandableSettingCard(
    title: String,
    icon: @Composable () -> Unit,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    content: @Composable () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp))
            .clickable { onExpandClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icon()
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (trailingAction != null) {
                        trailingAction()
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = Color(0xFF262626), thickness = 1.dp)
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingsScreen(
    currentPin: String,
    registeredGmail: String?,
    currentUserEmail: String?,
    backupCode: String,
    onBack: () -> Unit,
    onChangePin: (String, String) -> Boolean,
    lang: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isBiometricEnabled: Boolean,
    onToggleBiometrics: (Boolean, String?, String?) -> Unit,
    onGenerateAndSendBackupKey: (String, String, (Boolean, String?) -> Unit) -> Unit,
    activeMasterKey: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isGeneratingBackup by remember { mutableStateOf(false) }

    var expandedCardIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x22FFFFFF), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = L10n.get("btn_close", lang),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = L10n.get("settings_title", lang).uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val scrollState = rememberScrollState()
        // Scrollable settings contents
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Transparent)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Biometrics
            ExpandableSettingCard(
                title = if (lang == AppLanguage.AR) "تأمين التطبيق (بصمة، وجه، PIN)" else "App Security (Fingerprint, Face, PIN)",
                icon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF00F2FE)) },
                isExpanded = expandedCardIndex == 0,
                onExpandClick = { expandedCardIndex = if (expandedCardIndex == 0) -1 else 0 },
                trailingAction = {
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { checked ->
                            val activity = context as? FragmentActivity
                            if (activity == null) {
                                Toast.makeText(context, "FragmentActivity required", Toast.LENGTH_SHORT).show()
                                return@Switch
                            }
                            
                            if (checked) {
                                if (!BiometricHelper.canAuthenticate(context)) {
                                    Toast.makeText(context, if (lang == AppLanguage.AR) "جهازك لا يدعم البصمة أو لم تقم بإعدادها" else "Biometrics not supported or not enrolled", Toast.LENGTH_LONG).show()
                                    return@Switch
                                }
                                if (activeMasterKey == null) {
                                    Toast.makeText(context, "Master key missing", Toast.LENGTH_SHORT).show()
                                    return@Switch
                                }
                                
                                BiometricHelper.showBiometricPromptForEncryption(
                                    activity = activity,
                                    masterKey = activeMasterKey,
                                    onSuccess = { encData, iv ->
                                        onToggleBiometrics(true, encData, iv)
                                        Toast.makeText(context, if (lang == AppLanguage.AR) "تم تفعيل البصمة بنجاح" else "Biometrics Enabled", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        if (err == "KEY_INVALIDATED") {
                                            onToggleBiometrics(false, null, null)
                                            val msg = if (lang == AppLanguage.AR) "تم مسح المفتاح القديم، الرجاء التفعيل مرة أخرى." else "Old key removed. Please toggle again."
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            } else {
                                onToggleBiometrics(false, null, null)
                                BiometricHelper.removeBiometricKey()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00F2FE),
                            checkedTrackColor = Color(0xFF00F2FE).copy(alpha = 0.5f)
                        )
                    )
                },
                content = {
                    Text(
                        text = if (lang == AppLanguage.AR) "يتيح لك فتح الخزنة باستخدام البصمة أو التعرف على الوجه بسرعة وبدون كتابة الرمز السري." else "Allows unlocking the vault quickly using fingerprint or face recognition without entering the PIN.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            )

            // Language Changer
            ExpandableSettingCard(
                title = L10n.get("settings_section_lang", lang),
                icon = { Icon(Icons.Default.Translate, contentDescription = null, tint = Color(0xFF00F2FE)) },
                isExpanded = expandedCardIndex == 1,
                onExpandClick = { expandedCardIndex = if (expandedCardIndex == 1) -1 else 1 },
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf(
                            Triple(AppLanguage.AR, L10n.get("lang_ar", lang), "🇩🇿"),
                            Triple(AppLanguage.EN, L10n.get("lang_en", lang), "🇺🇸"),
                            Triple(AppLanguage.FR, L10n.get("lang_fr", lang), "🇫🇷")
                        )

                        languages.forEach { (itemLang, label, flag) ->
                            val isSelected = lang == itemLang
                            Button(
                                onClick = {
                                    onLanguageChange(itemLang)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF00F2FE) else Color(0xFF262626)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(text = flag, fontSize = 18.sp)
                                    Text(
                                        text = label.split(" ")[0],
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            )

            // Change PIN
            ExpandableSettingCard(
                title = L10n.get("settings_section_pin", lang),
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00F2FE)) },
                isExpanded = expandedCardIndex == 2,
                onExpandClick = { expandedCardIndex = if (expandedCardIndex == 2) -1 else 2 },
                content = {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { if (it.length <= 4) oldPin = it },
                        label = { Text(L10n.get("field_old_pin", lang), color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F2FE),
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedLabelColor = Color(0xFF00F2FE),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4) newPin = it },
                        label = { Text(L10n.get("field_new_pin", lang), color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F2FE),
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedLabelColor = Color(0xFF00F2FE),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4) confirmPin = it },
                        label = { Text(L10n.get("field_confirm_pin", lang), color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F2FE),
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedLabelColor = Color(0xFF00F2FE),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (oldPin.length < 4 || newPin.length < 4 || confirmPin.length < 4) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val msg = if (lang == AppLanguage.AR) "الرجاء إدخال رموز PIN صالحة (4 أرقام)" else "Veuillez entrer des codes PIN valides (4 chiffres)"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (newPin != confirmPin) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val msg = if (lang == AppLanguage.AR) "رموز PIN الجديدة غير متطابقة!" else "Les nouveaux codes PIN ne correspondent pas!"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            val success = onChangePin(oldPin, newPin)
                            if (success) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, L10n.get("pin_update_success", lang), Toast.LENGTH_SHORT).show()
                                oldPin = ""
                                newPin = ""
                                confirmPin = ""
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, L10n.get("pin_update_failed", lang), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(L10n.get("btn_update_pin", lang), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )

    // Google Account section removed

            // Backup Recovery Code
            ExpandableSettingCard(
                title = L10n.get("settings_section_backup", lang),
                icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF00F2FE)) },
                isExpanded = expandedCardIndex == 4,
                onExpandClick = { expandedCardIndex = if (expandedCardIndex == 4) -1 else 4 },
                content = {
                    Text(
                        text = L10n.get("backup_code_desc", lang),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = backupCode,
                            color = Color(0xFF00F2FE),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        val targetEmail = registeredGmail ?: currentUserEmail ?: ""
                                        if (targetEmail.isNotEmpty()) {
                                            // Use direct email Intent - guaranteed to work with registered email
                                            val subject = if (lang == AppLanguage.AR)
                                                "+QR Vault - رمز الاسترداد الاحتياطي"
                                            else
                                                "+QR Vault - Backup Recovery Code"

                                            val body = if (lang == AppLanguage.AR)
                                                "مرحباً،\n\nفيما يلي رمز الاسترداد الاحتياطي الخاص بك لتطبيق +QR Vault:\n\n🔐 $backupCode\n\nاحتفظ بهذا الرمز في مكان آمن. يُستخدم لاستعادة الوصول إلى خزنتك في حال نسيت رمز PIN.\n\n⚠️ لا تشارك هذا الرمز مع أي أحد."
                                            else
                                                "Hello,\n\nHere is your backup recovery code for +QR Vault app:\n\n🔐 $backupCode\n\nKeep this code in a safe place. It is used to restore access to your vault if you forget your PIN.\n\n⚠️ Do not share this code with anyone."

                                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:")
                                                putExtra(Intent.EXTRA_EMAIL, arrayOf(targetEmail))
                                                putExtra(Intent.EXTRA_SUBJECT, subject)
                                                putExtra(Intent.EXTRA_TEXT, body)
                                            }
                                            try {
                                                context.startActivity(emailIntent)
                                            } catch (e: Exception) {
                                                // Fallback to ACTION_SEND if no email client supports SENDTO
                                                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "message/rfc822"
                                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(targetEmail))
                                                    putExtra(Intent.EXTRA_SUBJECT, subject)
                                                    putExtra(Intent.EXTRA_TEXT, body)
                                                }
                                                context.startActivity(Intent.createChooser(fallbackIntent,
                                                    if (lang == AppLanguage.AR) "إرسال عبر البريد" else "Send via Email"))
                                            }
                                        } else {
                                            val msg = if (lang == AppLanguage.AR) "لا يوجد بريد إلكتروني مسجل" else "No registered email found"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            )


                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, backupCode)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Backup Code"))
                                    }
                            )

                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(backupCode))
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val msg = if (lang == AppLanguage.AR) "تم نسخ رمز الاسترداد الاحتياطي!" else "Backup code copied!"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                    }
                }
            )

            var account by remember { mutableStateOf<com.google.android.gms.auth.api.signin.GoogleSignInAccount?>(null) }
            val driveScope = com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE)
            val gso = remember {
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(driveScope)
                    .build()
            }
            val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

            // Check if account has Drive permission granted
            LaunchedEffect(Unit) {
                val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                if (lastAccount != null && GoogleSignIn.hasPermissions(lastAccount, driveScope)) {
                    account = lastAccount
                } else {
                    // Force re-auth if scope not granted
                    account = null
                }
            }

            val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val signedIn = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                        if (signedIn != null && GoogleSignIn.hasPermissions(signedIn, driveScope)) {
                            account = signedIn
                            Toast.makeText(context, if (lang == AppLanguage.AR) "تم تسجيل الدخول بنجاح" else "Signed in successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            account = null
                            Toast.makeText(context, if (lang == AppLanguage.AR) "لم يتم منح صلاحية Drive، حاول مجدداً" else "Drive permission not granted, try again", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, if (lang == AppLanguage.AR) "فشل تسجيل الدخول: ${e.message}" else "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            ExpandableSettingCard(
                title = if (lang == AppLanguage.AR) "النسخ الاحتياطي والاستعادة" else "Backup & Restore",
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF0F9D58)) },
                isExpanded = expandedCardIndex == 5,
                onExpandClick = { expandedCardIndex = if (expandedCardIndex == 5) -1 else 5 },
                content = {
                    Text(
                        text = if (lang == AppLanguage.AR) 
                            "يمكنك حفظ نسخة مشفرة من قاعدة البيانات الخاصة بك في مجلد QR Vault+_Backup على Google Drive الخاص بك تلقائياً."
                        else 
                            "You can automatically save an encrypted backup to the QR Vault+_Backup folder on your Google Drive.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    
                    if (account == null) {
                        Button(
                            onClick = { signInLauncher.launch(googleSignInClient.signInIntent) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Text(if (lang == AppLanguage.AR) "تسجيل الدخول باستخدام Google" else "Sign In with Google", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "${if (lang == AppLanguage.AR) "متصل كـ:" else "Signed in as:"} ${account?.email}",
                            color = Color(0xFF00F2FE),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { 
                                    isGeneratingBackup = true
                                    coroutineScope.launch {
                                        try {
                                            val keyWithoutHyphens = backupCode.replace("-", "")
                                            val backupFile = DatabaseBackupUtils.getEncryptedBackupFile(context, keyWithoutHyphens)
                                            if (backupFile != null) {
                                                val helper = GoogleDriveBackupHelper(context, account!!)
                                                val success = helper.uploadBackupFile(backupFile)
                                                if (success) {
                                                    context.getSharedPreferences("secure_vault_prefs", Context.MODE_PRIVATE)
                                                        .edit().putLong("last_backup_time", System.currentTimeMillis()).apply()
                                                    val msg = if (lang == AppLanguage.AR) "تم حفظ النسخة الاحتياطية بنجاح على Google Drive" else "Backup saved to Google Drive successfully"
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "Upload failed.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                val msg = if (lang == AppLanguage.AR) "قاعدة البيانات فارغة، لا يوجد شيء لنسخه احتياطياً" else "Database is empty, nothing to backup"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingBackup = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                                modifier = Modifier.weight(1f).height(48.dp),
                                enabled = !isGeneratingBackup,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                if (isGeneratingBackup) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(if (lang == AppLanguage.AR) "النسخ الاحتياطي" else "Upload Backup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            
                            Button(
                                onClick = { 
                                    isGeneratingBackup = true
                                    coroutineScope.launch {
                                        try {
                                            val tempFile = java.io.File(context.cacheDir, "temp_restore.enc")
                                            val helper = GoogleDriveBackupHelper(context, account!!)
                                            val downloaded = helper.downloadBackupFile(tempFile)
                                            
                                            if (downloaded) {
                                                val keyWithoutHyphens = backupCode.replace("-", "")
                                                val success = DatabaseBackupUtils.restoreEncryptedBackupFile(context, android.net.Uri.fromFile(tempFile), keyWithoutHyphens)
                                                if (success) {
                                                    val msg = if (lang == AppLanguage.AR) "تم استعادة النسخة الاحتياطية بنجاح! يرجى إعادة تشغيل التطبيق." else "Backup restored successfully! Please restart the app."
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    kotlinx.coroutines.delay(2000)
                                                    kotlin.system.exitProcess(0)
                                                } else {
                                                    val msg = if (lang == AppLanguage.AR) "فشل في استعادة النسخة. تأكد من أن الرمز صحيح." else "Failed to restore backup. Invalid key."
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                val msg = if (lang == AppLanguage.AR) "لا توجد نسخة احتياطية على Google Drive" else "No backup found on Google Drive"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingBackup = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                modifier = Modifier.weight(1f).height(48.dp),
                                enabled = !isGeneratingBackup,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (lang == AppLanguage.AR) "استعادة" else "Restore Backup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            )


            
            Spacer(modifier = Modifier.height(30.dp))
        }

    }
}

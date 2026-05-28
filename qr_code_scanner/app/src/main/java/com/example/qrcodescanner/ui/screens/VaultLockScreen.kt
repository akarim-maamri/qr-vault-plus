package com.example.qrcodescanner.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Face
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.core.*
import com.example.qrcodescanner.data.BiometricHelper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n
import com.example.qrcodescanner.data.CameraCaptureUtils

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultLockScreen(
    onUnlockAttempt: (String) -> Boolean,
    onRecoverWithBackup: (String, String) -> Boolean,
    onRecoverWithGoogle: (String, String) -> Boolean,
    isGoogleLinked: Boolean,
    currentUserEmail: String?,
    lang: AppLanguage,
    isBiometricEnabled: Boolean = false,
    biometricEncryptedData: Pair<String, String>? = null,
    onBiometricUnlock: (String) -> Unit = {},
    onDisableBiometrics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    
    // Recovery Popup States
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryMethod by remember { mutableStateOf(0) } // 0 for Google Email, 1 for Backup Code
    
    var emailInput by remember { mutableStateOf(currentUserEmail ?: "") }
    var backupCodeInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmNewPinInput by remember { mutableStateOf("") }

    LaunchedEffect(pinInput) {
        if (pinInput.length == 4) {
            delay(150) // Small delay for visual completion
            val success = onUnlockAttempt(pinInput)
            if (success) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                failedAttempts = 0
                // Vault successfully unlocked! Parent state will recompose and show Dashboard
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isError = true
                failedAttempts++
                
                if (failedAttempts == 3) {
                    coroutineScope.launch {
                        try {
                            // The photo is automatically saved to internal storage by CameraCaptureUtils
                            val photo = CameraCaptureUtils.captureFrontCameraImage(context, lifecycleOwner)
                            if (photo != null) {
                                android.widget.Toast.makeText(context, if (lang == AppLanguage.AR) "تم التقاط صورة الدخيل" else "Intruder selfie captured", android.widget.Toast.LENGTH_SHORT).show()
                                val prefs = context.getSharedPreferences("VaultPrefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().putString("new_intruder_photo", photo.absolutePath).apply()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    failedAttempts = 0 // Reset after attempting to capture
                }

                delay(400)
                pinInput = ""
                isError = false
            }
        }
    }

    val glowColor by animateColorAsState(
        targetValue = if (isError) Color(0xFFFE4A49) else Color(0xFF00F2FE),
        label = "GlowColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Header Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            // Glowing Lock Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = 0.08f))
                    .border(2.dp, glowColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vault Locked",
                    tint = glowColor,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = L10n.get("vault_title", lang).uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isError) L10n.get("incorrect_pin_toast", lang) else L10n.get("vault_subtitle", lang),
                color = if (isError) Color(0xFFFE4A49) else Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        // Middle Dots Section
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            for (i in 0 until 4) {
                val active = i < pinInput.length
                val dotColor by animateColorAsState(
                    targetValue = if (isError) Color(0xFFFE4A49) else if (active) Color(0xFF00F2FE) else Color(0xFF262626),
                    label = "DotColor"
                )
                
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(
                            width = 1.dp,
                            color = if (active) Color.Transparent else Color(0xFF333333),
                            shape = CircleShape
                        )
                )
            }
        }

        // Bottom Numpad Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val numpadRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (isBiometricEnabled) "fingerprint" else "", "0", "back")
            )

            for (row in numpadRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (key in row) {
                        if (key.isEmpty()) {
                            // Blank filler space
                            Box(modifier = Modifier.size(72.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A1A1A))
                                    .border(1.dp, Color(0xFF262626), CircleShape)
                                    .clickable {
                                        if (key == "back") {
                                            if (pinInput.isNotEmpty()) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                pinInput = pinInput.dropLast(1)
                                            }
                                        } else if (key == "fingerprint") {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val activity = context as? FragmentActivity
                                            if (activity != null && biometricEncryptedData != null) {
                                                BiometricHelper.showBiometricPromptForDecryption(
                                                    activity = activity,
                                                    encryptedData = biometricEncryptedData.first,
                                                    ivData = biometricEncryptedData.second,
                                                    onSuccess = { masterKey -> onBiometricUnlock(masterKey) },
                                                    onError = { err ->
                                                        if (err == "KEY_INVALIDATED") {
                                                            onDisableBiometrics()
                                                            val msg = if (lang == AppLanguage.AR) "تم تغيير بصمات الجهاز، يرجى الدخول بالرمز السري وتفعيل البصمة من جديد." else "Device biometrics changed. Please login with PIN and re-enroll."
                                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(context, "Biometric failed: $err", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                )
                                            } else {
                                                Toast.makeText(context, "Biometric data missing", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            if (pinInput.length < 4) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                pinInput += key
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                    if (key == "back") {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Delete",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else if (key == "fingerprint") {
                                        val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
                                        val scale by infiniteTransition.animateFloat(
                                            initialValue = 1f,
                                            targetValue = 1.15f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1000, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "pulse"
                                        )
                                        var showFace by remember { mutableStateOf(true) }
                                        LaunchedEffect(Unit) {
                                            while (true) {
                                                kotlinx.coroutines.delay(2000)
                                                showFace = !showFace
                                            }
                                        }

                                        Icon(
                                            imageVector = if (showFace) Icons.Default.Face else Icons.Default.Fingerprint,
                                            contentDescription = "Biometric",
                                            tint = Color(0xFF00F2FE),
                                            modifier = Modifier.size(32.dp).graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        // Forgot PIN Trigger - visible under numpad
        Text(
            text = L10n.get("btn_forgot_pin", lang),
            color = Color(0xFF00F2FE),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { showRecoveryDialog = true }
                .padding(vertical = 8.dp)
        )
    }

    // Gorgeous Cyberpunk Recovery Dialog
    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            containerColor = Color(0xFF1E1E1E),
            modifier = Modifier.border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF00F2FE))
                    Text(
                        text = L10n.get("dialog_recovery_title", lang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Method Selection Segmented Tabs
                    TabRow(
                        selectedTabIndex = recoveryMethod,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF00F2FE),
                        divider = { Divider(color = Color(0xFF262626)) }
                    ) {
                        Tab(
                            selected = recoveryMethod == 0,
                            onClick = { recoveryMethod = 0 },
                            text = {
                                Text(
                                    if (lang == AppLanguage.AR) "إيميل + رمز الطوارئ" else "Email + Code",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        Tab(
                            selected = recoveryMethod == 1,
                            onClick = { recoveryMethod = 1 },
                            text = {
                                Text(
                                    if (lang == AppLanguage.AR) "رمز الطوارئ فقط" else "Code Only",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }

                    if (recoveryMethod == 0) {
                        // Email + Backup Code recovery
                        Text(
                            text = if (lang == AppLanguage.AR)
                                "أدخل إيميل التفعيل ورمز الطوارئ المرسل إليك. سيتم استخدام رمز الطوارئ لاستعادة الخزنة."
                            else
                                "Enter your activation email and the backup code sent to you. The code will be used to recover your vault.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = {
                                Text(
                                    if (lang == AppLanguage.AR) "إيميل التفعيل" else "Activation Email"
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF87),
                                focusedLabelColor = Color(0xFF00FF87),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = backupCodeInput,
                            onValueChange = { backupCodeInput = it.uppercase() },
                            label = { Text(if (lang == AppLanguage.AR) "رمز الطوارئ" else "Backup Code") },
                            placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00F2FE),
                                focusedLabelColor = Color(0xFF00F2FE),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        // Backup Code only recovery
                        Text(
                            text = if (lang == AppLanguage.AR) "أدخل رمز الاسترداد المكون من 16 حرفاً (XXXX-XXXX-XXXX-XXXX) الذي تم توليده تلقائياً عند إعداد الخزنة." else "Enter the 16-character emergency code (XXXX-XXXX-XXXX-XXXX) generated during vault setup.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = backupCodeInput,
                            onValueChange = { backupCodeInput = it.uppercase() },
                            label = { Text(L10n.get("backup_code_placeholder", lang)) },
                            placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00F2FE),
                                focusedLabelColor = Color(0xFF00F2FE),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Always show new PIN fields
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4) newPinInput = it },
                        label = { Text(L10n.get("new_pin_placeholder", lang)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F2FE),
                            focusedLabelColor = Color(0xFF00F2FE),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = confirmNewPinInput,
                        onValueChange = { if (it.length <= 4) confirmNewPinInput = it },
                        label = { Text(L10n.get("field_confirm_pin", lang)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F2FE),
                            focusedLabelColor = Color(0xFF00F2FE),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                val canSubmit = backupCodeInput.length >= 16 &&
                        newPinInput.length == 4 && confirmNewPinInput.length == 4

                Button(
                    onClick = {
                        if (newPinInput != confirmNewPinInput) {
                            Toast.makeText(context, if (lang == AppLanguage.AR) "الرموز المدخلة غير متطابقة!" else "Les codes PIN ne correspondent pas!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Both methods use recoverWithBackupCode - email tab just shows email as reminder
                        val success = onRecoverWithBackup(backupCodeInput.trim(), newPinInput)
                        if (success) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, L10n.get("recovery_success_toast", lang), Toast.LENGTH_LONG).show()
                            showRecoveryDialog = false
                            emailInput = ""
                            backupCodeInput = ""
                            newPinInput = ""
                            confirmNewPinInput = ""
                        } else {
                            Toast.makeText(
                                context,
                                if (lang == AppLanguage.AR) "رمز الطوارئ غير صحيح. تحقق من الرمز المرسل إلى إيميلك" else "Invalid backup code. Check the code sent to your email.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE), disabledContainerColor = Color(0xFF262626))
                ) {
                    Text(
                        L10n.get("btn_recover", lang), 
                        color = if (canSubmit) Color.Black else Color.Gray, 
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) {
                    Text(L10n.get("btn_cancel", lang), color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

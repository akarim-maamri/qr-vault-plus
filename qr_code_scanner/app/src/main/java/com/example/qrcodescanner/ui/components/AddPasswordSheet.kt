package com.example.qrcodescanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrcodescanner.data.CryptoUtils
import com.example.qrcodescanner.data.PasswordEntity
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordSheet(
    activePin: String,
    onDismiss: () -> Unit,
    onSave: (PasswordEntity) -> Unit,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Password generator configurations
    var length by remember { mutableFloatStateOf(16f) }
    var includeUpper by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }

    val strengthColorAndText = remember(password, lang) {
        when {
            password.isEmpty() -> Color.Gray to L10n.get("no_password_strength", lang)
            password.length < 8 -> Color(0xFFFE4A49) to L10n.get("pass_gen_strength_weak", lang)
            password.length < 12 -> Color(0xFFFFD97D) to L10n.get("pass_gen_strength_medium", lang)
            password.length < 16 -> Color(0xFF00FF87) to L10n.get("pass_gen_strength_strong", lang)
            else -> Color(0xFF00F2FE) to L10n.get("pass_gen_strength_very_secure", lang)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(40.dp, 4.dp)
                    .background(Color(0xFF333333), RoundedCornerShape(2.dp))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    L10n.get("sheet_add_password", lang).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Fields Form
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Account Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(L10n.get("field_title", lang), color = Color.Gray) },
                    placeholder = { Text(if (lang == AppLanguage.AR) "مثال: جوجل، شبكة واي فاي" else "ex: Google, Réseau WiFi", color = Color.DarkGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00F2FE),
                        unfocusedBorderColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Username / Email
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(L10n.get("field_username", lang), color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00F2FE),
                        unfocusedBorderColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(L10n.get("field_password", lang), color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00F2FE),
                        unfocusedBorderColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Dynamic strength bar
                if (password.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(strengthColorAndText.first)
                        )
                        Text(
                            text = strengthColorAndText.second,
                            color = strengthColorAndText.first,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Password Generator Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = null,
                                tint = Color(0xFF00F2FE),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (lang == AppLanguage.AR) "مولد كلمات المرور السيبراني" else "Cyber Password Generator",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Length Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(L10n.get("pass_gen_length", lang), color = Color.Gray, fontSize = 12.sp)
                                Text(if (lang == AppLanguage.AR) "${length.toInt()} رموز" else "${length.toInt()} caractères", color = Color(0xFF00F2FE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = length,
                                onValueChange = { length = it },
                                valueRange = 8f..32f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00F2FE),
                                    activeTrackColor = Color(0xFF00F2FE),
                                    inactiveTrackColor = Color(0xFF262626)
                                )
                            )
                        }

                        // Checkboxes Options Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = includeUpper,
                                    onCheckedChange = { includeUpper = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00F2FE))
                                )
                                Text(L10n.get("pass_gen_caps", lang), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = includeNumbers,
                                    onCheckedChange = { includeNumbers = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00F2FE))
                                )
                                Text(L10n.get("pass_gen_nums", lang), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = includeSymbols,
                                    onCheckedChange = { includeSymbols = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00F2FE))
                                )
                                Text(L10n.get("pass_gen_symbols", lang), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Generate Button
                        Button(
                            onClick = {
                                password = generateRandomPassword(
                                    length = length.toInt(),
                                    includeUpper = includeUpper,
                                    includeNumbers = includeNumbers,
                                    includeSymbols = includeSymbols
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE).copy(alpha = 0.1f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (lang == AppLanguage.AR) "توليد كلمة مرور" else "Générer le mot de passe", color = Color(0xFF00F2FE), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Optional URL
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(if (lang == AppLanguage.AR) "رابط الموقع (اختياري)" else "URL du site Web (Optionnel)", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00F2FE),
                        unfocusedBorderColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (lang == AppLanguage.AR) "ملاحظات إضافية (اختياري)" else "Notes (Optionnel)", color = Color.Gray) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00F2FE),
                        unfocusedBorderColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save and Cancel Buttons
            Button(
                onClick = {
                    if (title.isNotEmpty() && password.isNotEmpty()) {
                        // Encrypt password before saving
                        val encryptedPass = CryptoUtils.encrypt(password, activePin)
                        onSave(
                            PasswordEntity(
                                title = title,
                                username = username,
                                passwordEncrypted = encryptedPass,
                                url = url,
                                notes = notes,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotEmpty() && password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00F2FE),
                    disabledContainerColor = Color(0xFF262626)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    L10n.get("btn_save", lang),
                    color = if (title.isNotEmpty() && password.isNotEmpty()) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun generateRandomPassword(
    length: Int,
    includeUpper: Boolean,
    includeNumbers: Boolean,
    includeSymbols: Boolean
): String {
    val lower = "abcdefghijklmnopqrstuvwxyz"
    val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val numbers = "0123456789"
    val symbols = "!@#$%^&*()_-+=<>?/"
    
    val charPool = StringBuilder(lower)
    if (includeUpper) charPool.append(upper)
    if (includeNumbers) charPool.append(numbers)
    if (includeSymbols) charPool.append(symbols)
    
    val random = SecureRandom()
    return (1..length)
        .map { charPool[random.nextInt(charPool.length)] }
        .joinToString("")
}

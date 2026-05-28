package com.example.qrcodescanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.qrcodescanner.data.CryptoUtils
import com.example.qrcodescanner.data.PasswordEntity
import com.example.qrcodescanner.data.SecureNoteEntity
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n
import com.example.qrcodescanner.ui.components.AddPasswordSheet
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    activePin: String,
    passwordsList: List<PasswordEntity>,
    secureNotesList: List<SecureNoteEntity>,
    onSavePassword: (PasswordEntity) -> Unit,
    onDeletePassword: (PasswordEntity) -> Unit,
    onSaveNote: (SecureNoteEntity) -> Unit,
    onUpdateNote: (SecureNoteEntity) -> Unit,
    onDeleteNote: (SecureNoteEntity) -> Unit,
    onLockVault: () -> Unit,
    onOpenSettings: () -> Unit,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    // Tab segment: 0 for Passwords, 1 for Secure Notes
    var selectedSubTab by remember { mutableStateOf(0) }
    
    // Modals visibility
    var showAddPasswordSheet by remember { mutableStateOf(false) }
    var showAddNoteSheet by remember { mutableStateOf(false) }
    
    // View/Edit Note State
    var activeNoteForEdit by remember { mutableStateOf<SecureNoteEntity?>(null) }
    
    // For showing QR Code dialog
    var qrPasswordEntity by remember { mutableStateOf<PasswordEntity?>(null) }
    
    // For confirming delete
    var deleteConfirmPasswordEntity by remember { mutableStateOf<PasswordEntity?>(null) }
    var deleteConfirmNoteEntity by remember { mutableStateOf<SecureNoteEntity?>(null) }

    // New intruder popup
    val prefs = context.getSharedPreferences("VaultPrefs", Context.MODE_PRIVATE)
    var newIntruderPhotoPath by remember { mutableStateOf(prefs.getString("new_intruder_photo", null)) }

    // Filtering passwords based on search query
    val filteredPasswords = remember(passwordsList, searchQuery) {
        if (searchQuery.isBlank()) {
            passwordsList
        } else {
            passwordsList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.username.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Filtering notes based on search query
    val filteredNotes = remember(secureNotesList, searchQuery) {
        if (searchQuery.isBlank()) {
            secureNotesList
        } else {
            secureNotesList.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        L10n.get("vault_dashboard_title", lang),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    // Settings gear icon to access control panel
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = if (lang == AppLanguage.AR) "إعدادات الخزنة" else "Paramètres du coffre-fort",
                            tint = Color(0xFF00F2FE)
                        )
                    }
                    // Lock button to quickly secure the vault
                    IconButton(onClick = onLockVault) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = if (lang == AppLanguage.AR) "قفل الخزنة" else "Verrouiller le coffre-fort",
                            tint = Color(0xFFFE4A49)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        floatingActionButton = {
                if (selectedSubTab != 2) {
                    FloatingActionButton(
                        onClick = {
                            if (selectedSubTab == 0) {
                                showAddPasswordSheet = true
                            } else if (selectedSubTab == 1) {
                                showAddNoteSheet = true
                            }
                        },
                        containerColor = Color(0xFF00F2FE),
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (selectedSubTab == 0) L10n.get("btn_add_password", lang) else L10n.get("btn_add_note", lang),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
        },
        containerColor = Color(0xFF121212),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Segmented sub-tabs at the top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A1A))
                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeTabColor = Color(0xFF00F2FE)
                    
                    // Tab 1: Passwords
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedSubTab == 0) activeTabColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedSubTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = if (selectedSubTab == 0) activeTabColor else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = L10n.get("tab_passwords", lang),
                                color = if (selectedSubTab == 0) activeTabColor else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    
                    // Tab 2: Secure Notes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedSubTab == 1) activeTabColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedSubTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = if (selectedSubTab == 1) activeTabColor else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = L10n.get("tab_notes", lang),
                                color = if (selectedSubTab == 1) activeTabColor else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Tab 3: Intruders
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedSubTab == 2) activeTabColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedSubTab = 2 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (selectedSubTab == 2) activeTabColor else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (lang == AppLanguage.AR) "الدخلاء" else "Intruders",
                                color = if (selectedSubTab == 2) activeTabColor else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Search Bar Section (hidden for Intruders)
                if (selectedSubTab != 2) {
                    OutlinedTextField(
                        value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            text = if (selectedSubTab == 0) L10n.get("search_placeholder", lang) else L10n.get("search_notes_placeholder", lang), 
                            color = Color.Gray,
                            fontSize = 13.sp
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Gray
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00F2FE),
                        unfocusedBorderColor = Color(0xFF262626),
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                if (selectedSubTab == 0) {
                    // Passwords list
                    if (filteredPasswords.isEmpty()) {
                        EmptyVaultState(
                            title = if (searchQuery.isEmpty()) L10n.get("vault_empty", lang) else L10n.get("no_results", lang),
                            description = if (searchQuery.isEmpty()) {
                                if (lang == AppLanguage.AR) "اضغط على زر الـ + بالأسفل لحفظ أول كلمة مرور مشفرة بشكل آمن."
                                else "Appuyez sur le bouton + ci-dessous pour enregistrer votre premier mot de passe chiffré en toute sécurité."
                            } else {
                                if (lang == AppLanguage.AR) "جرب تغيير كلمات البحث."
                                else "Essayez de modifier vos termes de recherche."
                            },
                            icon = Icons.Default.Security
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredPasswords, key = { it.id }) { passwordEntity ->
                                PasswordItemRow(
                                    passwordEntity = passwordEntity,
                                    activePin = activePin,
                                    onShowQr = { qrPasswordEntity = passwordEntity },
                                    onDelete = { deleteConfirmPasswordEntity = passwordEntity }
                                )
                            }
                        }
                    }
                } else if (selectedSubTab == 1) {
                    // Secure Notes list
                    if (filteredNotes.isEmpty()) {
                        EmptyVaultState(
                            title = if (searchQuery.isEmpty()) (if (lang == AppLanguage.AR) "لا توجد ملاحظات آمنة" else "Aucune note sécurisée") else L10n.get("no_results", lang),
                            description = if (searchQuery.isEmpty()) {
                                if (lang == AppLanguage.AR) "انقر على زر + بالأسفل لحفظ أول ملاحظة مشفرة لك."
                                else "Appuyez sur le bouton + ci-dessous pour enregistrer votre première note chiffrée."
                            } else {
                                if (lang == AppLanguage.AR) "جرب تغيير كلمات البحث."
                                else "Essayez de modifier vos termes de recherche."
                            },
                            icon = Icons.Default.Description
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredNotes, key = { it.id }) { noteEntity ->
                                NoteItemRow(
                                    noteEntity = noteEntity,
                                    activePin = activePin,
                                    onViewOrEdit = { activeNoteForEdit = noteEntity },
                                    onDelete = { deleteConfirmNoteEntity = noteEntity },
                                    lang = lang
                                )
                            }
                        }
                    }
                } else if (selectedSubTab == 2) {
                    IntruderGalleryScreen(lang = lang, modifier = Modifier.weight(1f))
                }
            }
        }

        // Add Password Bottom Sheet Form
        if (showAddPasswordSheet) {
            AddPasswordSheet(
                activePin = activePin,
                onDismiss = { showAddPasswordSheet = false },
                onSave = onSavePassword,
                lang = lang
            )
        }

        // Add Note Bottom Sheet Form
        if (showAddNoteSheet) {
            AddEditNoteSheet(
                activePin = activePin,
                noteEntity = null,
                onDismiss = { showAddNoteSheet = false },
                onSave = {
                    onSaveNote(it)
                    showAddNoteSheet = false
                },
                lang = lang
            )
        }

        // Edit Note Bottom Sheet Form
        activeNoteForEdit?.let { note ->
            AddEditNoteSheet(
                activePin = activePin,
                noteEntity = note,
                onDismiss = { activeNoteForEdit = null },
                onSave = { updatedNote ->
                    onUpdateNote(updatedNote)
                    activeNoteForEdit = null
                },
                lang = lang
            )
        }

        // Intruder Popup
        if (newIntruderPhotoPath != null) {
            val file = File(newIntruderPhotoPath!!)
            if (file.exists()) {
                val bitmap = remember(file) { BitmapFactory.decodeFile(file.absolutePath) }
                AlertDialog(
                    onDismissRequest = {
                        prefs.edit().remove("new_intruder_photo").apply()
                        newIntruderPhotoPath = null
                    },
                    containerColor = Color(0xFF1E1E1E),
                    title = {
                        Text(
                            text = if (lang == AppLanguage.AR) "تحذير: محاولة دخول فاشلة" else "Warning: Failed Login Attempt",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = if (lang == AppLanguage.AR) "تم التقاط هذه الصورة للدخيل أثناء محاولة فتح الخزنة." else "This photo was captured during a failed login attempt.",
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Intruder",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                prefs.edit().remove("new_intruder_photo").apply()
                                newIntruderPhotoPath = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE4A49)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (lang == AppLanguage.AR) "حسناً" else "OK", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            } else {
                prefs.edit().remove("new_intruder_photo").apply()
                newIntruderPhotoPath = null
            }
        }

        // Share/Show QR Code Dialog
        qrPasswordEntity?.let { password ->
            val decryptedPassword = remember(password, activePin) {
                CryptoUtils.decrypt(password.passwordEncrypted, activePin)
            }
            val qrTextContent = "TITLE:${password.title}\nUSER:${password.username}\nPASS:${decryptedPassword}"
            val qrBitmap = remember(qrTextContent) {
                generateQrCodeBitmap(qrTextContent)
            }

            AlertDialog(
                onDismissRequest = { qrPasswordEntity = null },
                containerColor = Color(0xFF1E1E1E),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = password.title.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (lang == AppLanguage.AR) "رمز الوصول الآمن" else "Code d'accès sécurisé",
                            color = Color(0xFF00F2FE),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(3.dp, Color(0xFF00F2FE), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = if (lang == AppLanguage.AR) "رمز استجابة سريعة للاعتماد" else "QR Code d'identification",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (lang == AppLanguage.AR) "امسح هذا الرمز باستخدام جهاز آخر لتستورد تفاصيل الحساب بشكل آمن وتلقائي." else "Scannez ce code avec un autre appareil pour importer les détails du compte en toute sécurité.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            qrBitmap?.let { shareQrCode(context, it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.get("btn_share", lang), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { qrPasswordEntity = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                    ) {
                        Text(L10n.get("btn_close", lang), fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Delete Password Confirmation Dialog
        deleteConfirmPasswordEntity?.let { password ->
            AlertDialog(
                onDismissRequest = { deleteConfirmPasswordEntity = null },
                containerColor = Color(0xFF1E1E1E),
                title = {
                    Text(
                        if (lang == AppLanguage.AR) "حذف البيانات؟" else "Supprimer les données?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        if (lang == AppLanguage.AR) "هل أنت متأكد من رغبتك في حذف بيانات الحساب \"${password.title}\" نهائياً؟ لا يمكن التراجع عن هذا الإجراء." else "Êtes-vous sûr de vouloir supprimer définitivement les données de \"${password.title}\"? Cette action est irréversible.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeletePassword(password)
                            deleteConfirmPasswordEntity = null
                            Toast.makeText(context, if (lang == AppLanguage.AR) "تم حذف الحساب بنجاح!" else "Compte supprimé avec succès!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE4A49)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (lang == AppLanguage.AR) "حذف" else "Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { deleteConfirmPasswordEntity = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                    ) {
                        Text(L10n.get("btn_cancel", lang))
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Delete Note Confirmation Dialog
        deleteConfirmNoteEntity?.let { note ->
            AlertDialog(
                onDismissRequest = { deleteConfirmNoteEntity = null },
                containerColor = Color(0xFF1E1E1E),
                title = {
                    Text(
                        if (lang == AppLanguage.AR) "حذف الملاحظة؟" else "Supprimer la note?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        if (lang == AppLanguage.AR) "هل أنت متأكد من رغبتك في حذف الملاحظة \"${note.title}\" نهائياً؟ لا يمكن استعادتها لاحقاً." else "Êtes-vous sûr de vouloir supprimer définitivement la note \"${note.title}\"? Elle ne pourra pas être récupérée.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteNote(note)
                            deleteConfirmNoteEntity = null
                            Toast.makeText(context, if (lang == AppLanguage.AR) "تم حذف الملاحظة!" else "Note supprimée!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFE4A49)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (lang == AppLanguage.AR) "حذف الملاحظة" else "Supprimer la note", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { deleteConfirmNoteEntity = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                    ) {
                        Text(L10n.get("btn_cancel", lang))
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun EmptyVaultState(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color(0xFF00F2FE).copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(54.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun NoteItemRow(
    noteEntity: SecureNoteEntity,
    activePin: String,
    onViewOrEdit: () -> Unit,
    onDelete: () -> Unit,
    lang: AppLanguage
) {
    val context = LocalContext.current
    
    // Decrypt a quick snippet of content to show as a preview
    val contentPreview = remember(noteEntity.contentEncrypted, activePin, lang) {
        try {
            val decrypted = CryptoUtils.decrypt(noteEntity.contentEncrypted, activePin)
            if (decrypted.length > 60) decrypted.take(60) + "..." else decrypted
        } catch (e: Exception) {
            if (lang == AppLanguage.AR) "[قراءة الملاحظة غير متوفرة]" else "[Lecture de la note indisponible]"
        }
    }

    val formattedDate = remember(noteEntity.timestamp) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(noteEntity.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp))
            .clickable { onViewOrEdit() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF00F2FE).copy(alpha = 0.08f))
                .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color(0xFF00F2FE),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Note Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = noteEntity.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = contentPreview,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDate,
                color = Color(0xFF00F2FE).copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Light
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Operations Panel
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // View / Edit Button
            IconButton(
                onClick = onViewOrEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Note",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Note",
                    tint = Color(0xFFFE4A49),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteSheet(
    activePin: String,
    noteEntity: SecureNoteEntity?,
    onDismiss: () -> Unit,
    onSave: (SecureNoteEntity) -> Unit,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(noteEntity?.title ?: "") }
    
    // Decrypt content on open
    val initialContent = remember(noteEntity, activePin) {
        if (noteEntity != null) {
            CryptoUtils.decrypt(noteEntity.contentEncrypted, activePin)
        } else {
            ""
        }
    }
    var content by remember { mutableStateOf(initialContent) }
    var isSaving by remember { mutableStateOf(false) }
    
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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        // Removed fillMaxHeight to let it wrap content appropriately on small screens
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (noteEntity == null) L10n.get("sheet_add_note", lang) else L10n.get("sheet_edit_note", lang),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (lang == AppLanguage.AR) "عنوان الملاحظة" else "Titre de la note", color = Color.Gray) },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Content Editor
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { 
                    Text(
                        if (lang == AppLanguage.AR) "اكتب ملاحظتك المشفرة هنا..." else "Écrivez votre note chiffrée ici...", 
                        color = Color.DarkGray,
                        fontSize = 13.sp
                    ) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00F2FE),
                    unfocusedBorderColor = Color(0xFF333333)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp), // Fixed smaller height to fit phones without hiding the save button
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save buttons
            Button(
                onClick = {
                    if (title.isNotEmpty() && content.isNotEmpty() && !isSaving) {
                        isSaving = true
                        val encryptedContent = CryptoUtils.encrypt(content, activePin)
                        val note = SecureNoteEntity(
                            id = noteEntity?.id ?: 0,
                            title = title,
                            contentEncrypted = encryptedContent,
                            timestamp = System.currentTimeMillis()
                        )
                        onSave(note)
                    }
                },
                enabled = title.isNotEmpty() && content.isNotEmpty() && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00F2FE),
                    disabledContainerColor = Color(0xFF262626)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = L10n.get("btn_save", lang),
                    color = if (title.isNotEmpty() && content.isNotEmpty()) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PasswordItemRow(
    passwordEntity: PasswordEntity,
    activePin: String,
    onShowQr: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Decrypt securely on-demand using the key derived from the active Master PIN
    val decryptedPassword = remember(passwordEntity.passwordEncrypted, activePin, isPasswordVisible) {
        if (isPasswordVisible) {
            CryptoUtils.decrypt(passwordEntity.passwordEncrypted, activePin)
        } else {
            "••••••••"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Futuristic Logo/Avatar with glowing cyan accent
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF00F2FE).copy(alpha = 0.08f))
                .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val char = if (passwordEntity.title.isNotEmpty()) passwordEntity.title[0].uppercaseChar() else 'P'
            Text(
                text = char.toString(),
                color = Color(0xFF00F2FE),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Credential Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = passwordEntity.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (passwordEntity.username.isNotEmpty()) passwordEntity.username else "No email / username",
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            // Password Field
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = decryptedPassword,
                    color = if (isPasswordVisible) Color(0xFF00FF87) else Color.DarkGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = if (isPasswordVisible) 0.5.sp else 2.sp
                )
                
                // Eyeball Visibility Icon
                Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle password visibility",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { isPasswordVisible = !isPasswordVisible }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Operations Panel
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Copy Password Button
            IconButton(
                onClick = {
                    val pass = CryptoUtils.decrypt(passwordEntity.passwordEncrypted, activePin)
                    copyToClipboard(context, pass)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Password",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // QR Code button
            IconButton(
                onClick = onShowQr,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Show QR Code",
                    tint = Color(0xFF00F2FE),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete row button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete account",
                    tint = Color(0xFFFE4A49),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Password Vault", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Password copied to clipboard!", Toast.LENGTH_SHORT).show()
}

// Helpers
private fun generateQrCodeBitmap(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

private fun shareQrCode(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "vault_qr.png")
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
        context.startActivity(Intent.createChooser(shareIntent, "Share Vault QR Code"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing QR Code", Toast.LENGTH_SHORT).show()
    }
}

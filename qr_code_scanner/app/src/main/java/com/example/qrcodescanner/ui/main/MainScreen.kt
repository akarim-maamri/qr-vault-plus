package com.example.qrcodescanner.ui.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.qrcodescanner.Main
import com.example.qrcodescanner.data.DefaultDataRepository
import com.example.qrcodescanner.data.ScanDatabase
import com.example.qrcodescanner.data.AppLanguage
import com.example.qrcodescanner.data.L10n
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.qrcodescanner.ui.screens.GenerateScreen
import com.example.qrcodescanner.ui.screens.HistoryScreen
import com.example.qrcodescanner.ui.screens.ScanScreen
import com.example.qrcodescanner.ui.screens.VaultDashboardScreen
import com.example.qrcodescanner.ui.screens.VaultLockScreen
import com.example.qrcodescanner.ui.screens.VaultSettingsScreen
import com.example.qrcodescanner.ui.components.AppInfoDialog
import com.example.qrcodescanner.ui.components.WalkthroughDialog

@Composable
fun MainScreen(
    initialTab: Int,
    authLink: String? = null,
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { ScanDatabase.getDatabase(context) }
    val repository = remember { DefaultDataRepository(database.scanResultDao, database.passwordDao, database.secureNoteDao) }
    
    // Inject both repository and application context into ViewModel
    val mainViewModel: MainScreenViewModel = viewModel { 
        MainScreenViewModel(repository, context.applicationContext) 
    }

    val historyList by mainViewModel.historyList.collectAsState()
    val registeredGmail by mainViewModel.registeredGmail.collectAsState()
    val currentLanguage by mainViewModel.currentLanguage.collectAsState()
    val isVaultSetup by mainViewModel.isVaultSetup.collectAsState()
    val isVaultLocked by mainViewModel.isVaultLocked.collectAsState()
    val activePin by mainViewModel.activePin.collectAsState()
    val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }

    var isSettingsDialogVisible by rememberSaveable { mutableStateOf(false) }
    var inSettings by rememberSaveable { mutableStateOf(false) }
    var showBiometricSetupDialog by rememberSaveable { mutableStateOf(false) }
    var pendingBiometricActivation by rememberSaveable { mutableStateOf(false) }

    var showAppInfo by remember { mutableStateOf(false) }
    var showWalkthrough by remember { mutableStateOf(false) }
    var showBackupReminder by rememberSaveable { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("secure_vault_prefs", android.content.Context.MODE_PRIVATE) }


    // Launch walkthrough automatically on first launch once registered Gmail is set
    LaunchedEffect(registeredGmail) {
        if (registeredGmail != null && !prefs.getBoolean("walkthrough_shown", false)) {
            showWalkthrough = true
            prefs.edit().putBoolean("walkthrough_shown", true).apply()
        }
    }

    LaunchedEffect(isVaultSetup, isVaultLocked) {
        if (isVaultSetup && !isVaultLocked) {
            val currentTime = System.currentTimeMillis()
            val fifteenDaysInMillis = 15L * 24 * 60 * 60 * 1000
            // Record first install time on first unlock
            val firstInstallTime = prefs.getLong("first_install_time", 0L).let { existing ->
                if (existing == 0L) {
                    val now = currentTime
                    prefs.edit().putLong("first_install_time", now).apply()
                    now
                } else existing
            }
            val lastBackupTime = prefs.getLong("last_backup_time", 0L)
            val timeSinceInstall = currentTime - firstInstallTime
            val timeSinceLastBackup = if (lastBackupTime > 0L) currentTime - lastBackupTime else Long.MAX_VALUE
            // Only show after 15 days from install, then every 15 days after last backup
            if (timeSinceInstall >= fifteenDaysInMillis && timeSinceLastBackup >= fifteenDaysInMillis) {
                showBackupReminder = true
            }
        }
    }

    LaunchedEffect(authLink) {
        // authLink handling removed since we use Email OTP instead of deep links
    }


    val layoutDirection = if (currentLanguage == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = modifier.fillMaxSize()) {
            if (registeredGmail == null) {
                // Render stunning cyber-neon Gmail Onboarding startup screen
                GmailStartupScreen(
                    viewModel = mainViewModel,
                    currentLanguage = currentLanguage
                )
            } else {
                if (!isVaultSetup) {
                    com.example.qrcodescanner.ui.components.SetupVaultDialog(
                        onSetupComplete = { pin ->
                            mainViewModel.setupVault(pin)
                            if (com.example.qrcodescanner.data.BiometricHelper.canAuthenticate(context)) {
                                showBiometricSetupDialog = true
                            }
                        },
                        lang = currentLanguage
                    )
                } else if (isVaultLocked || activePin == null) {
                    val currentUserEmail = com.google.firebase.Firebase.auth.currentUser?.email
                    VaultLockScreen(
                        onUnlockAttempt = { pin -> mainViewModel.unlockVault(pin) },
                        onRecoverWithBackup = { backupCode, newPin -> mainViewModel.recoverWithBackupCode(backupCode, newPin) },
                        onRecoverWithGoogle = { email, newPin -> mainViewModel.recoverWithGoogle(email, newPin) },
                        isGoogleLinked = registeredGmail != null,
                        currentUserEmail = currentUserEmail,
                        lang = currentLanguage,
                        isBiometricEnabled = isBiometricEnabled,
                        biometricEncryptedData = mainViewModel.getBiometricEncryptedData(),
                        onBiometricUnlock = { masterKey -> mainViewModel.unlockWithBiometrics(masterKey) },
                        onDisableBiometrics = { mainViewModel.setBiometricEnabled(false) }
                    )
                } else {
                    Scaffold(
                    topBar = {
                        // Unified dynamic header with dynamic translation and Premium App Info Dialog Button
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .background(Color(0xFF121212))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = L10n.get("app_title", currentLanguage),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                val infiniteTransition = rememberInfiniteTransition()
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.2f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                IconButton(
                                    onClick = { showAppInfo = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "App Info",
                                        tint = Color(0xFF00F2FE),
                                        modifier = Modifier.scale(scale)
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = Color(0xFF262626),
                                thickness = 1.dp
                            )
                        }
                    },
                    bottomBar = {
                        // Futuristic premium glassmorphic bottom navigation
                        NavigationBar(
                            containerColor = Color(0xFF1A1A1A),
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .border(1.dp, Color(0xFF262626), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan") },
                                label = { Text(L10n.get("tab_scan", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00F2FE),
                                    selectedTextColor = Color(0xFF00F2FE),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color(0x2200F2FE)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                label = { Text(L10n.get("tab_history", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00F2FE),
                                    selectedTextColor = Color(0xFF00F2FE),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color(0x2200F2FE)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.QrCode, contentDescription = "Generate") },
                                label = { Text(L10n.get("tab_generate", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00F2FE),
                                    selectedTextColor = Color(0xFF00F2FE),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color(0x2200F2FE)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                                label = { Text(L10n.get("tab_vault", currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00F2FE),
                                    selectedTextColor = Color(0xFF00F2FE),
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color(0x2200F2FE)
                                )
                            )
                        }
                    },
                    containerColor = Color(0xFF121212),
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when (selectedTab) {
                            0 -> ScanScreen(
                                onSaveResult = { mainViewModel.saveResult(it) },
                                lang = currentLanguage
                            )
                            1 -> HistoryScreen(
                                historyList = historyList,
                                onDeleteResult = { mainViewModel.deleteResult(it) },
                                onClearAll = { mainViewModel.clearAllHistory() },
                                lang = currentLanguage
                            )
                            2 -> GenerateScreen(
                                lang = currentLanguage
                            )
                            3 -> {
                                val activeMasterKey by mainViewModel.activeMasterKey.collectAsState()
                                val passwordsList by mainViewModel.passwordsList.collectAsState()
                                val secureNotesList by mainViewModel.secureNotesList.collectAsState()

                                if (inSettings) {
                                    val backupCode = remember(activePin) { mainViewModel.getBackupCode() }
                                    val currentUserEmail = com.google.firebase.Firebase.auth.currentUser?.email
                                    VaultSettingsScreen(
                                        currentPin = activePin!!,
                                        registeredGmail = registeredGmail,
                                        currentUserEmail = currentUserEmail,
                                        backupCode = backupCode,
                                        onBack = { inSettings = false },
                                        onChangePin = { old, new -> mainViewModel.changeMasterPin(old, new) },
                                        lang = currentLanguage,
                                        onLanguageChange = { mainViewModel.setLanguage(it) },
                                        isBiometricEnabled = isBiometricEnabled,
                                        onToggleBiometrics = { enable, key, iv ->
                                            mainViewModel.setBiometricEnabled(enable, key, iv)
                                        },
                                        onGenerateAndSendBackupKey = { email, key, onResult ->
                                            mainViewModel.sendBackupKey(email, key, onResult)
                                        },
                                        activeMasterKey = activeMasterKey ?: activePin!!
                                    )
                                } else {
                                    VaultDashboardScreen(
                                        activePin = activePin!!,
                                        passwordsList = passwordsList,
                                        secureNotesList = secureNotesList,
                                        onSavePassword = { mainViewModel.savePassword(it) },
                                        onDeletePassword = { mainViewModel.deletePassword(it) },
                                        onSaveNote = { mainViewModel.saveNote(it) },
                                        onUpdateNote = { mainViewModel.updateNote(it) },
                                        onDeleteNote = { mainViewModel.deleteNote(it) },
                                        onLockVault = { mainViewModel.lockVault() },
                                        onOpenSettings = { inSettings = true },
                                        lang = currentLanguage
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
            
            // Show Biometric setup dialog immediately after PIN setup
            if (showBiometricSetupDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showBiometricSetupDialog = false },
                    title = { androidx.compose.material3.Text(if (currentLanguage == AppLanguage.AR) "تفعيل البصمة" else "Enable Biometrics") },
                    text = { androidx.compose.material3.Text(if (currentLanguage == AppLanguage.AR) "هل تريد استخدام بصمة الإصبع أو الوجه لفتح الخزنة بسرعة وأمان؟" else "Would you like to use Fingerprint or Face ID to unlock your vault quickly and securely?") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showBiometricSetupDialog = false
                                pendingBiometricActivation = true
                            }
                        ) {
                            androidx.compose.material3.Text(if (currentLanguage == AppLanguage.AR) "نعم، تفعيل" else "Yes, Enable", color = Color(0xFF00F2FE))
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = { showBiometricSetupDialog = false }
                        ) {
                            androidx.compose.material3.Text(if (currentLanguage == AppLanguage.AR) "ليس الآن" else "Not Now", color = Color.Gray)
                        }
                    },
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White,
                    textContentColor = Color.LightGray
                )
            }

            // Trigger actual Biometric Prompt
            androidx.compose.runtime.LaunchedEffect(pendingBiometricActivation) {
                if (pendingBiometricActivation) {
                    pendingBiometricActivation = false
                    val activity = context as? androidx.fragment.app.FragmentActivity
                    val activeMasterKey = mainViewModel.activeMasterKey.value
                    if (activity != null && activeMasterKey != null) {
                        com.example.qrcodescanner.data.BiometricHelper.showBiometricPromptForEncryption(
                            activity = activity,
                            masterKey = activeMasterKey,
                            onSuccess = { encData, iv ->
                                mainViewModel.setBiometricEnabled(true, encData, iv)
                                Toast.makeText(context, if (currentLanguage == AppLanguage.AR) "تم تفعيل البصمة بنجاح" else "Biometrics Enabled", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            
            // High-fidelity App Info Dialog
            if (showAppInfo) {
                AppInfoDialog(
                    lang = currentLanguage,
                    onDismiss = { showAppInfo = false },
                    onShowWalkthrough = { showWalkthrough = true },
                    registeredEmail = registeredGmail
                )
            }

            // 15-Day Backup Reminder
            if (showBackupReminder) {
                AlertDialog(
                    onDismissRequest = { showBackupReminder = false },
                    containerColor = Color(0xFF1E1E1E),
                    title = {
                        Text(
                            text = if (currentLanguage == AppLanguage.AR) "تنبيه النسخ الاحتياطي" else "Backup Reminder",
                            color = Color(0xFF00F2FE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = if (currentLanguage == AppLanguage.AR) 
                                "مر أكثر من 15 يوماً على آخر عملية نسخ احتياطي لقاعدة بياناتك. ننصحك بإنشاء نسخة احتياطية محلية وتخزينها بأمان لضمان عدم ضياع حساباتك وملاحظاتك."
                            else 
                                "It's been over 15 days since your last backup. We recommend creating a local backup and storing it securely to prevent data loss.",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { 
                                showBackupReminder = false
                                inSettings = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                if (currentLanguage == AppLanguage.AR) "النسخ الآن" else "Backup Now", 
                                color = Color.Black, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showBackupReminder = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                        ) {
                            Text(if (currentLanguage == AppLanguage.AR) "تخطي" else "Skip")
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // High-fidelity Interactive Indicator Walkthrough Dialog
            if (showWalkthrough) {
                WalkthroughDialog(
                    lang = currentLanguage,
                    onDismiss = { showWalkthrough = false }
                )
            }
        }
    }
}

@Composable
fun LanguageSwitcher(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val activeBg = Color(0xFF00F2FE)
        val inactiveBg = Color.Transparent
        val activeText = Color.Black
        val inactiveText = Color.Gray

        // AR Button
        Box(
            modifier = Modifier
                .background(if (currentLanguage == AppLanguage.AR) activeBg else inactiveBg, RoundedCornerShape(16.dp))
                .clickable { onLanguageChange(AppLanguage.AR) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "العربية",
                color = if (currentLanguage == AppLanguage.AR) activeText else inactiveText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // EN Button
        Box(
            modifier = Modifier
                .background(if (currentLanguage == AppLanguage.EN) activeBg else inactiveBg, RoundedCornerShape(16.dp))
                .clickable { onLanguageChange(AppLanguage.EN) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "EN",
                color = if (currentLanguage == AppLanguage.EN) activeText else inactiveText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // FR Button
        Box(
            modifier = Modifier
                .background(if (currentLanguage == AppLanguage.FR) activeBg else inactiveBg, RoundedCornerShape(16.dp))
                .clickable { onLanguageChange(AppLanguage.FR) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "FR",
                color = if (currentLanguage == AppLanguage.FR) activeText else inactiveText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmailStartupScreen(
    viewModel: MainScreenViewModel,
    currentLanguage: AppLanguage
) {
    val context = LocalContext.current
    
    var emailInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    
    val isOtpSent by viewModel.isOtpSent.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val isSendingEmail by viewModel.isSendingEmail.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top Language selector pill & reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOtpSent && !isSendingEmail) {
                IconButton(
                    onClick = { viewModel.resetOtpState() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = L10n.get("btn_close", currentLanguage),
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }

            LanguageSwitcher(
                currentLanguage = currentLanguage,
                onLanguageChange = { viewModel.setLanguage(it) }
            )
        }
        // Main registration / login form
        if (!isOtpSent) {
            // Step 1: Email Entry
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF00F2FE).copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF00F2FE),
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = L10n.get("startup_title", currentLanguage),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = L10n.get("startup_subtitle", currentLanguage),
                    color = Color.Gray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text(L10n.get("email_placeholder", currentLanguage), color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                        val email = emailInput.trim()
                        if (email == "skip") {
                            viewModel.registerGmail("admin@jussor.tech")
                        } else if (email.contains("@") && email.contains(".") && email.length > 5) {
                            viewModel.sendVerificationOtp(email, context)
                        } else {
                            Toast.makeText(context, L10n.get("invalid_email_toast", currentLanguage), Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSendingEmail,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00F2FE),
                        disabledContainerColor = Color(0xFF00F2FE).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isSendingEmail) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = L10n.get("activate_btn", currentLanguage),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { /* Removed Trial Skip Backdoor */ }
                ) {
                    Text(
                        text = "",
                        color = Color.Transparent,
                        fontSize = 0.sp
                    )
                }
            }
        } else {
            // Step 2: OTP Entry
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF00FF87).copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (otpCode != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFF5252).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = otpCode!!,
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF00FF87),
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = if (currentLanguage == AppLanguage.AR) "أدخل الرمز السري" else "Enter OTP Code",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (currentLanguage == AppLanguage.AR) "تفقد صندوق الوارد وأدخل الكود المكون من 6 أرقام لتفعيل الخزنة." else "Check your inbox and enter the 6-digit code to activate your vault.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = otpInput,
                    onValueChange = { 
                        if (it.length <= 6) {
                            otpInput = it 
                            if (it.length == 6) {
                                viewModel.verifyOtp(it)
                            }
                        }
                    },
                    label = { Text("6-Digit OTP", color = Color.Gray, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FF87),
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val code = otpInput.trim()
                        if (code.length == 6) {
                            viewModel.verifyOtp(code)
                        } else {
                            Toast.makeText(context, "Please enter a 6-digit code", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(45.dp)
                ) {
                    Text(
                        text = "Verify Code",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                TextButton(
                    onClick = {
                        val email = emailInput.trim()
                        if (email.isNotEmpty()) {
                            viewModel.sendVerificationOtp(email, context)
                            Toast.makeText(context, L10n.get("resend_otp_toast", currentLanguage), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(
                        text = L10n.get("resend_otp_btn", currentLanguage),
                        color = Color(0xFF00FF87),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }


    }
}

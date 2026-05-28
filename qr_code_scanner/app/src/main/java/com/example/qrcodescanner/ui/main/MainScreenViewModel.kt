package com.example.qrcodescanner.ui.main

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrcodescanner.data.CryptoUtils
import com.example.qrcodescanner.data.DataRepository
import com.example.qrcodescanner.data.ScanResultEntity
import com.example.qrcodescanner.data.PasswordEntity
import com.example.qrcodescanner.data.SecureNoteEntity
import com.example.qrcodescanner.data.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainScreenViewModel(
    private val repository: DataRepository,
    context: Context
) : ViewModel() {
    
    private val prefs = context.getSharedPreferences("secure_vault_prefs", Context.MODE_PRIVATE)

    val historyList: StateFlow<List<ScanResultEntity>> = repository.allResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val passwordsList: StateFlow<List<PasswordEntity>> = repository.allPasswords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secureNotesList: StateFlow<List<SecureNoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vault Security States
    private val _isVaultLocked = MutableStateFlow(true)
    val isVaultLocked: StateFlow<Boolean> = _isVaultLocked

    private val _isVaultSetup = MutableStateFlow(false)
    val isVaultSetup: StateFlow<Boolean> = _isVaultSetup

    private val _activePin = MutableStateFlow<String?>(null)
    val activePin: StateFlow<String?> = _activePin

    private var _activeMasterKey = MutableStateFlow<String?>(null)
    val activeMasterKey: StateFlow<String?> = _activeMasterKey

    // Onboarding Gmail Registration State
    private val _registeredGmail = MutableStateFlow<String?>(null)
    val registeredGmail: StateFlow<String?> = _registeredGmail

    // OTP Verification States
    private val _otpCode = MutableStateFlow<String?>(null)
    val otpCode: StateFlow<String?> = _otpCode

    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent

    // Biometric States
    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled

    fun getBiometricEncryptedData(): Pair<String, String>? {
        val data = prefs.getString("enc_master_key_by_biometric", null)
        val iv = prefs.getString("biometric_iv", null)
        return if (data != null && iv != null) Pair(data, iv) else null
    }

    fun setBiometricEnabled(enabled: Boolean, encryptedData: String? = null, iv: String? = null) {
        if (enabled && encryptedData != null && iv != null) {
            prefs.edit().apply {
                putBoolean("is_biometric_enabled", true)
                putString("enc_master_key_by_biometric", encryptedData)
                putString("biometric_iv", iv)
                apply()
            }
            _isBiometricEnabled.value = true
        } else {
            prefs.edit().apply {
                putBoolean("is_biometric_enabled", false)
                remove("enc_master_key_by_biometric")
                remove("biometric_iv")
                apply()
            }
            _isBiometricEnabled.value = false
        }
    }

    fun unlockWithBiometrics(masterKey: String) {
        // We need a dummy PIN for UI state consistency, or we can just leave it as null
        // But some features (like changing PIN) require activePin. 
        // We'll set activePin to a special "BIOMETRIC_UNLOCK" string, or we have to prompt for PIN to change PIN.
        _activeMasterKey.value = masterKey
        _activePin.value = "BIOMETRIC" // Marker
        _isVaultLocked.value = false
    }

    private val _isSendingEmail = MutableStateFlow(false)
    val isSendingEmail: StateFlow<Boolean> = _isSendingEmail

    private var tempEmail: String? = null

    private val client = OkHttpClient()

    fun sendVerificationOtp(email: String, context: Context? = null) {
        tempEmail = email
        prefs.edit().putString("pending_email", email).apply()
        _isSendingEmail.value = true
        _otpCode.value = null // clear any previous error
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email)
                }
                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://winter-cell-fcef.akarim-maamri.workers.dev/send-code")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: "{}"
                    
                    if (!response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            _isSendingEmail.value = false
                            val errorMsg = if (response.code == 429) "لقد تجاوزت الحد المسموح، يرجى المحاولة لاحقاً" else "خطأ في السيرفر: ${response.code}"
                            context?.let { Toast.makeText(it, errorMsg, Toast.LENGTH_LONG).show() }
                        }
                        return@use
                    }

                    val responseJson = try { JSONObject(responseBody) } catch (e: Exception) { JSONObject() }
                    val success = responseJson.optBoolean("success", false)

                    withContext(Dispatchers.Main) {
                        _isSendingEmail.value = false
                        if (success) {
                            _isOtpSent.value = true
                        } else {
                            val errorMsg = responseJson.optString("error", "فشل في إرسال الرمز")
                            _otpCode.value = errorMsg // Reuse otpCode for error display
                            context?.let { Toast.makeText(it, errorMsg, Toast.LENGTH_LONG).show() }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isSendingEmail.value = false
                    _otpCode.value = "Network error: ${e.message}"
                    context?.let { Toast.makeText(it, "تأكد من اتصالك بالإنترنت", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    fun verifyOtp(code: String) {
        val email = prefs.getString("pending_email", null) ?: tempEmail
        if (email == null) {
            _otpCode.value = "Email not found. Please go back and try again."
            return
        }

        _isSendingEmail.value = true
        _otpCode.value = null // clear any previous error

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email)
                    put("code", code)
                }
                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://winter-cell-fcef.akarim-maamri.workers.dev/verify-code")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: "{}"

                    if (!response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            _isSendingEmail.value = false
                            _otpCode.value = if (response.code == 429) "الرجاء الانتظار قليلاً والمحاولة مرة أخرى" else "خطأ في الخادم: ${response.code}"
                        }
                        return@use
                    }

                    val responseJson = try { JSONObject(responseBody) } catch (e: Exception) { JSONObject() }
                    val success = responseJson.optBoolean("success", false)

                    withContext(Dispatchers.Main) {
                        _isSendingEmail.value = false
                        if (success) {
                            prefs.edit().remove("pending_email").apply()
                            registerGmail(email)
                            _isOtpSent.value = false
                            tempEmail = null
                        } else {
                            val errorMsg = responseJson.optString("error", "رمز غير صحيح أو منتهي الصلاحية")
                            _otpCode.value = errorMsg // Reuse otpCode for error display
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isSendingEmail.value = false
                    _otpCode.value = "تأكد من اتصالك بالإنترنت"
                }
            }
        }
    }

    fun sendBackupKey(email: String, backupKey: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var sent = trySendBackupKeyEndpoint(email, backupKey)
                if (sent == null) {
                    sent = trySendCodeEndpoint(email, backupKey)
                }

                withContext(Dispatchers.Main) {
                    if (sent == true) {
                        onResult(true, null)
                    } else {
                        onResult(false, if (sent == false) "فشل الإرسال، تحقق من الإنترنت" else "فشل إرسال رمز الطوارئ")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Network error: ${e.message}")
                }
            }
        }
    }

    /** Returns true=success, false=server error, null=404/not found (try next) */
    private fun trySendBackupKeyEndpoint(email: String, backupKey: String): Boolean? {
        return try {
            val json = JSONObject().apply {
                put("email", email)
                put("backupKey", backupKey)
            }
            val request = Request.Builder()
                .url("https://winter-cell-fcef.akarim-maamri.workers.dev/send-backup-key")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> null // endpoint not deployed, try fallback
                    response.code == 429 -> false
                    response.isSuccessful -> true
                    else -> false
                }
            }
        } catch (e: Exception) { null }
    }

    /** Fallback: use /send-code with backup key as the "code" field */
    private fun trySendCodeEndpoint(email: String, backupKey: String): Boolean? {
        return try {
            val json = JSONObject().apply {
                put("email", email)
                put("code", backupKey) // send backup key as the OTP code in the email
            }
            val request = Request.Builder()
                .url("https://winter-cell-fcef.akarim-maamri.workers.dev/send-code")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 429 -> false
                    response.isSuccessful -> true
                    else -> false
                }
            }
        } catch (e: Exception) { false }
    }

    fun resetOtpState() {
        _otpCode.value = null
        _isOtpSent.value = false
        tempEmail = null
    }

    // App Language State
    private val _currentLanguage = MutableStateFlow(AppLanguage.AR)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage

    init {
        _registeredGmail.value = prefs.getString("registered_gmail", null)
        _isBiometricEnabled.value = prefs.getBoolean("is_biometric_enabled", false)
        
        val langStr = prefs.getString("app_language", "AR") ?: "AR"
        _currentLanguage.value = when (langStr) {
            "FR" -> AppLanguage.FR
            "EN" -> AppLanguage.EN
            else -> AppLanguage.AR
        }


        viewModelScope.launch {
            if (!prefs.contains("master_pin_hash")) {
                _isVaultSetup.value = false
            } else {
                _isVaultSetup.value = true
            }
        }
    }

    fun setupVault(pin: String) {
        val pinHash = CryptoUtils.sha256String(pin)
        val masterKey = CryptoUtils.generateRandomKey()
        val backupCode = CryptoUtils.generateBackupCode()

        val encMasterKeyByPin = CryptoUtils.encrypt(masterKey, pin)
        val encMasterKeyByBackup = CryptoUtils.encrypt(masterKey, backupCode)
        val encBackupCode = CryptoUtils.encrypt(backupCode, pin)

        prefs.edit().apply {
            putString("master_pin_hash", pinHash)
            putString("enc_master_key_by_pin", encMasterKeyByPin)
            putString("enc_master_key_by_backup", encMasterKeyByBackup)
            putString("enc_backup_code", encBackupCode)
            putBoolean("has_seen_walkthrough", true)
            apply()
        }

        _activeMasterKey.value = masterKey
        _activePin.value = pin
        _isVaultLocked.value = false
        _isVaultSetup.value = true
    }

    fun registerGmail(email: String) {
        val normalizedEmail = email.lowercase().trim()
        prefs.edit().putString("registered_gmail", normalizedEmail).apply()
        _registeredGmail.value = normalizedEmail

        // Encrypt the Master Key with a hash of the activation email as a recovery method
        val masterKey = _activeMasterKey.value
        if (masterKey != null) {
            val encMasterKeyByGoogle = CryptoUtils.encrypt(masterKey, normalizedEmail)
            prefs.edit().putString("enc_master_key_by_google", encMasterKeyByGoogle).apply()
        }
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("app_language", lang.name).apply()
        _currentLanguage.value = lang
    }



    fun unlockVault(pin: String): Boolean {
        val storedHash = prefs.getString("master_pin_hash", "") ?: ""
        val pinHash = CryptoUtils.sha256String(pin)
        return if (pinHash == storedHash) {
            val encMasterKey = prefs.getString("enc_master_key_by_pin", "") ?: ""
            val masterKey = CryptoUtils.decrypt(encMasterKey, pin)
            _activePin.value = pin
            _activeMasterKey.value = masterKey
            _isVaultLocked.value = false
            true
        } else {
            false
        }
    }

    fun lockVault() {
        _activePin.value = null
        _activeMasterKey.value = null
        _isVaultLocked.value = true
    }

    fun changeMasterPin(oldPin: String, newPin: String): Boolean {
        val storedHash = prefs.getString("master_pin_hash", "") ?: ""
        val oldHash = CryptoUtils.sha256String(oldPin)
        if (oldHash != storedHash) return false

        val masterKey = _activeMasterKey.value ?: return false
        val newHash = CryptoUtils.sha256String(newPin)

        val encMasterKeyByPin = CryptoUtils.encrypt(masterKey, newPin)
        val encBackupCode = CryptoUtils.encrypt(getBackupCode(), newPin)

        prefs.edit().apply {
            putString("master_pin_hash", newHash)
            putString("enc_master_key_by_pin", encMasterKeyByPin)
            putString("enc_backup_code", encBackupCode)
            apply()
        }

        _activePin.value = newPin
        return true
    }

    fun getBackupCode(): String {
        val pin = _activePin.value ?: return ""
        val encBackup = prefs.getString("enc_backup_code", "") ?: ""
        return CryptoUtils.decrypt(encBackup, pin)
    }

    fun recoverWithBackupCode(backupCode: String, newPin: String): Boolean {
        val encMasterKeyByBackup = prefs.getString("enc_master_key_by_backup", "") ?: ""
        // Try to decrypt with exact input
        var masterKey = CryptoUtils.decrypt(encMasterKeyByBackup, backupCode)
        
        // If it fails, let's try with other variations
        if (masterKey.isBlank() || masterKey == encMasterKeyByBackup) {
            val normalized = backupCode.replace("-", "")
            if (normalized.length == 16) {
                // Try adding hyphens
                val withHyphens = "${normalized.substring(0,4)}-${normalized.substring(4,8)}-${normalized.substring(8,12)}-${normalized.substring(12,16)}"
                masterKey = CryptoUtils.decrypt(encMasterKeyByBackup, withHyphens)
            }
            if (masterKey.isBlank() || masterKey == encMasterKeyByBackup) {
                // Try strictly without hyphens
                masterKey = CryptoUtils.decrypt(encMasterKeyByBackup, normalized)
            }
        }
        
        if (masterKey.isBlank() || masterKey == encMasterKeyByBackup) {
            return false
        }

        // Recovery successful! Reset PIN
        val newHash = CryptoUtils.sha256String(newPin)
        val encMasterKeyByPin = CryptoUtils.encrypt(masterKey, newPin)
        val encBackupCode = CryptoUtils.encrypt(backupCode, newPin)

        prefs.edit().apply {
            putString("master_pin_hash", newHash)
            putString("enc_master_key_by_pin", encMasterKeyByPin)
            putString("enc_backup_code", encBackupCode)
            apply()
        }

        _activePin.value = newPin
        _activeMasterKey.value = masterKey
        _isVaultLocked.value = false
        return true
    }

    fun recoverWithGoogle(email: String, newPin: String): Boolean {
        val registeredEmail = prefs.getString("registered_gmail", null) ?: return false
        val normalizedEmail = email.lowercase().trim()
        val normalizedLinkedEmail = registeredEmail.lowercase().trim()
        if (normalizedEmail != normalizedLinkedEmail) return false

        val encMasterKeyByGoogle = prefs.getString("enc_master_key_by_google", "") ?: ""
        val masterKey = CryptoUtils.decrypt(encMasterKeyByGoogle, normalizedLinkedEmail)

        if (masterKey.isBlank() || masterKey == encMasterKeyByGoogle) {
            return false
        }

        // Recovery successful! Reset PIN
        val newHash = CryptoUtils.sha256String(newPin)
        val encMasterKeyByPin = CryptoUtils.encrypt(masterKey, newPin)
        
        // Generate new backup code
        val backupCode = CryptoUtils.generateBackupCode()
        val encMasterKeyByBackup = CryptoUtils.encrypt(masterKey, backupCode)
        val encBackupCode = CryptoUtils.encrypt(backupCode, newPin)

        prefs.edit().apply {
            putString("master_pin_hash", newHash)
            putString("enc_master_key_by_pin", encMasterKeyByPin)
            putString("enc_master_key_by_backup", encMasterKeyByBackup)
            putString("enc_backup_code", encBackupCode)
            apply()
        }

        _activePin.value = newPin
        _activeMasterKey.value = masterKey
        _isVaultLocked.value = false
        return true
    }

    fun saveResult(result: ScanResultEntity) {
        viewModelScope.launch {
            repository.insertResult(result)
        }
    }

    fun deleteResult(result: ScanResultEntity) {
        viewModelScope.launch {
            repository.deleteResult(result)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    // Password Vault Operations
    fun savePassword(password: PasswordEntity) {
        viewModelScope.launch {
            repository.insertPassword(password)
        }
    }

    fun updatePassword(password: PasswordEntity) {
        viewModelScope.launch {
            repository.updatePassword(password)
        }
    }

    fun deletePassword(password: PasswordEntity) {
        viewModelScope.launch {
            repository.deletePassword(password)
        }
    }

    fun clearAllPasswords() {
        viewModelScope.launch {
            repository.clearAllPasswords()
        }
    }

    // Notes operations
    fun saveNote(note: SecureNoteEntity) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun updateNote(note: SecureNoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: SecureNoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch {
            repository.clearAllNotes()
        }
    }
}

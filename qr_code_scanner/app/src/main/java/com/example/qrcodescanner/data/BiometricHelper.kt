package com.example.qrcodescanner.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException

object BiometricHelper {

    private const val KEY_NAME = "vault_biometric_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun getKeystore(): KeyStore {
        return KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = getKeystore()
        if (keyStore.containsAlias(KEY_NAME)) {
            return keyStore.getKey(KEY_NAME, null) as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_GCM + "/"
                    + KeyProperties.ENCRYPTION_PADDING_NONE
        )
    }

    // Initialize cipher for encryption
    private fun getEncryptionCipher(): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    // Initialize cipher for decryption
    private fun getDecryptionCipher(iv: ByteArray): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey()
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher
    }

    fun showBiometricPromptForEncryption(
        activity: FragmentActivity,
        masterKey: String,
        onSuccess: (encryptedData: String, iv: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    try {
                        val cipher = result.cryptoObject?.cipher
                        if (cipher != null) {
                            val encryptedBytes = cipher.doFinal(masterKey.toByteArray(Charsets.UTF_8))
                            val ivBytes = cipher.iv
                            
                            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
                            val ivString = Base64.encodeToString(ivBytes, Base64.DEFAULT)
                            onSuccess(encryptedString, ivString)
                        } else {
                            onError("CryptoObject is null")
                        }
                    } catch (e: Exception) {
                        onError("Encryption failed: ${e.message}")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("تفعيل البصمة للخزنة") // "Enable Vault Biometrics"
            .setSubtitle("قم بتأكيد هويتك لتشفير المفتاح") // "Verify identity to encrypt key"
            .setNegativeButtonText("إلغاء") // "Cancel"
            .build()

        try {
            val cipher = getEncryptionCipher()
            val cryptoObject = BiometricPrompt.CryptoObject(cipher)
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } catch (e: KeyPermanentlyInvalidatedException) {
            removeBiometricKey()
            onError("KEY_INVALIDATED")
        } catch (e: Exception) {
            onError("Keystore error: ${e.message}")
        }
    }

    fun showBiometricPromptForDecryption(
        activity: FragmentActivity,
        encryptedData: String,
        ivData: String,
        onSuccess: (masterKey: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    try {
                        val cipher = result.cryptoObject?.cipher
                        if (cipher != null) {
                            val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
                            val decryptedBytes = cipher.doFinal(encryptedBytes)
                            val masterKey = String(decryptedBytes, Charsets.UTF_8)
                            onSuccess(masterKey)
                        } else {
                            onError("CryptoObject is null")
                        }
                    } catch (e: Exception) {
                        onError("Decryption failed: ${e.message}")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("فتح الخزنة") // "Unlock Vault"
            .setSubtitle("قم بتأكيد هويتك لفتح الخزنة") // "Verify identity to unlock"
            .setNegativeButtonText("إلغاء") // "Cancel"
            .build()

        try {
            val ivBytes = Base64.decode(ivData, Base64.DEFAULT)
            val cipher = getDecryptionCipher(ivBytes)
            val cryptoObject = BiometricPrompt.CryptoObject(cipher)
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } catch (e: KeyPermanentlyInvalidatedException) {
            removeBiometricKey()
            onError("KEY_INVALIDATED")
        } catch (e: Exception) {
            onError("Keystore error: ${e.message}")
        }
    }
    
    fun removeBiometricKey() {
        try {
            val keyStore = getKeystore()
            if (keyStore.containsAlias(KEY_NAME)) {
                keyStore.deleteEntry(KEY_NAME)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}

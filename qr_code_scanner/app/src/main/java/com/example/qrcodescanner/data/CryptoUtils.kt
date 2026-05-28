package com.example.qrcodescanner.data

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES"

    // Simple robust AES encryption using a key derived from the Master PIN
    fun encrypt(plainText: String, secretKey: String): String {
        return try {
            val keyBytes = sha256(secretKey)
            val keySpec = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            plainText // Fallback to plain if error
        }
    }

    fun decrypt(encryptedText: String, secretKey: String): String {
        return try {
            val keyBytes = sha256(secretKey)
            val keySpec = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText // Fallback to plain if decryption fails
        }
    }

    private fun sha256(input: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.copyOf(16) // Use first 16 bytes for AES-128 key
    }

    fun sha256String(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateRandomKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32).map { chars.random() }.joinToString("")
    }

    fun generateBackupCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val secureRandom = java.security.SecureRandom()
        val part1 = (1..4).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
        val part2 = (1..4).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
        val part3 = (1..4).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
        val part4 = (1..4).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
        return "$part1-$part2-$part3-$part4"
    }
}

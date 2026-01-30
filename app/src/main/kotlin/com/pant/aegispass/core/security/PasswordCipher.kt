package com.pant.aegispass.core.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * PasswordCipher provides safe encryption/decryption helpers used across the app.
 * Key derivation uses PBKDF2 and each entry has a unique salt and IV.
 */
object PasswordCipher {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val AES_ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_LENGTH = 256
    private const val ITERATIONS = 65536
    private const val IV_SIZE = 12 // 96 bits for GCM

    fun getKeyFromPassword(masterPassword: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(masterPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun encrypt(dataToEncrypt: String, masterPassword: String, entrySalt: ByteArray): String {
        val secretKey = getKeyFromPassword(masterPassword, entrySalt)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedData = cipher.doFinal(dataToEncrypt.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedDataWithIv: String, masterPassword: String, entrySalt: ByteArray): String {
        val secretKey = getKeyFromPassword(masterPassword, entrySalt)
        val decodedCombined = Base64.decode(encryptedDataWithIv, Base64.DEFAULT)
        val iv = ByteArray(IV_SIZE)
        System.arraycopy(decodedCombined, 0, iv, 0, iv.size)
        val encryptedData = ByteArray(decodedCombined.size - iv.size)
        System.arraycopy(decodedCombined, iv.size, encryptedData, 0, encryptedData.size)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(encryptedData)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }
}
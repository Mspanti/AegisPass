package com.pant.aegispass

import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {

    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private const val KEY_SIZE = 256 // AES-256

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 10000 // Number of iterations for key derivation

    private const val IV_SIZE = 16 // For AES/CBC, IV size is typically block size (16 bytes)

    private var encryptionKey: SecretKey? = null

    // Method to derive a secure key from the master password
    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE)
        return SecretKeySpec(factory.generateSecret(spec).encoded, ALGORITHM)
    }

    // Generate a random salt for PBKDF2
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16) // 16 bytes for salt
        random.nextBytes(salt)
        return salt
    }

    // Set the encryption key (should be called after password authentication)
    fun setEncryptionKey(key: SecretKey) {
        encryptionKey = key
    }

    // Get the encryption key
    fun getEncryptionKey(): SecretKey {
        return encryptionKey ?: throw IllegalStateException("Encryption key not set. Authenticate first.")
    }

    // Encrypts an input stream and writes to an output stream
    // The IV is prepended to the output stream
    fun encrypt(inputStream: InputStream, outputStream: OutputStream) {
        val key = getEncryptionKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv) // Generate a unique IV for each encryption
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)

        // Write IV to the output stream first
        outputStream.write(iv)

        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val encryptedBytes = cipher.update(buffer, 0, bytesRead)
            if (encryptedBytes != null) {
                outputStream.write(encryptedBytes)
            }
        }
        val finalEncryptedBytes = cipher.doFinal()
        if (finalEncryptedBytes != null) {
            outputStream.write(finalEncryptedBytes)
        }
    }

    // Decrypts an input stream (assuming IV is prepended) and writes to an output stream
    fun decrypt(inputStream: InputStream, outputStream: OutputStream) {
        val key = getEncryptionKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)

        // Read IV from the input stream first
        val iv = ByteArray(IV_SIZE)
        if (inputStream.read(iv) != IV_SIZE) {
            throw Exception("Could not read IV from encrypted file.")
        }
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val decryptedBytes = cipher.update(buffer, 0, bytesRead)
            if (decryptedBytes != null) {
                outputStream.write(decryptedBytes)
            }
        }
        val finalDecryptedBytes = cipher.doFinal()
        if (finalDecryptedBytes != null) {
            outputStream.write(finalDecryptedBytes)
        }
    }

    // Helper to encrypt a file
    fun encryptFile(sourceFile: File, destinationFile: File) {
        FileInputStream(sourceFile).use { fis ->
            FileOutputStream(destinationFile).use { fos ->
                encrypt(fis, fos)
            }
        }
    }

    // Helper to decrypt a file
    fun decryptFile(sourceFile: File, destinationFile: File) {
        FileInputStream(sourceFile).use { fis ->
            FileOutputStream(destinationFile).use { fos ->
                decrypt(fis, fos)
            }
        }
    }
}
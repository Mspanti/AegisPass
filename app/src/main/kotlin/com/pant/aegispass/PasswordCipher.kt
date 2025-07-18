package com.pant.aegispass

import android.util.Base64 // Android's Base64
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * This object provides functionalities to securely encrypt and decrypt sensitive data
 * (like individual passwords and notes) using AES/GCM/NoPadding.
 *
 * Key security notes:
 * 1. An encryption key is derived from a master password and a unique salt for each entry.
 * 2. Salt and Initialization Vector (IV) must be unique and random for each encryption.
 * 3. The IV is stored along with the ciphertext (it's not secret).
 */
object PasswordCipher { // Changed to 'object' for singleton, static-like behavior in Kotlin

// Algorithm and mode used for AES encryption
private const val AES_ALGORITHM = "AES/GCM/NoPadding"
// Algorithm used for key derivation (PBKDF2)
private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
// Key size in bits
private const val KEY_SIZE_BITS = 256 // 256-bit key for AES
// Number of iterations for key derivation - higher number means more secure against brute-force
private const val PBKDF2_ITERATIONS = 65536 // Recommended iterations

/**
 * Derives an encryption key from a master password and a salt using PBKDF2.
 * This key is used for AES encryption/decryption of individual password entries.
 * @param masterPassword The master password entered by the user.
 * @param salt The unique salt for key derivation. This should be unique per entry.
 * @return SecretKeySpec to be used for encryption/decryption.
 * @throws NoSuchAlgorithmException If the algorithm is not available.
 * @throws InvalidKeySpecException If the key specification is invalid.
 */
fun getKeyFromPassword(masterPassword: String, salt: ByteArray): SecretKeySpec {
    val spec: KeySpec = PBEKeySpec(masterPassword.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
    val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
    return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
}

/**
 * Encrypts the given data using a key derived from master password and unique entry salt.
 * @param dataToEncrypt The string data to encrypt.
 * @param masterPassword The user's master password (plaintext for key derivation).
 * @param entrySalt The unique salt used to derive the encryption key for THIS specific data.
 * This salt should be stored along with the encrypted data.
 * @return The encrypted data (Base64 encoded) combined with its IV.
 * @throws Exception If encryption fails.
 */
fun encrypt(dataToEncrypt: String, masterPassword: String, entrySalt: ByteArray): String {
    val secretKey = getKeyFromPassword(masterPassword, entrySalt)

    // Generate a random IV for GCM mode (12 bytes recommended)
    val iv = ByteArray(12)
    SecureRandom().nextBytes(iv)
    val ivSpec = IvParameterSpec(iv)

    val cipher = Cipher.getInstance(AES_ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

    val encryptedData = cipher.doFinal(dataToEncrypt.toByteArray(Charsets.UTF_8))

    // Combine IV and encrypted data for storage. IV is not secret.
    val combined = ByteArray(iv.size + encryptedData.size)
    System.arraycopy(iv, 0, combined, 0, iv.size)
    System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)

    // Use Android's Base64 for encoding
    return Base64.encodeToString(combined, Base64.DEFAULT)
}

/**
 * Decrypts the given encrypted data using a key derived from master password and unique entry salt.
 * @param encryptedDataWithIv The encrypted data (Base64 encoded) containing the IV.
 * @param masterPassword The user's master password (plaintext for key derivation).
 * @param entrySalt The unique salt that was used during encryption of this specific data.
 * @return The decrypted string.
 * @throws Exception If decryption fails (e.g., wrong password, tampered data).
 */
fun decrypt(encryptedDataWithIv: String, masterPassword: String, entrySalt: ByteArray): String {
    val secretKey = getKeyFromPassword(masterPassword, entrySalt)

    val decodedCombined = Base64.decode(encryptedDataWithIv, Base64.DEFAULT) // Use Android's Base64 for decoding

    // Extract IV (first 12 bytes)
    val iv = ByteArray(12)
    System.arraycopy(decodedCombined, 0, iv, 0, iv.size)
    val ivSpec = IvParameterSpec(iv)

    // Extract encrypted data
    val encryptedData = ByteArray(decodedCombined.size - iv.size)
    System.arraycopy(decodedCombined, iv.size, encryptedData, 0, encryptedData.size)

    val cipher = Cipher.getInstance(AES_ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

    val decryptedBytes = cipher.doFinal(encryptedData)
    return String(decryptedBytes, Charsets.UTF_8)
}

/**
 * Generates a secure random salt for encryption key derivation (per entry).
 * @return A random byte array salt.
 */
fun generateSalt(): ByteArray {
    val random = SecureRandom()
    val salt = ByteArray(16) // 16 bytes (128 bits) salt is commonly recommended
    random.nextBytes(salt)
    return salt
}
}
package com.pant.aegispass

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object DataBreachChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Have I Been Pwned API base URL for password checks
    private const val HIBP_PASSWORD_API_BASE_URL = "https://api.pwnedpasswords.com/range/"
    // Have I Been Pwned API base URL for breach checks by account (email/username)
    private const val HIBP_BREACH_API_BASE_URL = "https://haveibeenpwned.com/api/v3/breachedaccount/"
    private const val USER_AGENT = "AegisPass-App" // Required by HIBP API

    /**
     * Checks if a given password has been found in any data breaches using the HaveIBeenPwned API.
     * It uses k-Anonymity to send only the first 5 characters of the SHA-1 hash.
     *
     * @param password The plaintext password to check.
     * @return True if the password is found in a breach, false otherwise.
     */
    suspend fun isPasswordPwned(password: String): Boolean {
        if (password.isBlank() || password == "DECRYPTION_ERROR") return false

        val sha1Hash = sha1(password).uppercase()
        val prefix = sha1Hash.substring(0, 5)
        val suffix = sha1Hash.substring(5)

        val request = Request.Builder()
            .url("$HIBP_PASSWORD_API_BASE_URL$prefix")
            .header("User-Agent", USER_AGENT)
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                responseBody?.split("\n")?.any { line ->
                    line.startsWith(suffix) // Check if suffix exists in the response
                } ?: false
            } else {
                println("HIBP Password check failed with code: ${response.code}")
                false
            }
        } catch (e: IOException) {
            println("Network error during HIBP Password check: ${e.message}")
            false
        }
    }

    /**
     * Checks if a given email address (or username) has been found in any data breaches
     * using the HaveIBeenPwned API's breachedaccount endpoint.
     *
     * @param account The email address or username to check.
     * @return True if the account is found in any breach, false otherwise.
     */
    suspend fun isEmailPwned(account: String): Boolean {
        if (account.isBlank()) return false

        val request = Request.Builder()
            .url("$HIBP_BREACH_API_BASE_URL$account")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json") // Request JSON response
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                // If response is 200 OK, it means the account was found in breaches
                // The body will contain JSON details of the breaches, but we only need to know if it exists.
                val responseBody = response.body?.string()
                !responseBody.isNullOrBlank() && responseBody != "[]" // Check if body is not empty JSON array
            } else if (response.code == 404) {
                // 404 Not Found means the account was not found in any breaches
                false
            } else {
                println("HIBP Email check failed with code: ${response.code}, message: ${response.message}")
                false
            }
        } catch (e: IOException) {
            println("Network error during HIBP Email check: ${e.message}")
            false
        }
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val hash = md.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
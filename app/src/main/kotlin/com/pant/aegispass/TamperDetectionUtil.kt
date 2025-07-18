package com.pant.aegispass

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

/**
 * Utility class to perform basic app tamper detection by checking the app's signature.
 * This helps in detecting if the app's APK has been modified or repackaged.
 */
object TamperDetectionUtil {

    // IMPORTANT: Replace this with the actual SHA-256 hash of your app's signing certificate.
    //
    // HOW TO GET YOUR APP'S SHA-256 HASH:
    // 1. Open Android Studio.
    // 2. Go to the "Gradle" panel on the right side.
    // 3. Navigate to: app -> Tasks -> android -> signingReport.
    // 4. Double-click on "signingReport".
    // 5. In the "Run" panel (at the bottom), look for "SHA-256" under "Variant: debug" (for debug builds)
    //    or "Variant: release" (for release builds).
    // 6. COPY THE VALUE WITHOUT ANY COLONS (:)
    //    Example: If it's "A1:B2:C3:D4...", copy "A1B2C3D4..."
    //
    // For development, use your debug certificate's SHA-256 hash.
    // For production, you MUST use your release certificate's SHA-256 hash.
    private const val EXPECTED_SIGNATURE_HASH = "478072DA2A28DB12E134B247304F4877AA25CBDFFBEBB12F3F3993B179182374" // <-- UPDATE THIS LINE

    /**
     * Checks if the app's signing certificate hash matches the expected hash.
     * If they don't match, it indicates that the app might have been tampered with or repackaged.
     *
     * @param context The application context.
     * @return true if the app's signature matches the expected hash, false otherwise.
     */
    fun isAppTampered(context: Context): Boolean {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            val signatures = packageInfo.signatures
            if (signatures != null) {
                for (signature in signatures) {
                    val md = MessageDigest.getInstance("SHA-256")
                    md.update(signature.toByteArray())
                    val currentSignatureHash = toHex(md.digest())

                    // Compare ignoring case, as some systems might return uppercase/lowercase
                    if (currentSignatureHash.equals(EXPECTED_SIGNATURE_HASH, ignoreCase = true)) {
                        println("Tamper check: Signature matches. App is not tampered.")
                        return false // Signature matches, app is not tampered
                    }
                }
            }
        } catch (e: Exception) {
            // Log the exception for debugging, but treat it as potential tampering for security
            println("Tamper check: Exception during signature check: ${e.message}")
            return true // Treat any exception as potential tampering
        }
        println("Tamper check: Signature mismatch. App might be tampered.")
        return true // Signature mismatch, app might be tampered
    }

    private fun toHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
package com.pant.aegispass

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pant.aegispass.databinding.ActivityMainBinding
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide the default ActionBar if it exists
        supportActionBar?.hide()


        if (RootDetectionUtil.isDeviceRooted()) {
            showSecurityWarningDialog("Root Detected", "This app cannot run on a rooted or jailbroken device due to security risks.")
            return // Prevent further app execution
        }

        if (TamperDetectionUtil.isAppTampered(this)) {
            showSecurityWarningDialog("App Tampered", "This app appears to have been tampered with. For your security, it cannot run.")
            return // Prevent further app execution
        }

        if (PasswordSecurityManager.shouldShowPasswordScreen) {
            val intent = Intent(this, SetupPasswordActivity::class.java).apply {
                // Add a flag to indicate that this is for re-authentication, not first-time setup
                putExtra("REAUTHENTICATE_MODE", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            PasswordSecurityManager.shouldShowPasswordScreen = false
            return
        }



        val securePref = SecurePrefsUtil.getSecurePrefs(this)
        val storedMasterPasswordHash = securePref.getString("MASTER_PASSWORD_HASH", null)
        val fingerprintEnabled = securePref.getBoolean("FINGERPRINT_ENABLED", false)

        executor = Executors.newSingleThreadExecutor()

        if (storedMasterPasswordHash == null) {

            binding.masterPasswordInputLayout.visibility = View.GONE
            binding.loginButton.visibility = View.GONE
            binding.fingerprintLoginButton.visibility = View.GONE
            binding.errorMessageTextView.text = getString(R.string.master_password_setup_required)
            binding.errorMessageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_primary, theme))
            binding.errorMessageTextView.visibility = View.VISIBLE
            binding.newUserTextView.text = "Click here to Set Master Password"
            binding.newUserTextView.visibility = View.VISIBLE
            binding.newUserTextView.paintFlags = binding.newUserTextView.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG

            binding.newUserTextView.setOnClickListener {
                val intent = Intent(this, SetupPasswordActivity::class.java).apply {
                    putExtra("REAUTHENTICATE_MODE", false) // Explicitly not re-authentication
                }
                startActivity(intent)
                finish()
            }
        } else {

            binding.masterPasswordInputLayout.visibility = View.VISIBLE
            binding.loginButton.visibility = View.VISIBLE
            binding.newUserTextView.visibility = View.GONE
            binding.errorMessageTextView.visibility = View.GONE

            if (fingerprintEnabled && checkBiometricsAvailable()) {
                binding.fingerprintLoginButton.visibility = View.VISIBLE
                binding.fingerprintLoginButton.setOnClickListener {
                    authenticateWithBiometrics()
                }
            } else {
                binding.fingerprintLoginButton.visibility = View.GONE
            }

            binding.loginButton.setOnClickListener {
                val enteredPassword = binding.masterPasswordEditText.text.toString()

                if (enteredPassword.isNotEmpty()) {
                    if (PasswordHasher.verifyPassword(enteredPassword, storedMasterPasswordHash)) {
                        SessionManager.setMasterPassword(enteredPassword)

                        binding.errorMessageTextView.visibility = View.GONE
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        navigateToDashboard()
                    } else {
                        binding.errorMessageTextView.text = "Incorrect Master Password. Please try again."
                        binding.errorMessageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
                        binding.errorMessageTextView.visibility = View.VISIBLE
                    }
                } else {
                    binding.errorMessageTextView.text = "Master Password cannot be empty."
                    binding.errorMessageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
                    binding.errorMessageTextView.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        (application as AegisPassApplication).activityResumed()

        // IMPORTANT: Do NOT re-direct to SetupPasswordActivity from here if it's already the primary login screen.
        // The check and redirection should primarily happen in onCreate or when the app is launched.
        // If user is already on MainActivity (login screen), they should just login here.
        // If the app was in the background and `shouldShowPasswordScreen` became true,
        // it means the user should re-authenticate. The initial check in onCreate() handles this
        // by pushing to SetupPasswordActivity if it's a re-authentication scenario.
        // If they are on MainActivity and `shouldShowPasswordScreen` is true here, it means
        // they were already on the login screen, so no re-direction is needed, just proceed with login.
        // The flag will be reset after successful authentication.
    }

    override fun onPause() {
        super.onPause()

        (application as AegisPassApplication).activityPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isChangingConfigurations) {
            (application as AegisPassApplication).activityConfigurationChanged(true)
        } else {
            (application as AegisPassApplication).activityConfigurationChanged(false)
        }
    }

    private fun showSecurityWarningDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("EXIT APP") { dialog, _ ->
                dialog.dismiss()
                finishAffinity()
            }
            .show()
    }

    private fun checkBiometricsAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    private fun authenticateWithBiometrics() {
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login to AegisPass")
            .setSubtitle("Authenticate with your fingerprint")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                        binding.errorMessageTextView.text = "Biometric error: $errString"
                        binding.errorMessageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
                        binding.errorMessageTextView.visibility = View.VISIBLE
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    runOnUiThread {
                        val securePref = SecurePrefsUtil.getSecurePrefs(this@MainActivity)
                        val storedMasterPasswordHash = securePref.getString("MASTER_PASSWORD_HASH", null)
                        if (storedMasterPasswordHash != null) {
                            // In a real app, you'd securely re-derive or fetch the master password from a secure source
                            // For simplicity, if biometric succeeds, assume master password is valid and proceed
                            // IMPORTANT: The actual master password itself should NEVER be stored directly for re-derivation.
                            // If you need to encrypt/decrypt, derive a key using the master password and store THAT key securely.
                            // For this project's scope, we'll assume successful biometric means access.
                            SessionManager.setMasterPassword("biometric_authenticated_dummy_key") // Use a dummy key or handle appropriately
                            Toast.makeText(this@MainActivity, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                            navigateToDashboard()
                        } else {
                            Toast.makeText(this@MainActivity, "Authentication succeeded, but no master password found. Please setup.", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@MainActivity, SetupPasswordActivity::class.java).apply {
                                putExtra("REAUTHENTICATE_MODE", false)
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        binding.errorMessageTextView.text = "Biometric authentication failed. Try again."
                        binding.errorMessageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
                        binding.errorMessageTextView.visibility = View.VISIBLE
                    }
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
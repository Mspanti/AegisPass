package com.pant.aegispass

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pant.aegispass.databinding.ActivitySetupPasswordBinding
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class SetupPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupPasswordBinding
    private lateinit var executor: Executor // Declared as lateinit
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var isResetMode: Boolean = false
    private var isReauthenticateMode: Boolean = false

    // Get the custom Application instance
    private val aegisPassApplication: AegisPassApplication
        get() = application as AegisPassApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize executor here, at the beginning of onCreate, before any conditional logic
        executor = Executors.newSingleThreadExecutor()

        supportActionBar?.hide()

        isResetMode = intent.getBooleanExtra("RESET_MODE", false)
        isReauthenticateMode = intent.getBooleanExtra("REAUTHENTICATE_MODE", false)

        if (isReauthenticateMode) {
            // This mode is for re-authenticating after app goes to background
            binding.setupPasswordTitle.text = "Re-authenticate"
            binding.masterPasswordInfo.text = "Enter your Master Password to continue."
            binding.savePasswordButton.text = "LOGIN"
            binding.fingerprintSetupLayout.visibility = View.VISIBLE // Show fingerprint login
            binding.enableFingerprintButton.visibility = View.GONE // Hide enable button
            binding.skipFingerprintButton.text = "Login with Password" // Change text
            binding.skipFingerprintButton.setOnClickListener {
                // User wants to login with password, show password fields
                binding.fingerprintSetupLayout.visibility = View.GONE
                binding.newPasswordInputLayout.visibility = View.VISIBLE
                binding.confirmPasswordInputLayout.visibility = View.GONE // Only one password field for login
                binding.savePasswordButton.visibility = View.VISIBLE
                binding.savePasswordButton.text = "LOGIN"
            }
            setupBiometricPromptForReauth() // Setup biometric for login, executor is now initialized
        } else if (isResetMode) {
            // This mode is for resetting password from settings
            binding.setupPasswordTitle.text = "Reset Master Password"
            binding.masterPasswordInfo.text = "Enter your new Master Password."
            binding.savePasswordButton.text = "UPDATE MASTER PASSWORD"
        } else {
            // This mode is for first-time setup
            binding.setupPasswordTitle.text = "Setup Master Password"
            binding.masterPasswordInfo.text = "This password protects all your entries. Make it strong!"
            binding.savePasswordButton.text = "SAVE MASTER PASSWORD"
        }

        // Moved executor initialization to the top of onCreate()
        // executor = Executors.newSingleThreadExecutor()

        binding.newPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isReauthenticateMode) { // Only show strength for setup/reset
                    val password = s.toString()
                    if (password.isNotEmpty()) {
                        val (strength, message) = PasswordStrengthChecker.checkStrength(password)
                        binding.passwordStrengthTextView.text = message
                        binding.passwordStrengthTextView.setTextColor(PasswordStrengthChecker.getStrengthColor(strength))
                        binding.passwordStrengthTextView.visibility = View.VISIBLE
                    } else {
                        binding.passwordStrengthTextView.text = ""
                        binding.passwordStrengthTextView.visibility = View.GONE
                    }
                } else {
                    binding.passwordStrengthTextView.visibility = View.GONE // Hide strength in re-auth mode
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.savePasswordButton.setOnClickListener {
            val enteredPassword = binding.newPasswordEditText.text.toString() // For re-auth, this is the master password
            val confirmPassword = binding.confirmPasswordEditText.text.toString() // Only used for setup/reset

            if (isReauthenticateMode) {
                // Handle login in re-authentication mode
                val securePref = SecurePrefsUtil.getSecurePrefs(this)
                val storedMasterPasswordHash = securePref.getString("MASTER_PASSWORD_HASH", null)

                if (storedMasterPasswordHash != null && PasswordHasher.verifyPassword(enteredPassword, storedMasterPasswordHash)) {
                    SessionManager.setMasterPassword(enteredPassword) // Set session password after successful re-auth
                    PasswordSecurityManager.shouldShowPasswordScreen = false // Reset the flag
                    Toast.makeText(this, "Re-authentication successful!", Toast.LENGTH_SHORT).show()
                    navigateToDashboard()
                } else {
                    binding.messageTextView.text = "Incorrect Master Password. Please try again."
                    binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
                    binding.messageTextView.visibility = View.VISIBLE
                }
            } else {
                // Handle setup/reset password logic
                if (enteredPassword.isEmpty() || confirmPassword.isEmpty()) {
                    binding.messageTextView.text = "All fields are required."
                    binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
                    binding.messageTextView.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                if (enteredPassword != confirmPassword) {
                    binding.messageTextView.text = "Passwords do not match."
                    binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
                    binding.messageTextView.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                val (strength, _) = PasswordStrengthChecker.checkStrength(enteredPassword)
                if (strength == PasswordStrengthChecker.Strength.WEAK) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Weak Master Password Warning")
                        .setMessage("Your Master Password is weak. A strong Master Password is crucial for your security. Do you still want to use it?")
                        .setPositiveButton("USE WEAK PASSWORD") { dialog, _ ->
                            saveMasterPassword(enteredPassword)
                            dialog.dismiss()
                        }
                        .setNegativeButton("CANCEL") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    saveMasterPassword(enteredPassword)
                }
            }
        }

        // Only setup biometric prompt for initial setup or reset, NOT for re-authentication initial screen
        if (!isReauthenticateMode) {
            // Setup fingerprint prompt for enabling it initially
            setupBiometricPromptForSetup()
        } else {
            // If in re-authenticate mode, show fingerprint login button if available
            val securePref = SecurePrefsUtil.getSecurePrefs(this)
            val fingerprintEnabled = securePref.getBoolean("FINGERPRINT_ENABLED", false)
            if (fingerprintEnabled && checkBiometricsAvailable()) {
                // This part is already handled above in the initial `if (isReauthenticateMode)` block
                // No need to duplicate setupBiometricPromptForReauth() call here.
                // It's called once at the top if isReauthenticateMode is true.
            } else {
                // No fingerprint, or not enabled, show password fields for login
                // This part is also handled above in the initial `if (isReauthenticateMode)` block
            }
        }
    }

    override fun onResume() {
        super.onResume()
        aegisPassApplication.activityResumed()

        // IMPORTANT: The re-authentication logic should primarily be handled in onCreate
        // to ensure it's the first thing that happens when the activity is launched.
        // If `shouldShowPasswordScreen` is true here, it means the user might have
        // navigated back to this screen or the app resumed from background while on this screen.
        // We should not re-redirect if already on the re-authentication screen.
        // The biometric prompt should be shown again if it was dismissed.
        if (isReauthenticateMode && PasswordSecurityManager.shouldShowPasswordScreen) {
            val securePref = SecurePrefsUtil.getSecurePrefs(this)
            val fingerprintEnabled = securePref.getBoolean("FINGERPRINT_ENABLED", false)
            if (fingerprintEnabled && checkBiometricsAvailable()) {
                // Re-show the biometric prompt if it's the primary login method for re-auth
                biometricPrompt.authenticate(promptInfo) // Re-authenticate
            }
        }
    }

    override fun onPause() {
        super.onPause()
        aegisPassApplication.activityPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        aegisPassApplication.activityConfigurationChanged(isChangingConfigurations)
    }

    private fun saveMasterPassword(masterPassword: String) {
        val masterPasswordHash = PasswordHasher.hashPassword(masterPassword)

        val securePref = SecurePrefsUtil.getSecurePrefs(this)
        with (securePref.edit()) {
            putString("MASTER_PASSWORD_HASH", masterPasswordHash)
            putBoolean("FINGERPRINT_ENABLED", false)
            apply()
        }

        SessionManager.setMasterPassword(masterPassword)

        binding.messageTextView.text = if (isResetMode) "Master Password updated successfully!" else "Master Password set successfully!"
        binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_primary, theme))
        binding.messageTextView.visibility = View.VISIBLE

        binding.fingerprintSetupLayout.visibility = View.VISIBLE
        binding.savePasswordButton.visibility = View.GONE
        binding.newPasswordInputLayout.visibility = View.GONE
        binding.confirmPasswordInputLayout.visibility = View.GONE
        binding.passwordStrengthTextView.visibility = View.GONE
        binding.masterPasswordInfo.visibility = View.GONE

        setupBiometricPromptForSetup()
        binding.enableFingerprintButton.setOnClickListener {
            checkBiometricsAndAuthenticateForSetup()
        }
        binding.skipFingerprintButton.setOnClickListener {
            navigateToDashboard()
        }
    }

    private fun checkBiometricsAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    private fun setupBiometricPromptForSetup() {
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Fingerprint Login")
            .setSubtitle("Use your fingerprint to quickly unlock AegisPass.")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    runOnUiThread {
                        Toast.makeText(this@SetupPasswordActivity, "Fingerprint authentication error: $errString", Toast.LENGTH_SHORT).show()
                        binding.messageTextView.text = "Fingerprint setup failed ($errString). You can skip or try again."
                        binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_red_dark, theme))
                        binding.messageTextView.visibility = View.VISIBLE
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    runOnUiThread {
                        Toast.makeText(this@SetupPasswordActivity, "Fingerprint authentication succeeded! Enabled.", Toast.LENGTH_SHORT).show()
                        val securePref = SecurePrefsUtil.getSecurePrefs(this@SetupPasswordActivity)
                        with (securePref.edit()) {
                            putBoolean("FINGERPRINT_ENABLED", true)
                            apply()
                        }
                        binding.messageTextView.text = "Fingerprint login enabled! Redirecting..."
                        binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_green_dark, theme))
                        binding.messageTextView.visibility = View.VISIBLE
                        navigateToDashboard()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    runOnUiThread {
                        Toast.makeText(this@SetupPasswordActivity, "Fingerprint authentication failed.", Toast.LENGTH_SHORT).show()
                        binding.messageTextView.text = "Fingerprint authentication failed. Try again."
                        binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_orange_dark, theme))
                        binding.messageTextView.visibility = View.VISIBLE
                    }
                }
            })
    }

    private fun setupBiometricPromptForReauth() {
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
                        Toast.makeText(this@SetupPasswordActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                        binding.messageTextView.text = "Biometric login failed ($errString). Please use password."
                        binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_red_dark, theme))
                        binding.messageTextView.visibility = View.VISIBLE
                        binding.fingerprintSetupLayout.visibility = View.GONE
                        binding.newPasswordInputLayout.visibility = View.VISIBLE
                        binding.confirmPasswordInputLayout.visibility = View.GONE // Only one password field for login
                        binding.savePasswordButton.visibility = View.VISIBLE
                        binding.savePasswordButton.text = "LOGIN"
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    runOnUiThread {
                        val securePref = SecurePrefsUtil.getSecurePrefs(this@SetupPasswordActivity)
                        val storedMasterPasswordHash = securePref.getString("MASTER_PASSWORD_HASH", null)
                        if (storedMasterPasswordHash != null) {
                            SessionManager.setMasterPassword("biometric_authenticated_dummy_key")
                            PasswordSecurityManager.shouldShowPasswordScreen = false
                            Toast.makeText(this@SetupPasswordActivity, "Re-authentication succeeded!", Toast.LENGTH_SHORT).show()
                            navigateToDashboard()
                        } else {
                            Toast.makeText(this@SetupPasswordActivity, "Biometric succeeded, but no master password found. Please setup.", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@SetupPasswordActivity, SetupPasswordActivity::class.java).apply {
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
                        Toast.makeText(this@SetupPasswordActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        binding.messageTextView.text = "Biometric authentication failed. Try again."
                        binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_orange_dark, theme))
                        binding.messageTextView.visibility = View.VISIBLE
                    }
                }
            })
    }

    private fun checkBiometricsAndAuthenticateForSetup() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                runOnUiThread {
                    Toast.makeText(this, "No biometric hardware features available.", Toast.LENGTH_LONG).show()
                    binding.messageTextView.text = "No fingerprint hardware found on this device."
                    binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_red_light, theme))
                    binding.messageTextView.visibility = View.VISIBLE
                }
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                runOnUiThread {
                    Toast.makeText(this, "Biometric features are currently unavailable.", Toast.LENGTH_LONG).show()
                    binding.messageTextView.text = "Fingerprint hardware is unavailable."
                    binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_red_light, theme))
                    binding.messageTextView.visibility = View.VISIBLE
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                runOnUiThread {
                    Toast.makeText(this, "No fingerprint enrolled. Please enroll fingerprints in device settings.", Toast.LENGTH_LONG).show()
                    binding.messageTextView.text = "No fingerprint enrolled. Please setup in device settings."
                    binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_orange_dark, theme))
                    binding.messageTextView.visibility = View.VISIBLE
                }
            }
            else -> {
                runOnUiThread {
                    Toast.makeText(this, "Biometric authentication not possible.", Toast.LENGTH_LONG).show()
                    binding.messageTextView.text = "Biometric authentication not possible on this device."
                    binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_red_dark, theme))
                    binding.messageTextView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun checkBiometricsAndAuthenticateForReauth() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE, BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                runOnUiThread {
                    Toast.makeText(this, "Biometric unavailable. Please use password.", Toast.LENGTH_LONG).show()
                    binding.messageTextView.text = "Biometric unavailable. Please use password."
                    binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_red_light, theme))
                    binding.messageTextView.visibility = View.VISIBLE
                    binding.fingerprintSetupLayout.visibility = View.GONE
                    binding.newPasswordInputLayout.visibility = View.VISIBLE
                    binding.confirmPasswordInputLayout.visibility = View.GONE
                    binding.savePasswordButton.visibility = View.VISIBLE
                    binding.savePasswordButton.text = "LOGIN"
                }
            }
            else -> {
                runOnUiThread {
                    Toast.makeText(this, "Biometric authentication not possible.", Toast.LENGTH_LONG).show()
                    binding.messageTextView.text = "Biometric authentication not possible on this device. Please use password."
                    binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, android.R.color.holo_red_dark, theme))
                    binding.messageTextView.visibility = View.VISIBLE
                    binding.fingerprintSetupLayout.visibility = View.GONE
                    binding.newPasswordInputLayout.visibility = View.VISIBLE
                    binding.confirmPasswordInputLayout.visibility = View.GONE
                    binding.savePasswordButton.visibility = View.VISIBLE
                    binding.savePasswordButton.text = "LOGIN"
                }
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
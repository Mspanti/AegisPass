package com.pant.aegispass

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pant.aegispass.databinding.ActivitySettingsBinding
import com.pant.aegispass.databinding.DialogMasterPasswordPromptBinding
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var isResetMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button
        supportActionBar?.title = "Settings"

        executor = Executors.newSingleThreadExecutor()

        // Use SecurePrefsUtil for sensitive preferences
        val securePref = SecurePrefsUtil.getSecurePrefs(this)
        val isFingerprintEnabled = securePref.getBoolean("FINGERPRINT_ENABLED", false)

        binding.fingerprintSwitch.isChecked = isFingerprintEnabled

        // Set app version using an alternative method to bypass BuildConfig issue
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            binding.versionTextView.text = "Version: ${pInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            binding.versionTextView.text = "Version: N/A"
        }

        binding.resetMasterPasswordButton.setOnClickListener {
            showResetMasterPasswordDialog()
        }

        binding.fingerprintSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBiometricsAndAuthenticateForEnable()
            } else {
                showDisableFingerprintConfirmation()
            }
        }

        binding.digitalFootprintButton.setOnClickListener {
            val intent = Intent(this, DigitalFootprintActivity::class.java)
            startActivity(intent)
        }

        setupBiometricPromptForToggle()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun showResetMasterPasswordDialog() {
        val dialogBinding = DialogMasterPasswordPromptBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.dialogTitle.text = "Confirm Master Password"
        dialogBinding.dialogMessage.visibility = View.GONE

        dialogBinding.okButton.setOnClickListener {
            val enteredPassword = dialogBinding.masterPasswordEditText.text.toString()
            val securePref = SecurePrefsUtil.getSecurePrefs(this) // Use securePrefs
            val storedMasterPasswordHash = securePref.getString("MASTER_PASSWORD_HASH", null)

            if (enteredPassword.isNotEmpty() && storedMasterPasswordHash != null &&
                PasswordHasher.verifyPassword(enteredPassword, storedMasterPasswordHash)) {
                dialog.dismiss()
                val intent = Intent(this, SetupPasswordActivity::class.java)
                intent.putExtra("RESET_MODE", true)
                startActivity(intent)
                finish()
            } else {
                dialogBinding.dialogMessage.text = "Incorrect Master Password."
                dialogBinding.dialogMessage.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
                dialogBinding.dialogMessage.visibility = View.VISIBLE
            }
        }

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showDisableFingerprintConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Disable Fingerprint Login")
            .setMessage("Are you sure you want to disable fingerprint login? You will need to use your Master Password to log in.")
            .setPositiveButton("DISABLE") { dialog, _ ->
                val securePref = SecurePrefsUtil.getSecurePrefs(this) // Use securePrefs
                with(securePref.edit()) {
                    putBoolean("FINGERPRINT_ENABLED", false)
                    apply()
                }
                binding.fingerprintSwitch.isChecked = false
                Toast.makeText(this, "Fingerprint login disabled.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                binding.fingerprintSwitch.isChecked = true
                dialog.dismiss()
            }
            .show()
    }

    private fun setupBiometricPromptForToggle() {
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Fingerprint Login")
            .setSubtitle("Authenticate to enable fingerprint login.")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                        binding.fingerprintSwitch.isChecked = false
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    runOnUiThread {
                        val securePref = SecurePrefsUtil.getSecurePrefs(this@SettingsActivity) // Use securePrefs
                        with(securePref.edit()) {
                            putBoolean("FINGERPRINT_ENABLED", true)
                            apply()
                        }
                        binding.fingerprintSwitch.isChecked = true
                        Toast.makeText(this@SettingsActivity, "Fingerprint login enabled!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        binding.fingerprintSwitch.isChecked = false
                    }
                }
            })
    }

    private fun checkBiometricsAndAuthenticateForEnable() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                runOnUiThread {
                    Toast.makeText(this, "No biometric hardware features available.", Toast.LENGTH_LONG).show()
                    binding.fingerprintSwitch.isChecked = false
                }
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                runOnUiThread {
                    Toast.makeText(this, "Biometric features are currently unavailable.", Toast.LENGTH_LONG).show()
                    binding.fingerprintSwitch.isChecked = false
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                runOnUiThread {
                    Toast.makeText(this, "No fingerprint enrolled. Please enroll fingerprints in device settings.", Toast.LENGTH_LONG).show()
                    binding.fingerprintSwitch.isChecked = false
                }
            }
            else -> {
                runOnUiThread {
                    Toast.makeText(this, "Biometric authentication not possible.", Toast.LENGTH_LONG).show()
                    binding.fingerprintSwitch.isChecked = false
                }
            }
        }
    }
}
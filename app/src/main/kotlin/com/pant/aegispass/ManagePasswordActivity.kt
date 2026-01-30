package com.pant.aegispass

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pant.aegispass.databinding.ActivityManagePasswordBinding
import com.pant.aegispass.data.local.AppDatabase
import com.pant.aegispass.data.local.PasswordDao
import com.pant.aegispass.data.local.PasswordEntry
import com.pant.aegispass.core.security.PasswordCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64

class ManagePasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagePasswordBinding
    private lateinit var passwordDao: PasswordDao
    private var entryId: Long = 0
    private lateinit var masterPasswordForEncryption: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Password"

        passwordDao = AppDatabase.getDatabase(applicationContext).passwordDao()

        masterPasswordForEncryption = SessionManager.getMasterPassword()
            ?: run {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

        entryId = intent.getLongExtra("PASSWORD_ENTRY_ID", 0L)
        if (entryId != 0L) {
            loadPasswordEntry(entryId)
            binding.savePasswordButton.text = "UPDATE ENTRY"
            binding.toolbar.title = "Edit Password"
        } else {
            binding.savePasswordButton.text = "ADD ENTRY"
            binding.toolbar.title = "Add New Password"
        }

        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
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
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.savePasswordButton.setOnClickListener {
            savePasswordEntry()
        }

        binding.generatePasswordButton.setOnClickListener {
            val generatedPassword = PasswordGenerator.generateStrongPassword()
            binding.passwordEditText.setText(generatedPassword)
            binding.confirmPasswordEditText.setText(generatedPassword)
            Toast.makeText(this, "Password generated!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun loadPasswordEntry(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val entry = passwordDao.getEntryById(id)
            withContext(Dispatchers.Main) {
                entry?.let {
                    binding.serviceNameEditText.setText(it.serviceName)
                    binding.usernameEditText.setText(it.username)
                    binding.passwordEditText.setText(
                        PasswordCipher.decrypt(
                            it.encryptedPassword,
                            masterPasswordForEncryption,
                            Base64.decode(it.entrySalt, Base64.DEFAULT)
                        )
                    )
                    binding.confirmPasswordEditText.setText(
                        PasswordCipher.decrypt(
                            it.encryptedPassword,
                            masterPasswordForEncryption,
                            Base64.decode(it.entrySalt, Base64.DEFAULT)
                        )
                    )
                    // Decrypt notes before setting to EditText
                    val decryptedNotes = it.encryptedNotes?.let { encryptedNotes ->
                        try {
                            PasswordCipher.decrypt(encryptedNotes, masterPasswordForEncryption, Base64.decode(it.entrySalt, Base64.DEFAULT))
                        } catch (e: Exception) {
                            println("Error decrypting notes: ${e.message}")
                            "DECRYPTION_ERROR" // Indicate error
                        }
                    }
                    binding.notesEditText.setText(decryptedNotes)
                }
            }
        }
    }

    private fun savePasswordEntry() {
        // Sanitize inputs before validation and saving
        val serviceName = InputSanitizer.sanitizeText(binding.serviceNameEditText.text.toString())
        val username = InputSanitizer.sanitizeText(binding.usernameEditText.text.toString())
        val password = InputSanitizer.sanitizePassword(binding.passwordEditText.text.toString())
        val confirmPassword = InputSanitizer.sanitizePassword(binding.confirmPasswordEditText.text.toString())
        val notes = InputSanitizer.sanitizeText(binding.notesEditText.text.toString())

        if (serviceName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            binding.messageTextView.text = "All fields are required."
            binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
            binding.messageTextView.visibility = View.VISIBLE
            return
        }

        if (password != confirmPassword) {
            binding.messageTextView.text = "Passwords do not match."
            binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
            binding.messageTextView.visibility = View.VISIBLE
            return
        }

        // Optional: Add email format validation if username is expected to be an email
        if (username.contains("@") && !InputSanitizer.isValidEmail(username)) {
            binding.messageTextView.text = "Please enter a valid email format for username."
            binding.messageTextView.setTextColor(ResourcesCompat.getColor(resources, com.google.android.material.R.color.design_default_color_error, theme))
            binding.messageTextView.visibility = View.VISIBLE
            return
        }


        val (strength, _) = PasswordStrengthChecker.checkStrength(password)
        if (strength == PasswordStrengthChecker.Strength.WEAK) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Weak Password Warning")
                .setMessage("The password you entered is weak. Using weak passwords can compromise your security. Do you still want to save it?")
                .setPositiveButton("SAVE ANYWAY") { dialog, _ ->
                    performSave(serviceName, username, password, notes)
                    dialog.dismiss()
                }
                .setNegativeButton("CANCEL") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            performSave(serviceName, username, password, notes)
        }
    }

    private fun performSave(serviceName: String, username: String, passwordPlain: String, notes: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val salt = PasswordCipher.generateSalt()
            val encryptedPassword = PasswordCipher.encrypt(passwordPlain, masterPasswordForEncryption, salt)
            val base64Salt = Base64.encodeToString(salt, Base64.DEFAULT)
            val encryptedNotes = notes.let {
                if (it.isNotEmpty()) PasswordCipher.encrypt(it, masterPasswordForEncryption, salt) else null
            }
            val currentTime = System.currentTimeMillis()

            val passwordEntry = if (entryId == 0L) {
                // New entry
                PasswordEntry(
                    serviceName = serviceName,
                    username = username,
                    encryptedPassword = encryptedPassword,
                    encryptedNotes = encryptedNotes,
                    entrySalt = base64Salt,
                    lastUpdated = currentTime
                )
            } else {
                // Existing entry
                passwordDao.getEntryById(entryId)?.copy(
                    serviceName = serviceName,
                    username = username,
                    encryptedPassword = encryptedPassword,
                    encryptedNotes = encryptedNotes,
                    entrySalt = base64Salt,
                    lastUpdated = currentTime
                ) ?: return@launch
            }

            if (entryId == 0L) {
                passwordDao.insert(passwordEntry)
            } else {
                passwordDao.update(passwordEntry)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ManagePasswordActivity, "Password entry saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
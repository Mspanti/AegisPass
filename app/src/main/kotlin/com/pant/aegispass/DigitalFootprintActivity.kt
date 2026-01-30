package com.pant.aegispass

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pant.aegispass.databinding.ActivityDigitalFootprintBinding
import com.pant.aegispass.data.local.AppDatabase
import com.pant.aegispass.data.local.PasswordDao
import com.pant.aegispass.data.local.PasswordEntry
import com.pant.aegispass.core.security.PasswordCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64
import android.widget.Toast

class DigitalFootprintActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDigitalFootprintBinding
    private lateinit var passwordDao: PasswordDao
    private lateinit var masterPasswordForDecryption: String
    private lateinit var adapter: DigitalFootprintAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDigitalFootprintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Digital Footprint"

        passwordDao = AppDatabase.getDatabase(applicationContext).passwordDao()

        masterPasswordForDecryption = SessionManager.getMasterPassword()
            ?: run {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return
            }

        adapter = DigitalFootprintAdapter(
            masterPassword = masterPasswordForDecryption,
            onFixIssueClick = { entryToFix ->
                val intent = Intent(this@DigitalFootprintActivity, ManagePasswordActivity::class.java).apply {
                    putExtra("PASSWORD_ENTRY_ID", entryToFix.id)
                }
                startActivity(intent)
            }
        )
        binding.digitalFootprintRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.digitalFootprintRecyclerView.adapter = adapter

        observePasswordEntries()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun observePasswordEntries() {
        lifecycleScope.launch {
            passwordDao.getAllPasswords().collectLatest { passwordList ->
                if (passwordList.isEmpty()) {
                    binding.noEntriesMessage.visibility = View.VISIBLE
                    binding.digitalFootprintRecyclerView.visibility = View.GONE
                    binding.riskSummaryTextView.visibility = View.GONE
                } else {
                    binding.noEntriesMessage.visibility = View.GONE
                    binding.digitalFootprintRecyclerView.visibility = View.VISIBLE
                    binding.riskSummaryTextView.visibility = View.VISIBLE

                    val riskUseCase = com.pant.aegispass.domain.usecase.RiskAssessmentUseCase()
                    val entriesWithRisk = mutableListOf<com.pant.aegispass.RiskAssessor.PasswordRisk>()
                    for (entry in passwordList) {
                        val decryptedPassword = try {
                            PasswordCipher.decrypt(
                                entry.encryptedPassword,
                                masterPasswordForDecryption,
                                Base64.decode(entry.entrySalt, Base64.DEFAULT)
                            )
                        } catch (e: Exception) {
                            "DECRYPTION_ERROR" // Handle decryption errors
                        }
                        val risk = riskUseCase(entry, decryptedPassword, passwordList)
                        entriesWithRisk.add(risk)
                    }

                    // Sort by risk score (highest risk first)
                    val sortedEntries = entriesWithRisk.sortedByDescending { it.riskScore }
                    adapter.submitList(sortedEntries)
                    updateRiskSummary(sortedEntries)
                }
            }
        }
    }

    private fun updateRiskSummary(entriesWithRisk: List<RiskAssessor.PasswordRisk>) {
        val criticalCount = entriesWithRisk.count { it.riskScore >= 80 } // Example threshold
        val highCount = entriesWithRisk.count { it.riskScore >= 60 && it.riskScore < 80 }
        val mediumCount = entriesWithRisk.count { it.riskScore >= 40 && it.riskScore < 60 }
        val lowCount = entriesWithRisk.count { it.riskScore < 40 }

        val totalEntries = entriesWithRisk.size

        val summary = StringBuilder("Overall Risk:\n")
        if (criticalCount > 0) summary.append("- $criticalCount Critical\n")
        if (highCount > 0) summary.append("- $highCount High\n")
        if (mediumCount > 0) summary.append("- $mediumCount Medium\n")
        if (lowCount > 0) summary.append("- $lowCount Low\n")

        if (totalEntries == 0) {
            summary.append("No entries to assess.")
        } else {
            summary.append("Total entries: $totalEntries")
        }

        binding.riskSummaryTextView.text = summary.toString()
    }
}
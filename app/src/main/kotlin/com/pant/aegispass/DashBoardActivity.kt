package com.pant.aegispass

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.pant.aegispass.presentation.dashboard.DashboardViewModelFactory
import com.pant.aegispass.presentation.dashboard.DashboardViewModel
import com.pant.aegispass.presentation.dashboard.DashboardIntent
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pant.aegispass.databinding.ActivityDashboardBinding
import com.pant.aegispass.data.local.AppDatabase
import com.pant.aegispass.data.local.PasswordDao
import com.pant.aegispass.data.local.PasswordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var passwordDao: PasswordDao
    private lateinit var masterPasswordForDecryption: String
    private lateinit var passwordAdapter: PasswordEntryAdapter
    private var allPasswords: List<PasswordEntry> = emptyList()

    // ViewModel
    private lateinit var dashboardViewModel: com.pant.aegispass.presentation.dashboard.DashboardViewModel

    // Get the custom Application instance
    private val aegisPassApplication: AegisPassApplication
        get() = application as AegisPassApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "My Passwords"

        masterPasswordForDecryption = SessionManager.getMasterPassword()
            ?: run {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return
            }

        passwordAdapter = PasswordEntryAdapter(
            masterPassword = masterPasswordForDecryption,
            onEditClick = { entryToEdit ->
                val intent = Intent(this@DashboardActivity, ManagePasswordActivity::class.java).apply {
                    putExtra("PASSWORD_ENTRY_ID", entryToEdit.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { entryToDelete ->
                showDeleteConfirmationDialog(entryToDelete)
            }
        )
        binding.passwordRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = passwordAdapter
        }

        // ViewModel + MVI wiring
        val factory = DashboardViewModelFactory(this)
        dashboardViewModel = ViewModelProvider(this, factory).get(DashboardViewModel::class.java)
        lifecycleScope.launchWhenStarted {
            dashboardViewModel.state.collect { st ->
                passwordAdapter.submitList(st.passwords)
                updateEmptyView(st.passwords.isEmpty())
            }
        }
        dashboardViewModel.handle(DashboardIntent.LoadPasswords)

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    val searchView = menuItem.actionView as? SearchView
                    searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            filterPasswords(query.orEmpty())
                            searchView.clearFocus()
                            return true
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            filterPasswords(newText.orEmpty())
                            return true
                        }
                    })
                    true
                }
                R.id.action_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_digital_footprint -> {
                    val intent = Intent(this, DigitalFootprintActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_private_safe -> {
                    val intent = Intent(this, PrivateSafeActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> false
            }
        }

        binding.fabAddPassword.setOnClickListener {
            val intent = Intent(this, ManagePasswordActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        aegisPassApplication.activityResumed()

        if (PasswordSecurityManager.shouldShowPasswordScreen) {
            val intent = Intent(this, SetupPasswordActivity::class.java).apply {
                putExtra("REAUTHENTICATE_MODE", true) // Pass this flag
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            PasswordSecurityManager.shouldShowPasswordScreen = false
        }
    }

    override fun onPause() {
        super.onPause()
        aegisPassApplication.activityPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Use the getter to access the application instance
        aegisPassApplication.activityConfigurationChanged(isChangingConfigurations)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.queryHint = "Search passwords..."
        return true
    }

    private fun observePasswords() {
        lifecycleScope.launch {
            passwordDao.getAllPasswords().collectLatest { passwordList ->
                allPasswords = passwordList
                filterPasswords("")
            }
        }
    }

    private fun filterPasswords(query: String) {
        val filteredList = if (query.isBlank()) {
            allPasswords
        } else {
            allPasswords.filter {
                it.serviceName.contains(query, ignoreCase = true) ||
                        it.username.contains(query, ignoreCase = true)
            }
        }
        passwordAdapter.submitList(filteredList)
        updateEmptyView(filteredList.isEmpty() && query.isBlank())
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.passwordRecyclerView.visibility = View.GONE
            binding.noPasswordsMessage.visibility = View.VISIBLE
        } else {
            binding.passwordRecyclerView.visibility = View.VISIBLE
            binding.noPasswordsMessage.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmationDialog(entry: PasswordEntry) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Password Entry")
            .setMessage("Are you sure you want to delete the entry for ${entry.serviceName} (${entry.username})?")
            .setPositiveButton("DELETE") { dialog, _ ->
                // Use ViewModel to handle delete via use case
                dashboardViewModel.handle(DashboardIntent.DeletePassword(entry.id))
                Toast.makeText(this@DashboardActivity, "Entry delete requested.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out? You will need your Master Password to access AegisPass again.")
            .setPositiveButton("LOGOUT") { dialog, _ ->
                SessionManager.clearMasterPassword()
                Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
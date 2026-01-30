package com.pant.aegispass

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pant.aegispass.databinding.ItemPasswordEntryBinding
import com.pant.aegispass.data.local.PasswordEntry
import com.pant.aegispass.core.security.PasswordCipher

class PasswordEntryAdapter(
    private val masterPassword: String,
    private val onDeleteClick: (PasswordEntry) -> Unit,
    private val onEditClick: (PasswordEntry) -> Unit
) : ListAdapter<PasswordEntry, PasswordEntryAdapter.PasswordEntryViewHolder>(PasswordEntryDiffCallback()) {

    inner class PasswordEntryViewHolder(private val binding: ItemPasswordEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isPasswordVisible = false

        init {
            binding.optionsMenuButton.setOnClickListener { view ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showOptionsMenu(view, getItem(position))
                }
            }

            // Copy password when tapping the copy icon
            binding.copyPasswordButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    copyDecryptedPasswordToClipboard(binding.root.context, getItem(position))
                }
            }

            // Toggle visibility when tapping the eye icon or the password text
            binding.showHidePasswordButton.setOnClickListener {
                togglePasswordVisibility()
            }
            binding.passwordMaskedTextView.setOnClickListener {
                togglePasswordVisibility()
            }
        }

        fun bind(passwordEntry: PasswordEntry) {
            binding.serviceNameTextView.text = passwordEntry.serviceName
            binding.usernameTextView.text = passwordEntry.username
            // Reset state
            isPasswordVisible = false
            binding.passwordMaskedTextView.text = "••••••••••"
            binding.decryptionErrorIcon.visibility = View.GONE
            binding.decryptionErrorTextView.visibility = View.GONE
            binding.showHidePasswordButton.setImageResource(R.drawable.ic_visibility_24)
        }

        private fun togglePasswordVisibility() {
            val pos = adapterPosition
            if (pos == RecyclerView.NO_POSITION) return
            val entry = getItem(pos)

            if (!isPasswordVisible) {
                try {
                    val decryptedPassword = PasswordCipher.decrypt(
                        entry.encryptedPassword,
                        masterPassword,
                        Base64.decode(entry.entrySalt, Base64.DEFAULT)
                    )
                    binding.passwordMaskedTextView.text = decryptedPassword
                    binding.decryptionErrorIcon.visibility = View.GONE
                    binding.decryptionErrorTextView.visibility = View.GONE
                    binding.showHidePasswordButton.setImageResource(R.drawable.ic_visibility_off_24)
                    isPasswordVisible = true
                } catch (e: Exception) {
                    // Show subtle error state
                    binding.decryptionErrorIcon.visibility = View.VISIBLE
                    binding.decryptionErrorTextView.visibility = View.VISIBLE
                    binding.passwordMaskedTextView.text = "••••••••••"
                    binding.showHidePasswordButton.setImageResource(R.drawable.ic_visibility_24)
                    isPasswordVisible = false
                }
            } else {
                binding.passwordMaskedTextView.text = "••••••••••"
                binding.showHidePasswordButton.setImageResource(R.drawable.ic_visibility_24)
                isPasswordVisible = false
                binding.decryptionErrorIcon.visibility = View.GONE
                binding.decryptionErrorTextView.visibility = View.GONE
            }
        }

        private fun showOptionsMenu(view: View, passwordEntry: PasswordEntry) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.item_password_menu, popup.menu) // Corrected reference

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_copy_password -> { // Corrected reference
                        copyDecryptedPasswordToClipboard(view.context, passwordEntry)
                        true
                    }
                    R.id.action_edit_entry -> { // Corrected reference
                        onEditClick(passwordEntry)
                        true
                    }
                    R.id.action_delete_entry -> { // Corrected reference
                        showDeleteConfirmationDialog(view.context, passwordEntry)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun copyDecryptedPasswordToClipboard(context: Context, passwordEntry: PasswordEntry) {
            try {
                val decryptedPassword = PasswordCipher.decrypt(
                    passwordEntry.encryptedPassword,
                    masterPassword,
                    Base64.decode(passwordEntry.entrySalt, Base64.DEFAULT)
                )
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Password", decryptedPassword)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Password copied to clipboard!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to copy password.", Toast.LENGTH_SHORT).show()
            }
        }

        private fun showDeleteConfirmationDialog(context: Context, entryToDelete: PasswordEntry) {
            MaterialAlertDialogBuilder(context)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this password entry for '${entryToDelete.serviceName}'?")
                .setNegativeButton("CANCEL") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("DELETE") { dialog, _ ->
                    onDeleteClick(entryToDelete)
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordEntryViewHolder {
        val binding = ItemPasswordEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PasswordEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PasswordEntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PasswordEntryDiffCallback : DiffUtil.ItemCallback<PasswordEntry>() {
    override fun areItemsTheSame(oldItem: PasswordEntry, newItem: PasswordEntry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PasswordEntry, newItem: PasswordEntry): Boolean {
        return oldItem == newItem
    }
}
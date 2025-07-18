package com.pant.aegispass

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pant.aegispass.databinding.ItemDigitalFootprintEntryBinding

class DigitalFootprintAdapter(
    private val masterPassword: String, // Needed for potential future decryption in adapter if required
    private val onFixIssueClick: (PasswordEntry) -> Unit
) : ListAdapter<RiskAssessor.PasswordRisk, DigitalFootprintAdapter.DigitalFootprintViewHolder>(DigitalFootprintDiffCallback()) {

    inner class DigitalFootprintViewHolder(private val binding: ItemDigitalFootprintEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(passwordRisk: RiskAssessor.PasswordRisk) {
            binding.serviceNameTextView.text = passwordRisk.entry.serviceName
            binding.usernameTextView.text = passwordRisk.entry.username
            binding.riskScoreTextView.text = passwordRisk.riskScore.toString()

            // Update risk reasons
            if (passwordRisk.reasons.isNotEmpty()) {
                binding.riskReasonTextView.text = "Reasons: ${passwordRisk.reasons.joinToString(", ")}"
                binding.riskReasonTextView.visibility = View.VISIBLE
            } else {
                binding.riskReasonTextView.visibility = View.GONE
            }

            // Set color based on risk score
            val riskColor = when {
                passwordRisk.riskScore >= 80 -> Color.RED // Critical
                passwordRisk.riskScore >= 60 -> Color.parseColor("#FF9800") // Orange - High
                passwordRisk.riskScore >= 40 -> Color.YELLOW // Medium
                else -> Color.GREEN // Low
            }
            binding.riskScoreTextView.setTextColor(riskColor)

            // Show password breach status if applicable
            if (passwordRisk.isPasswordBreached) {
                binding.passwordBreachStatusTextView.visibility = View.VISIBLE
            } else {
                binding.passwordBreachStatusTextView.visibility = View.GONE
            }

            // Show email breach status if applicable
            if (passwordRisk.isEmailBreached) {
                binding.emailBreachStatusTextView.visibility = View.VISIBLE
            } else {
                binding.emailBreachStatusTextView.visibility = View.GONE
            }

            // Show Fix Issue button if there are any reasons for risk or if breached
            if (passwordRisk.reasons.isNotEmpty() || passwordRisk.isPasswordBreached || passwordRisk.isEmailBreached) {
                binding.fixIssueButton.visibility = View.VISIBLE
                binding.fixIssueButton.setOnClickListener {
                    onFixIssueClick(passwordRisk.entry)
                }
            } else {
                binding.fixIssueButton.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DigitalFootprintViewHolder {
        val binding = ItemDigitalFootprintEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DigitalFootprintViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DigitalFootprintViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class DigitalFootprintDiffCallback : DiffUtil.ItemCallback<RiskAssessor.PasswordRisk>() {
    override fun areItemsTheSame(oldItem: RiskAssessor.PasswordRisk, newItem: RiskAssessor.PasswordRisk): Boolean {
        return oldItem.entry.id == newItem.entry.id
    }

    override fun areContentsTheSame(oldItem: RiskAssessor.PasswordRisk, newItem: RiskAssessor.PasswordRisk): Boolean {
        return oldItem == newItem // Data class comparison
    }
}
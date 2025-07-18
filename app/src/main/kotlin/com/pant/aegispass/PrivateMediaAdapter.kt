package com.pant.aegispass

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pant.aegispass.databinding.ItemPrivateMediaBinding
import java.io.File

class PrivateMediaAdapter(
    private val mediaList: List<PrivateMediaItem>,
    private val onItemClick: (PrivateMediaItem, Int) -> Unit,
    private val onItemLongClick: (PrivateMediaItem, Int) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<PrivateMediaAdapter.PrivateMediaViewHolder>() {

    private var isSelectionMode: Boolean = false
    private val selectedItems = mutableSetOf<PrivateMediaItem>()

    inner class PrivateMediaViewHolder(private val binding: ItemPrivateMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PrivateMediaItem) {
            binding.mediaName.text = item.fileName

            Glide.with(binding.mediaThumbnail.context)
                .load(File(item.filePath))
                .centerCrop()
                .into(binding.mediaThumbnail)

            binding.videoIcon.visibility = if (item.isVideo) View.VISIBLE else View.GONE

            // Handle selection UI
            binding.selectionCheckBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            binding.selectionCheckBox.isChecked = selectedItems.contains(item)

            // Change background color if selected
            if (selectedItems.contains(item)) {
                binding.mediaItemLayout.setBackgroundColor(Color.parseColor("#E0E0E0")) // Light grey for selected
            } else {
                binding.mediaItemLayout.setBackgroundColor(Color.TRANSPARENT) // Transparent for unselected
            }

            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(item)
                } else {
                    onItemClick(item, adapterPosition)
                }
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(item, adapterPosition)
                true // Consume the long click event
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrivateMediaViewHolder {
        val binding = ItemPrivateMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PrivateMediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrivateMediaViewHolder, position: Int) {
        holder.bind(mediaList[position])
    }

    override fun getItemCount(): Int = mediaList.size

    fun setSelectionMode(enabled: Boolean) {
        if (isSelectionMode != enabled) {
            isSelectionMode = enabled
            if (!enabled) {
                selectedItems.clear() // Clear selection when exiting selection mode
            }
            notifyDataSetChanged() // Rebind all items to show/hide checkboxes
            onSelectionChanged(selectedItems.size) // Update activity with selection count
        }
    }

    fun toggleSelection(item: PrivateMediaItem) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
        notifyItemChanged(mediaList.indexOf(item)) // Only rebind the changed item
        onSelectionChanged(selectedItems.size) // Update activity with selection count
    }

    // FIX: Change return type to MutableSet<PrivateMediaItem>
    fun getSelectedItems(): MutableSet<PrivateMediaItem> {
        return selectedItems
    }

    fun clearSelection() {
        selectedItems.clear()
        setSelectionMode(false) // Exit selection mode
    }

    fun isSelectionModeActive(): Boolean {
        return isSelectionMode
    }
}
package com.pant.aegispass

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import java.io.File

// This adapter is now solely for displaying images
class ImageViewerAdapter(
    private val context: Context,
    private val items: List<PrivateMediaItem> // This list should now only contain image items
) : RecyclerView.Adapter<ImageViewerAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image_viewer, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = items[position]
        val file = File(item.filePath)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

        Glide.with(context)
            .load(uri)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = items.size

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: PhotoView = itemView.findViewById(R.id.photoView)
    }
}
package com.pant.aegispass

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView

import java.io.File

class MediaViewerAdapter(
    private val context: Context,
    private val items: List<PrivateMediaItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_IMAGE = 0
    private val VIEW_TYPE_VIDEO = 1
    private val playerMap = mutableMapOf<Int, ExoPlayer>()
    private var currentActivePlayer: ExoPlayer? = null

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isVideo) VIEW_TYPE_VIDEO else VIEW_TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_VIDEO) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_video_viewer, parent, false)
            VideoViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_image_viewer, parent, false)
            ImageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val file = File(item.filePath)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

        if (holder is ImageViewHolder) {
            Glide.with(context).load(uri).into(holder.imageView)
        } else if (holder is VideoViewHolder) {
            holder.bind(uri, position)
        }
    }

    override fun getItemCount(): Int = items.size

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: PhotoView = itemView.findViewById(R.id.photoView)
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerView: PlayerView = itemView.findViewById(R.id.playerView)

        var exoPlayer: ExoPlayer? = null

        init {
            playerView.setControllerVisibilityListener(object : PlayerControlView.VisibilityListener {
                override fun onVisibilityChange(visibility: Int) {
                    // No custom UI elements to toggle here anymore.
                }
            })
        }

        fun bind(uri: Uri, position: Int) {
            // Get or create ExoPlayer for this position
            exoPlayer = playerMap.getOrPut(position) {
                ExoPlayer.Builder(context).build()
            }
            playerView.player = exoPlayer

            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()

            // Ensures full controller with timeline is enabled by default
            playerView.useController = true
            playerView.controllerShowTimeoutMs = 3000 // Controls hide after 3 seconds of inactivity
            playerView.showController() // Force it to show on bind, so user can see play button

            // DO NOT set playWhenReady = true here. Video will not autoplay.
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VideoViewHolder) {
            holder.exoPlayer?.playWhenReady = false // Ensure it's paused when recycled
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is VideoViewHolder) {
            currentActivePlayer = holder.exoPlayer
            // DO NOT set playWhenReady = true here. Video will not autoplay when view is attached.
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is VideoViewHolder) {
            holder.exoPlayer?.playWhenReady = false // Pause when detached
            if (currentActivePlayer == holder.exoPlayer) currentActivePlayer = null
        }
    }

    fun getActiveExoPlayer(): ExoPlayer? = currentActivePlayer
    fun getPlayerAtPosition(position: Int): ExoPlayer? = playerMap[position]
    fun releaseAllPlayers() {
        playerMap.values.forEach { it.release() }
        playerMap.clear()
        currentActivePlayer = null
    }
}

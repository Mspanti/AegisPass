package com.pant.aegispass

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pant.aegispass.databinding.ActivityMediaViewerBinding

class MediaViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaViewerBinding
    private var mediaList: ArrayList<PrivateMediaItem>? = null
    private var currentPosition: Int = 0
    private lateinit var pagerAdapter: ImageViewerAdapter // Using ImageViewerAdapter for images only

    private val aegisPassApplication: AegisPassApplication
        get() = application as AegisPassApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Image Viewer" // Title changed to reflect image-only functionality

        val extras = intent.extras
        if (extras != null) {
            mediaList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelableArrayList("mediaList", PrivateMediaItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                val rawList: ArrayList<Parcelable>? = extras.getParcelableArrayList("mediaList")
                val tempMediaList = ArrayList<PrivateMediaItem>()
                rawList?.forEach { parcelable ->
                    if (parcelable is PrivateMediaItem) {
                        tempMediaList.add(parcelable)
                    }
                }
                tempMediaList
            }
        } else {
            mediaList = null
        }

        currentPosition = intent.getIntExtra("startPosition", 0)

        mediaList?.let { list ->
            // Filter out videos, as this activity will only handle images now
            val imageList = list.filter { !it.isVideo } as ArrayList<PrivateMediaItem>

            if (imageList.isEmpty()) {
                Toast.makeText(this, "No images to display.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            pagerAdapter = ImageViewerAdapter(this, imageList) // Use ImageViewerAdapter
            binding.viewPager.adapter = pagerAdapter
            binding.viewPager.setCurrentItem(currentPosition, false)

            // No specific PageChangeCallback needed as there's no internal video player logic here
        } ?: run {
            Toast.makeText(this, "No media to display.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        aegisPassApplication.activityResumed()

        if (PasswordSecurityManager.shouldShowPasswordScreen) {
            val intent = Intent(this, SetupPasswordActivity::class.java).apply {
                putExtra("REAUTHENTICATE_MODE", true)
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
        aegisPassApplication.activityConfigurationChanged(isChangingConfigurations)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // No options menu needed for this activity as it only displays images now
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return false // No menu for image viewer
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}
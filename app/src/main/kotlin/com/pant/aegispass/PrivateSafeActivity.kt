package com.pant.aegispass

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pant.aegispass.databinding.ActivityPrivateSafeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

// Enum for sorting options
enum class MediaSortOrder {
    NAME_ASC, NAME_DESC, DATE_NEWEST, DATE_OLDEST, SIZE_ASC, SIZE_DESC, TYPE
}

class PrivateSafeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivateSafeBinding
    private lateinit var adapter: PrivateMediaAdapter
    private val privateMediaList = mutableListOf<PrivateMediaItem>()

    private var pendingDeleteUri: Uri? = null
    private var isSelectionModeActive: Boolean = false
    private var currentSortOrder: MediaSortOrder = MediaSortOrder.DATE_NEWEST // Default sort order

    // Permission request codes
    private val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 100
    private val READ_MEDIA_PERMISSION_REQUEST_CODE = 101 // For Android 13+ (TIRAMISU)
    private val POST_NOTIFICATIONS_PERMISSION_REQUEST_CODE = 102 // For Android 13+
    private val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 103 // For Android 11+ (R)

    private var currentMoveJob: Job? = null // For moving media from public to private
    private var currentRemoveJob: Job? = null // For moving media from private to public (and deleting from private)
    private var currentDeleteJob: Job? = null // For permanent deletion from private safe
    private var progressDialog: ProgressDialog? = null

    private val aegisPassApplication: AegisPassApplication
        get() = application as AegisPassApplication

    // Correct way to get the private directory
    private fun getPrivateMediaDir(): File {
        return File(filesDir, "private_media").apply {
            if (!exists()) {
                mkdirs()
                File(this, ".nomedia").createNewFile() // Create .nomedia to hide from gallery
            }
        }
    }

    // Register ActivityResultLauncher for picking media
    @RequiresApi(Build.VERSION_CODES.Q)
    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            // Changed to MutableSet to automatically handle duplicates from selection
            val urisToMove = mutableSetOf<Uri>()

            if (data?.clipData != null) {
                for (i in 0 until data.clipData!!.itemCount) {
                    urisToMove.add(data.clipData!!.getItemAt(i).uri)
                }
            } else {
                data?.data?.let { uri ->
                    urisToMove.add(uri)
                }
            }

            if (urisToMove.isNotEmpty()) {
                // Ensure no other heavy operation is active
                if (currentMoveJob?.isActive == true || currentRemoveJob?.isActive == true || currentDeleteJob?.isActive == true) {
                    Toast.makeText(this, "Please wait for the current operation to finish.", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }

                currentMoveJob = lifecycleScope.launch {
                    showProgressDialog("Moving files to Private Safe...", urisToMove.size)
                    var successCount = 0
                    var failCount = 0

                    for ((index, uri) in urisToMove.withIndex()) {
                        try {
                            moveMediaToPrivateSafe(uri)
                            successCount++
                        } catch (e: Exception) {
                            Log.e("AegisPass", "Failed to move file to private safe: $uri", e)
                            failCount++
                        }
                        withContext(Dispatchers.Main) {
                            progressDialog?.progress = index + 1
                        }
                    }
                    hideProgressDialog()
                    if (successCount > 0) {
                        Toast.makeText(this@PrivateSafeActivity, "$successCount file(s) moved to Private Safe. $failCount failed.", Toast.LENGTH_LONG).show()
                    } else if (failCount > 0) {
                        Toast.makeText(this@PrivateSafeActivity, "Failed to move any files. Check logs.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@PrivateSafeActivity, "No files selected.", Toast.LENGTH_SHORT).show()
                    }
                    loadPrivateMedia() // Reload media after batch operation
                }
            }
        }
    }

    // Register ActivityResultLauncher for recoverable security exceptions (for public storage deletion)
    @RequiresApi(Build.VERSION_CODES.Q)
    private val recoverableSecurityLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteUri?.let { uri ->
                lifecycleScope.launch(Dispatchers.IO) {
                    performDeleteMediaFromPublicStorage(uri) // Re-attempt deletion after permission
                    pendingDeleteUri = null
                }
            }
        } else {
            Toast.makeText(this, "Permission to delete original file denied. It may still appear in public gallery.", Toast.LENGTH_LONG).show()
            pendingDeleteUri = null
        }
    }

    // Launcher for MANAGE_EXTERNAL_STORAGE permission
    private val manageExternalStorageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Manage External Storage Permission Granted", Toast.LENGTH_SHORT).show()
                // Proceed with picking media if permission is granted
                pickMediaFromGallery()
            } else {
                Toast.makeText(this, "Manage External Storage Permission Denied. Cannot access all files.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivateSafeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Private Safe"

        adapter = PrivateMediaAdapter(
            mediaList = privateMediaList,
            onItemClick = { item, position ->
                if (isSelectionModeActive) {
                    adapter.toggleSelection(item)
                } else {
                    if (item.isVideo) {
                        openVideoInExternalPlayer(item)
                    } else {
                        val intent = Intent(this, MediaViewerActivity::class.java)
                        // Filter for only images to pass to MediaViewerActivity
                        val imageList = ArrayList(privateMediaList.filter { !it.isVideo })
                        val imagePosition = imageList.indexOf(item)
                        if (imagePosition != -1) {
                            intent.putExtra("mediaList", imageList)
                            intent.putExtra("startPosition", imagePosition)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Image not found or not an image type.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onItemLongClick = { item, position ->
                if (!isSelectionModeActive) {
                    setupSelectionMode()
                }
                adapter.toggleSelection(item)
            },
            onSelectionChanged = { count ->
                updateToolbarForSelectionMode(count)
            }
        )

        binding.privateMediaRecyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.privateMediaRecyclerView.adapter = adapter

        binding.addMediaFab.setOnClickListener {
            // Prevent adding media if another operation is active
            if (currentMoveJob?.isActive == true || currentRemoveJob?.isActive == true || currentDeleteJob?.isActive == true) {
                Toast.makeText(this, "Please wait for the current operation to finish.", Toast.LENGTH_SHORT).show()
            } else if (isSelectionModeActive) {
                Toast.makeText(this, "Exit selection mode to add new media.", Toast.LENGTH_SHORT).show()
            } else {
                checkAndRequestPermissions()
            }
        }

        loadPrivateMedia()
        createNotificationChannel()
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
        // Cancel any ongoing jobs when activity is destroyed
        currentMoveJob?.cancel()
        currentRemoveJob?.cancel()
        currentDeleteJob?.cancel()
        hideProgressDialog() // Dismiss dialog if it's showing
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_private_safe, menu)
        updateToolbarForSelectionMode(adapter.getSelectedItems().size)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Prevent actions if another heavy operation is active
        if (currentMoveJob?.isActive == true || currentRemoveJob?.isActive == true || currentDeleteJob?.isActive == true) {
            Toast.makeText(this, "Please wait for the current operation to finish.", Toast.LENGTH_SHORT).show()
            return true // Consume the click
        }

        return when (item.itemId) {
            android.R.id.home -> {
                if (isSelectionModeActive) {
                    exitSelectionMode()
                } else {
                    onBackPressedDispatcher.onBackPressed()
                }
                true
            }
            R.id.action_select -> {
                setupSelectionMode()
                true
            }
            R.id.action_select_all -> {
                selectAllItems()
                true
            }
            R.id.action_remove_from_safe -> {
                showRemoveConfirmationDialog()
                true
            }
            R.id.action_permanent_delete -> {
                showPermanentDeleteConfirmationDialog()
                true
            }
            R.id.action_cancel_selection -> {
                exitSelectionMode()
                true
            }
            R.id.sort_by_name_asc -> {
                currentSortOrder = MediaSortOrder.NAME_ASC
                loadPrivateMedia()
                true
            }
            R.id.sort_by_name_desc -> {
                currentSortOrder = MediaSortOrder.NAME_DESC
                loadPrivateMedia()
                true
            }
            R.id.sort_by_date_newest -> {
                currentSortOrder = MediaSortOrder.DATE_NEWEST
                loadPrivateMedia()
                true
            }
            R.id.sort_by_date_oldest -> {
                currentSortOrder = MediaSortOrder.DATE_OLDEST
                loadPrivateMedia()
                true
            }
            R.id.sort_by_size_asc -> {
                currentSortOrder = MediaSortOrder.SIZE_ASC
                loadPrivateMedia()
                true
            }
            R.id.sort_by_size_desc -> {
                currentSortOrder = MediaSortOrder.SIZE_DESC
                loadPrivateMedia()
                true
            }
            R.id.sort_by_type -> {
                currentSortOrder = MediaSortOrder.TYPE
                loadPrivateMedia()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun setupSelectionMode() {
        isSelectionModeActive = true
        adapter.setSelectionMode(true)
        updateToolbarForSelectionMode(0)
        invalidateOptionsMenu()
        binding.addMediaFab.visibility = View.GONE
    }

    private fun exitSelectionMode() {
        isSelectionModeActive = false
        adapter.clearSelection()
        updateToolbarForSelectionMode(0)
        invalidateOptionsMenu()
        binding.addMediaFab.visibility = View.VISIBLE
    }

    private fun selectAllItems() {
        adapter.getSelectedItems().clear()
        adapter.getSelectedItems().addAll(privateMediaList)
        adapter.notifyDataSetChanged()
        updateToolbarForSelectionMode(adapter.getSelectedItems().size)
    }

    private fun updateToolbarForSelectionMode(selectedCount: Int) {
        val menu = binding.toolbar.menu
        val selectItem = menu.findItem(R.id.action_select)
        val selectAllItem = menu.findItem(R.id.action_select_all)
        val removeFromSafeItem = menu.findItem(R.id.action_remove_from_safe)
        val permanentDeleteItem = menu.findItem(R.id.action_permanent_delete) // New item
        val cancelSelectionItem = menu.findItem(R.id.action_cancel_selection)
        val sortItem = menu.findItem(R.id.action_sort) // Sort option

        if (isSelectionModeActive) {
            selectItem?.isVisible = false
            sortItem?.isVisible = false // Hide sort in selection mode

            selectAllItem?.isVisible = true
            removeFromSafeItem?.isVisible = true && selectedCount > 0
            permanentDeleteItem?.isVisible = true && selectedCount > 0 // Only enable if items are selected
            cancelSelectionItem?.isVisible = true
            supportActionBar?.title = "Selected: $selectedCount"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } else {
            selectItem?.isVisible = true
            sortItem?.isVisible = true // Show sort in normal mode

            selectAllItem?.isVisible = false
            removeFromSafeItem?.isVisible = false
            permanentDeleteItem?.isVisible = false
            cancelSelectionItem?.isVisible = false
            supportActionBar?.title = "Private Safe"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun showRemoveConfirmationDialog() {
        val selectedCount = adapter.getSelectedItems().size
        if (selectedCount == 0) {
            Toast.makeText(this, "No items selected to remove.", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Remove from Private Safe")
            .setMessage("Are you sure you want to remove $selectedCount selected item(s) from Private Safe and move them back to public gallery?")
            .setPositiveButton("REMOVE") { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch { // Call suspend function within coroutine scope
                    moveSelectedMediaToPublicStorage()
                }
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPermanentDeleteConfirmationDialog() {
        val selectedCount = adapter.getSelectedItems().size
        if (selectedCount == 0) {
            Toast.makeText(this, "No items selected for permanent deletion.", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Permanent Delete")
            .setMessage("WARNING: This will permanently delete $selectedCount selected item(s) from your device. This action cannot be undone. Are you sure you want to proceed?")
            .setPositiveButton("DELETE PERMANENTLY") { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch { // Call suspend function within coroutine scope
                    deleteSelectedMediaPermanently()
                }
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun showProgressDialog(message: String, maxProgress: Int = 0) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this).apply {
                setMessage(message)
                setCancelable(false) // Prevent dismissal by touching outside
                setProgressStyle(ProgressDialog.STYLE_HORIZONTAL) // Show a horizontal progress bar
                max = maxProgress
                progress = 0
            }
        } else {
            progressDialog?.setMessage(message)
            progressDialog?.max = maxProgress
            progressDialog?.progress = 0
        }
        progressDialog?.show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private suspend fun moveSelectedMediaToPublicStorage() {
        val selectedItems = adapter.getSelectedItems().toList()
        if (selectedItems.isEmpty()) return

        // Ensure no other heavy operation is active
        if (currentMoveJob?.isActive == true || currentRemoveJob?.isActive == true || currentDeleteJob?.isActive == true) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PrivateSafeActivity, "Please wait for the current operation to finish.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        currentRemoveJob = lifecycleScope.launch {
            showProgressDialog("Moving files to public gallery...", selectedItems.size)
            var successCount = 0
            var failCount = 0

            for ((index, item) in selectedItems.withIndex()) {
                try {
                    val privateFile = File(item.filePath)
                    if (privateFile.exists()) {
                        val publicUri = saveMediaToPublicStorage(privateFile, item.fileName, item.isVideo)
                        if (publicUri != null) {
                            deleteMediaFromPrivateSafe(item) // Delete private file after successful move
                            successCount++
                        } else {
                            failCount++
                            Log.e("PrivateSafe", "Failed to move ${item.fileName} to public gallery.")
                        }
                    } else {
                        Log.w("PrivateSafe", "Private file not found: ${item.filePath}")
                        failCount++
                    }
                } catch (e: Exception) {
                    Log.e("PrivateSafe", "Error moving ${item.fileName} to public storage: ${e.message}", e)
                    failCount++
                }
                withContext(Dispatchers.Main) {
                    progressDialog?.progress = index + 1
                }
            }

            hideProgressDialog()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PrivateSafeActivity,
                    "Moved $successCount item(s) to public gallery. Failed: $failCount.",
                    Toast.LENGTH_LONG).show()
                exitSelectionMode()
                loadPrivateMedia() // Reload media after batch operation
            }
        }
    }

    private suspend fun deleteSelectedMediaPermanently() {
        val selectedItems = adapter.getSelectedItems().toList()
        if (selectedItems.isEmpty()) return

        // Ensure no other heavy operation is active
        if (currentMoveJob?.isActive == true || currentRemoveJob?.isActive == true || currentDeleteJob?.isActive == true) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PrivateSafeActivity, "Please wait for the current operation to finish.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        currentDeleteJob = lifecycleScope.launch {
            showProgressDialog("Permanently deleting files...", selectedItems.size)
            var successCount = 0
            var failCount = 0

            for ((index, item) in selectedItems.withIndex()) {
                try {
                    val privateFile = File(item.filePath)
                    if (privateFile.exists()) {
                        if (privateFile.delete()) {
                            Log.d("PrivateSafe", "Permanently deleted: ${item.fileName}")
                            successCount++
                        } else {
                            Log.e("PrivateSafe", "Failed to permanently delete: ${item.fileName}")
                            failCount++
                        }
                    } else {
                        Log.w("PrivateSafe", "Private file not found for permanent deletion: ${item.filePath}")
                        // If file not found but was in our list, consider it a success if it's no longer there
                        successCount++
                    }
                } catch (e: Exception) {
                    Log.e("PrivateSafe", "Error permanently deleting ${item.fileName}: ${e.message}", e)
                    failCount++
                }
                withContext(Dispatchers.Main) {
                    progressDialog?.progress = index + 1
                }
            }

            hideProgressDialog()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PrivateSafeActivity,
                    "Permanently deleted $successCount item(s). Failed: $failCount.",
                    Toast.LENGTH_LONG).show()
                exitSelectionMode()
                loadPrivateMedia() // Reload media after batch operation
            }
        }
    }


    private suspend fun saveMediaToPublicStorage(file: File, fileName: String, isVideo: Boolean): Uri? {
        return withContext(Dispatchers.IO) { // Ensure this heavy operation is on IO thread
            val mimeType = getMimeType(fileName) ?: (if (isVideo) "video/*" else "image/*")

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.SIZE, file.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES)
                    put(MediaStore.MediaColumns.IS_PENDING, 1) // Mark as pending
                }
            }

            val collection = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            var uri: Uri? = null
            var outputStream: OutputStream? = null

            try {
                uri = contentResolver.insert(collection, contentValues)
                if (uri == null) {
                    Log.e("PrivateSafe", "Failed to insert new MediaStore item for $fileName. URI is null.")
                    return@withContext null
                }

                outputStream = contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    Log.e("PrivateSafe", "Failed to open output stream for URI: $uri")
                    contentResolver.delete(uri, null, null) // Clean up partial entry
                    return@withContext null
                }

                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0) // Unmark as pending
                    contentResolver.update(uri, contentValues, null, null)
                } else {
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = uri
                    sendBroadcast(mediaScanIntent)
                }
                return@withContext uri
            } catch (e: Exception) {
                Log.e("PrivateSafe", "Error saving media to public storage for $fileName: ${e.message}", e)
                uri?.let {
                    contentResolver.delete(it, null, null) // Clean up partial entry
                }
                return@withContext null
            } finally {
                outputStream?.close()
            }
        }
    }

    private fun getMimeType(fileName: String): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension?.lowercase())
    }

    private fun deleteMediaFromPrivateSafe(item: PrivateMediaItem) {
        val file = File(item.filePath)
        if (file.exists()) {
            if (file.delete()) {
                Log.d("PrivateSafe", "Deleted from private safe: ${item.fileName}")
            } else {
                Log.e("PrivateSafe", "Failed to delete from private safe: ${item.fileName}")
            }
        } else {
            Log.w("PrivateSafe", "Private file not found when trying to delete: ${item.filePath}")
        }
    }

    // Permission handling
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12 permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            // For older Android, WRITE_EXTERNAL_STORAGE is also needed for moving files to public
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // For Android 11 (R) and above, MANAGE_EXTERNAL_STORAGE is a special permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            // We don't add this to permissionsToRequest array because it's a special intent-based request
            // We will handle it separately if other permissions are granted or not needed.
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), READ_MEDIA_PERMISSION_REQUEST_CODE)
        } else {
            // All necessary permissions are already granted, proceed to check MANAGE_EXTERNAL_STORAGE if applicable
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                requestManageExternalStoragePermission()
            } else {
                // All permissions handled, proceed to pick media
                pickMediaFromGallery()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestManageExternalStoragePermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory("android.intent.category.DEFAULT")
            intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
            manageExternalStorageLauncher.launch(intent)
        } catch (e: Exception) {
            // Fallback for some devices where ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION might not work
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            manageExternalStorageLauncher.launch(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_MEDIA_PERMISSION_REQUEST_CODE -> {
                var allGranted = true
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                        break
                    }
                }
                if (allGranted) {
                    Toast.makeText(this, "All required media permissions granted.", Toast.LENGTH_SHORT).show()
                    // After granting, check MANAGE_EXTERNAL_STORAGE for Android 11+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        requestManageExternalStoragePermission()
                    } else {
                        pickMediaFromGallery()
                    }
                } else {
                    Toast.makeText(this, "Required media permissions denied. Cannot pick media files.", Toast.LENGTH_LONG).show()
                    // Optionally guide user to settings if permission is permanently denied
                }
            }
            POST_NOTIFICATIONS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification permission denied. Cannot show file move notifications.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun pickMediaFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "*/*" // Allow all media types
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")) // Explicitly specify image and video
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Allow multiple selections
        }
        pickMediaLauncher.launch(intent)
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun moveMediaToPrivateSafe(sourceUri: Uri) {
        val privateDir = getPrivateMediaDir()
        val fileName = getFileNameFromUri(sourceUri) ?: "unknown_file_${System.currentTimeMillis()}"
        val destinationFile = File(privateDir, fileName)

        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        // Delete original only after successful copy
        deleteMediaFromPublicStorage(sourceUri)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun deleteMediaFromPublicStorage(uri: Uri) {
        performDeleteMediaFromPublicStorage(uri)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun performDeleteMediaFromPublicStorage(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val rowsDeleted = contentResolver.delete(uri, null, null)
                if (rowsDeleted > 0) {
                    Log.d("AegisPass", "Original media deleted from public storage: $uri")
                } else {
                    Log.w("AegisPass", "No rows deleted for original media: $uri.")
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    val file = File(uri.path)
                    if (file.exists()) {
                        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        mediaScanIntent.data = Uri.fromFile(file)
                        sendBroadcast(mediaScanIntent)
                    }
                }
            } catch (e: RecoverableSecurityException) {
                Log.e("AegisPass", "RecoverableSecurityException during deletion: ${e.message}", e)
                pendingDeleteUri = uri
                withContext(Dispatchers.Main) {
                    val intentSenderRequest = IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
                    recoverableSecurityLauncher.launch(intentSenderRequest)
                }
            } catch (e: Exception) {
                Log.e("PrivateSafe", "Error deleting original media from public storage: ${e.message}", e)
                throw e
            }
        }
    }

    private fun loadPrivateMedia() {
        lifecycleScope.launch(Dispatchers.IO) {
            val privateDir = getPrivateMediaDir()
            val mediaFiles = privateDir.listFiles { file: File ->
                file.isFile && !file.name.startsWith(".")
            }

            val loadedMedia = mediaFiles?.map { file: File ->
                val isVideo = file.extension.equals("mp4", ignoreCase = true) ||
                        file.extension.equals("mov", ignoreCase = true) ||
                        file.extension.equals("avi", ignoreCase = true) ||
                        file.extension.equals("mkv", ignoreCase = true) ||
                        file.extension.equals("webm", ignoreCase = true)

                PrivateMediaItem(file.name, file.absolutePath, isVideo)
            }?.toMutableList() ?: mutableListOf()

            // Apply sorting based on currentSortOrder
            val sortedList = when (currentSortOrder) {
                MediaSortOrder.NAME_ASC -> loadedMedia.sortedBy { it.fileName.lowercase() }
                MediaSortOrder.NAME_DESC -> loadedMedia.sortedByDescending { it.fileName.lowercase() }
                MediaSortOrder.DATE_NEWEST -> loadedMedia.sortedByDescending { File(it.filePath).lastModified() }
                MediaSortOrder.DATE_OLDEST -> loadedMedia.sortedBy { File(it.filePath).lastModified() }
                MediaSortOrder.SIZE_ASC -> loadedMedia.sortedBy { File(it.filePath).length() }
                MediaSortOrder.SIZE_DESC -> loadedMedia.sortedByDescending { File(it.filePath).length() }
                MediaSortOrder.TYPE -> loadedMedia.sortedWith(compareBy<PrivateMediaItem> { if (it.isVideo) 1 else 0 }.thenBy { it.fileName.lowercase() })
            }

            withContext(Dispatchers.Main) {
                privateMediaList.clear()
                privateMediaList.addAll(sortedList) // Add sorted list
                adapter.notifyDataSetChanged()
                updateNoMediaMessage()
            }
        }
    }

    private fun updateNoMediaMessage() {
        if (privateMediaList.isEmpty()) {
            binding.noMediaMessage.visibility = View.VISIBLE
            binding.privateMediaRecyclerView.visibility = View.GONE
        } else {
            binding.noMediaMessage.visibility = View.GONE
            binding.privateMediaRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun openVideoInExternalPlayer(item: PrivateMediaItem) {
        val videoFile = File(item.filePath)
        if (!videoFile.exists()) {
            Toast.makeText(this, "Video file not found.", Toast.LENGTH_SHORT).show()
            Log.e("AegisPass", "Video file not found at path: ${item.filePath}")
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".fileprovider",
                videoFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(intent, "Open video with"))
        } catch (e: Exception) {
            Log.e("AegisPass", "Error opening video in external player: ${e.message}", e)
            Toast.makeText(this, "Could not open video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val CHANNEL_ID = "private_safe_notifications"
    private val NOTIFICATION_ID = 1

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Private Safe Notifications"
            val descriptionText = "Notifications for media moved to Private Safe"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
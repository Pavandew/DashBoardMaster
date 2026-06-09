package com.example.masterdashboard.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentUploadManager(
    private val fragment: Fragment
) {
    companion object{
        private const val TAG = "DocumentUploadManager"
    }

    private val context: Context = fragment.requireContext()
    private var activeSelectionCallback: ((Uri) -> Unit) ? = null
    private var tempCameraUri: Uri? = null

    // 1. Launcher for System File Picker
    private val filePickerLauncher: ActivityResultLauncher<String> =
    fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { activeSelectionCallback?.invoke(it) }
    }

    //2. Launcher for Camera App Capture
    private val cameraLauncher: ActivityResultLauncher<Uri> =
        fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->

            if(success) {
                tempCameraUri?.let { activeSelectionCallback?.invoke(it) }
            } else {
                Log.w(TAG, "Camera capture cancelled or failed.")
            }
        }

    // 3. Launcher for Requesting Runtime Permission
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

            val cameraGranted = permission[Manifest.permission.CAMERA] ?: false

            // On API 33+ READ_EXTERNAL_STORAGE is not needed for implicit pickers, so default to true if on newer versions
            val storageGranted = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                true
            } else {
                permission[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }

            if(cameraGranted && storageGranted) {
                showUploadOptionDialog()
            } else {
                Toast.makeText(context, "Permission denied. Cannot access source inputs.", Toast.LENGTH_LONG).show()
            }
        }

    // Public entry point called by Adapter/fragment rows
    fun selectDocument(onFileSelected:(Uri) -> Unit) {
        this.activeSelectionCallback = onFileSelected

        if(checkHasPermission()) {
            showUploadOptionDialog()
        } else {
            requestRequiredPermission()
        }
    }

    private fun checkHasPermission(): Boolean {
        val cameraCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val storageCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true // API 33+ handles safe background read queries automatically via standard contracts
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        return cameraCheck && storageCheck
    }

    private fun requestRequiredPermission() {
        val permissionArray = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA)
        } else  {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissionArray)
    }

    private fun showUploadOptionDialog() {
        val options = arrayOf("Take Photo (Camera)", "Choose File(Gallery/Storage)")

        MaterialAlertDialogBuilder(context)
            .setTitle("Upload Document")
            .setItems(options) { dialog, which ->
                when(which) {
                    0 -> launchCameraEngine()
                    1 -> filePickerLauncher.launch("image/*")  // Standard file picker intent trigger
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun launchCameraEngine() {
        val tempFile = createTemporaryImageFile()
        if(tempFile != null) {
            // Generate secure content authority match string provider link
            val authority = "${context.packageName}.fileprovider"
            tempCameraUri = FileProvider.getUriForFile(context, authority, tempFile)

            tempCameraUri?.let { cameraLauncher.launch(it) }
        } else {
            Toast.makeText(context, "Failed to initialize storage for image capture", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTemporaryImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating temporary cache template file assignment block matching.", e)
            null
        }
    }


}
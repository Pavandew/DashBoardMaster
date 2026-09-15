package com.example.masterdashboard.manager_single_res_dash.settings

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.BottomSheetUpiPaymentSettingsBinding
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UpiPaymentSettingsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "UpiPaymentSettingsBottomSheet"
    }

    private var _binding: BottomSheetUpiPaymentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private val storage = FirebaseStorage.getInstance("gs://masterdashboard-836dd.firebasestorage.app")

    private var selectedImageUri: Uri? = null
    private var currentQrUrl: String = ""

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Log.d(TAG, "Image selected from gallery: $it")
            binding.ivQrPreview.load(it) {
                crossfade(true)
                placeholder(R.drawable.ic_upi_pay_24dp)
                error(R.drawable.ic_upi_pay_24dp)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUpiPaymentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        loadExistingUpiDetails()

        binding.btnSelectQrImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnSaveUpi.setOnClickListener {
            saveUpiSettings()
        }
    }

    private fun loadExistingUpiDetails() {
        val existingUpiId = sessionManager.getUpiId()
        currentQrUrl = sessionManager.getUpiQrUrl()

        if (existingUpiId.isNotEmpty()) {
            binding.etUpiId.setText(existingUpiId)
        }

        if (currentQrUrl.isNotEmpty()) {
            binding.ivQrPreview.load(currentQrUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_upi_pay_24dp)
                error(R.drawable.ic_upi_pay_24dp)
            }
        }
    }

    private fun saveUpiSettings() {
        val upiId = binding.etUpiId.text?.toString()?.trim() ?: ""
        val ownerUid = sessionManager.getUid()

        if (ownerUid.isEmpty()) {
            Toast.makeText(requireContext(), "User session not found", Toast.LENGTH_SHORT).show()
            return
        }

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                var updatedQrUrl = currentQrUrl

                if (selectedImageUri != null) {
                    Log.d(TAG, "Uploading new QR image to Firebase Storage...")
                    val storageRef = storage.reference.child("upi_qr/$ownerUid.jpg")
                    storageRef.putFile(selectedImageUri!!).await()
                    updatedQrUrl = storageRef.downloadUrl.await().toString()
                    Log.i(TAG, "Uploaded QR image URL: $updatedQrUrl")
                }

                val updates = mapOf(
                    "upiId" to upiId,
                    "upiQrUrl" to updatedQrUrl
                )
                val docRef = RestaurantPathHelper.getRestaurantDocRef(ownerUid)
                docRef.set(updates, SetOptions.merge()).await()

                sessionManager.saveUpiDetails(upiId, updatedQrUrl)

                Toast.makeText(requireContext(), "UPI Settings saved successfully!", Toast.LENGTH_SHORT).show()
                dismiss()

            } catch (e: Exception) {
                Log.e(TAG, "Error saving UPI settings", e)
                Toast.makeText(requireContext(), "Failed to save: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoadingState(false)
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBarUpi.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSaveUpi.isEnabled = !isLoading
        binding.btnSelectQrImage.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

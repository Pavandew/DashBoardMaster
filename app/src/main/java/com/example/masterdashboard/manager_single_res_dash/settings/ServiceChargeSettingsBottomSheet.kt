package com.example.masterdashboard.manager_single_res_dash.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.BottomSheetServiceChargeSettingsBinding
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ServiceChargeSettingsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ServiceChargeSettingsBottomSheet"
    }

    private var _binding: BottomSheetServiceChargeSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetServiceChargeSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupUI()
        loadExistingSettings()
    }

    private fun setupUI() {
        binding.cardSwitchServiceCharge.tvSwitchTitle.text = getString(R.string.title_enable_service_charge)
        binding.cardSwitchServiceCharge.tvSwitchSubtitle.text = getString(R.string.subtitle_enable_service_charge)

        binding.cardSwitchDineInOnly.tvSwitchTitle.text = getString(R.string.title_dine_in_only)
        binding.cardSwitchDineInOnly.tvSwitchSubtitle.text = getString(R.string.subtitle_dine_in_only)

        binding.cardSwitchApplyTax.tvSwitchTitle.text = getString(R.string.title_calculate_tax_sc)
        binding.cardSwitchApplyTax.tvSwitchSubtitle.text = getString(R.string.subtitle_calculate_tax_sc)

        // Toggle configuration panel visibility based on master switch
        binding.cardSwitchServiceCharge.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            binding.llServiceChargeConfig.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Preset Percentage Chips Click Listeners
        binding.chip5.setOnClickListener { binding.etServiceChargePercent.setText("5") }
        binding.chip75.setOnClickListener { binding.etServiceChargePercent.setText("7.5") }
        binding.chip10.setOnClickListener { binding.etServiceChargePercent.setText("10") }
        binding.chip125.setOnClickListener { binding.etServiceChargePercent.setText("12.5") }

        binding.btnSaveServiceCharge.setOnClickListener {
            saveServiceChargeRules()
        }
    }

    private fun loadExistingSettings() {
        val isEnabled = sessionManager.isServiceChargeEnabled()
        val percent = sessionManager.getServiceChargePercent()
        val label = sessionManager.getServiceChargeLabel()
        val isDineInOnly = sessionManager.isServiceChargeDineInOnly()
        val isApplyTax = sessionManager.isServiceChargeApplyTax()

        binding.cardSwitchServiceCharge.switchMaster.isChecked = isEnabled
        binding.llServiceChargeConfig.visibility = if (isEnabled) View.VISIBLE else View.GONE

        binding.etServiceChargePercent.setText(if (percent > 0) percent.toString() else "5")
        binding.etServiceChargeLabel.setText(label.ifEmpty { "Service Charge" })

        binding.cardSwitchDineInOnly.switchMaster.isChecked = isDineInOnly
        binding.cardSwitchApplyTax.switchMaster.isChecked = isApplyTax
    }

    private fun saveServiceChargeRules() {
        val isEnabled = binding.cardSwitchServiceCharge.switchMaster.isChecked
        val percentStr = binding.etServiceChargePercent.text?.toString()?.trim() ?: ""
        val percent = percentStr.toDoubleOrNull() ?: 0.0
        var label = binding.etServiceChargeLabel.text?.toString()?.trim() ?: ""
        val isDineInOnly = binding.cardSwitchDineInOnly.switchMaster.isChecked
        val isApplyTax = binding.cardSwitchApplyTax.switchMaster.isChecked

        if (isEnabled && percent <= 0.0) {
            Toast.makeText(requireContext(), "Please enter a valid rate percentage greater than 0", Toast.LENGTH_SHORT).show()
            return
        }

        if (label.isEmpty()) {
            label = "Service Charge"
        }

        val ownerUid = sessionManager.getUid()
        if (ownerUid.isEmpty()) {
            Toast.makeText(requireContext(), "User session not found", Toast.LENGTH_SHORT).show()
            return
        }

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Saving Service Charge Rules to Firestore: enabled=$isEnabled, rate=$percent%, label=$label")

                val serviceChargeUpdates = mapOf(
                    "serviceChargeEnabled" to isEnabled,
                    "serviceChargePercent" to percent,
                    "serviceChargeLabel" to label,
                    "serviceChargeDineInOnly" to isDineInOnly,
                    "serviceChargeApplyTax" to isApplyTax
                )

                // 1. Update Firestore user document
                firestore.collection(AppConstants.COLLECTION_USERS)
                    .document(ownerUid)
                    .update(serviceChargeUpdates)
                    .await()

                // 2. Update local SessionManager cache
                sessionManager.saveServiceChargeSettings(
                    enabled = isEnabled,
                    percent = percent,
                    label = label,
                    dineInOnly = isDineInOnly,
                    applyTax = isApplyTax
                )

                Toast.makeText(requireContext(), "Service Charge rules updated successfully!", Toast.LENGTH_SHORT).show()
                dismiss()

            } catch (e: Exception) {
                Log.e(TAG, "Error saving Service Charge rules", e)
                Toast.makeText(requireContext(), "Failed to save rules: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoadingState(false)
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBarServiceCharge.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSaveServiceCharge.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

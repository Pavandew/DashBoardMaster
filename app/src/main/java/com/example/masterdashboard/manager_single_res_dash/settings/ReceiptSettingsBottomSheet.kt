package com.example.masterdashboard.manager_single_res_dash.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.BottomSheetReceiptSettingsBinding
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.BillingPrinterSettings
import com.example.masterdashboard.manager_single_res_dash.settings.viewModel.ReceiptSettingsViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ReceiptSettingsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ReceiptSettingsBottomSheet"
    }

    private var _binding: BottomSheetReceiptSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private val viewModel: ReceiptSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReceiptSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupUI()
        observeViewModel()

        val ownerUid = sessionManager.getUid()
        if (ownerUid.isNotEmpty()) {
            viewModel.loadReceiptSettings(ownerUid)
        } else {
            loadFallbackSettings()
        }
    }

    private fun setupUI() {
        binding.cardSwitchShowLogo.tvSwitchTitle.text = getString(R.string.title_show_logo)
        binding.cardSwitchShowLogo.tvSwitchSubtitle.text = getString(R.string.subtitle_show_logo)

        binding.cardSwitchShowGstin.tvSwitchTitle.text = "Show GSTIN Number"
        binding.cardSwitchShowGstin.tvSwitchSubtitle.text = "Display GST registration number on receipts"

        binding.cardSwitchShowFssai.tvSwitchTitle.text = "Show FSSAI License"
        binding.cardSwitchShowFssai.tvSwitchSubtitle.text = "Display FSSAI food license number on receipts"

        binding.cardSwitchShowCustomerInfo.tvSwitchTitle.text = "Show Customer Details"
        binding.cardSwitchShowCustomerInfo.tvSwitchSubtitle.text = "Display customer name and phone on receipts"

        binding.btnSaveReceiptSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.receiptSettings.collect { settings ->
                        populateUI(settings)
                    }
                }

                launch {
                    viewModel.saveState.collect { state ->
                        when (state) {
                            is ResourceUiState.Loading -> setLoadingState(true)
                            is ResourceUiState.Success -> {
                                setLoadingState(false)
                                Toast.makeText(requireContext(), state.data, Toast.LENGTH_SHORT).show()
                                viewModel.resetSaveState()
                                dismiss()
                            }
                            is ResourceUiState.Error -> {
                                setLoadingState(false)
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetSaveState()
                            }
                            else -> setLoadingState(false)
                        }
                    }
                }
            }
        }
    }

    private fun populateUI(settings: BillingPrinterSettings) {
        binding.etInvoicePrefix.setText(settings.invoicePrefix.ifEmpty { "INV-" })
        binding.etInvoiceNumber.setText(settings.startingInvoiceNumber.ifEmpty { "1" })
        binding.etHeaderTagline.setText(settings.customHeaderTagline)
        binding.etFooterMessage.setText(settings.customFooterMessage.ifEmpty { "Thank You Visit Again" })

        binding.cardSwitchShowLogo.switchMaster.isChecked = settings.showLogoOnReceipts
        binding.cardSwitchShowGstin.switchMaster.isChecked = settings.showGstin
        binding.cardSwitchShowFssai.switchMaster.isChecked = settings.showFssai
        binding.cardSwitchShowCustomerInfo.switchMaster.isChecked = settings.showCustomerInfo

        when {
            settings.printSize.contains("58", ignoreCase = true) -> binding.chip58mm.isChecked = true
            settings.printSize.contains("A4", ignoreCase = true) -> binding.chipA4.isChecked = true
            else -> binding.chip80mm.isChecked = true
        }
    }

    private fun loadFallbackSettings() {
        val cachedDetails = sessionManager.getCachedRestaurantDetails()
        val settings = cachedDetails?.getEffectiveBillingPrinterSettings() ?: BillingPrinterSettings()
        populateUI(settings)
    }

    private fun saveSettings() {
        val invoicePrefix = binding.etInvoicePrefix.text?.toString()?.trim() ?: "INV-"
        val startingNumber = binding.etInvoiceNumber.text?.toString()?.trim() ?: "1"
        val headerTagline = binding.etHeaderTagline.text?.toString()?.trim() ?: ""
        val footerMessage = binding.etFooterMessage.text?.toString()?.trim() ?: "Thank You Visit Again"

        val showLogo = binding.cardSwitchShowLogo.switchMaster.isChecked
        val showGstin = binding.cardSwitchShowGstin.switchMaster.isChecked
        val showFssai = binding.cardSwitchShowFssai.switchMaster.isChecked
        val showCustomerInfo = binding.cardSwitchShowCustomerInfo.switchMaster.isChecked

        val printSize = when {
            binding.chip58mm.isChecked -> "58 mm thermal"
            binding.chipA4.isChecked -> "A4 standard"
            else -> "80 mm thermal"
        }

        val ownerUid = sessionManager.getUid()
        val currentSettings = viewModel.receiptSettings.value
        val newSettings = currentSettings.copy(
            invoicePrefix = invoicePrefix.ifEmpty { "INV-" },
            startingInvoiceNumber = startingNumber.ifEmpty { "1" },
            printSize = printSize,
            showLogoOnReceipts = showLogo,
            customHeaderTagline = headerTagline,
            customFooterMessage = footerMessage.ifEmpty { "Thank You Visit Again" },
            showGstin = showGstin,
            showFssai = showFssai,
            showCustomerInfo = showCustomerInfo
        )

        Log.d(TAG, "Initiating Receipt Settings save via ViewModel...")
        viewModel.saveReceiptSettings(ownerUid, newSettings, sessionManager)
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBarReceiptSettings.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSaveReceiptSettings.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

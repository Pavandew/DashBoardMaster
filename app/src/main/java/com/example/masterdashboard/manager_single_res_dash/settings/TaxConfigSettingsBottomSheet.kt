package com.example.masterdashboard.manager_single_res_dash.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.masterdashboard.databinding.BottomSheetTaxConfigSettingsBinding
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.TaxSettings
import com.example.masterdashboard.manager_single_res_dash.settings.viewModel.TaxSettingsViewModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.util.Locale

class TaxConfigSettingsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "TaxConfigSettingsBottomSheet"
    }

    private var _binding: BottomSheetTaxConfigSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private val viewModel: TaxSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTaxConfigSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupUI()
        observeViewModel()

        val ownerUid = sessionManager.getUid()
        if (ownerUid.isNotEmpty()) {
            viewModel.loadTaxSettings(ownerUid, sessionManager)
        } else {
            loadFallbackSettings()
        }
    }

    private fun setupUI() {
        binding.cardSwitchTax.tvSwitchTitle.text = getString(R.string.title_charge_tax)
        binding.cardSwitchTax.tvSwitchSubtitle.text = getString(R.string.subtitle_charge_tax)

        binding.cardSwitchPriceIncludesTax.tvSwitchTitle.text = getString(R.string.title_price_includes_tax)
        binding.cardSwitchPriceIncludesTax.tvSwitchSubtitle.text = getString(R.string.subtitle_price_includes_tax)

        // Toggle configuration panel visibility based on master switch
        binding.cardSwitchTax.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            binding.llTaxConfigPanel.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Preset Rate Chips Click Listeners
        binding.chipRate5.setOnClickListener { binding.etTaxRate.setText("5.0") }
        binding.chipRate12.setOnClickListener { binding.etTaxRate.setText("12.0") }
        binding.chipRate18.setOnClickListener { binding.etTaxRate.setText("18.0") }

        // Live Tax Explanation Listener
        binding.etTaxRate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateTaxExplanation()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.cgTaxLabel.setOnCheckedStateChangeListener { _, _ ->
            updateTaxExplanation()
        }

        binding.btnSaveTaxConfig.setOnClickListener {
            saveTaxSettings()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe loaded Tax Settings
                launch {
                    viewModel.taxSettings.collect { settings ->
                        populateUI(settings)
                    }
                }

                // Observe Save State
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

    private fun populateUI(settings: TaxSettings) {
        binding.cardSwitchTax.switchMaster.isChecked = settings.chargeTaxOnBills
        binding.llTaxConfigPanel.visibility = if (settings.chargeTaxOnBills) View.VISIBLE else View.GONE

        binding.etGstNumber.setText(settings.gstNumber)
        binding.etTaxRate.setText(if (settings.defaultTaxRate > 0) settings.defaultTaxRate.toString() else "5.0")
        binding.cardSwitchPriceIncludesTax.switchMaster.isChecked = settings.priceIncludesTax

        when (settings.taxLabel.uppercase(Locale.US)) {
            "VAT" -> binding.chipVat.isChecked = true
            "SALES TAX" -> binding.chipSalesTax.isChecked = true
            else -> binding.chipGst.isChecked = true
        }

        updateTaxExplanation()
    }

    private fun loadFallbackSettings() {
        val isTaxEnabled = sessionManager.isTaxEnabled()
        val rate = sessionManager.getGstRate()
        val label = sessionManager.getTaxLabel()
        val isPriceInclusive = sessionManager.isPriceIncludesTax()
        val cachedDetails = sessionManager.getCachedRestaurantDetails()

        val settings = TaxSettings(
            chargeTaxOnBills = isTaxEnabled,
            defaultTaxRate = rate,
            gstNumber = cachedDetails?.gstNumber ?: "",
            taxLabel = label,
            priceIncludesTax = isPriceInclusive
        )
        populateUI(settings)
    }

    private fun updateTaxExplanation() {
        val rateStr = binding.etTaxRate.text?.toString()?.trim() ?: "5.0"
        val rate = rateStr.toDoubleOrNull() ?: 5.0

        if (binding.chipGst.isChecked) {
            val halfRate = rate / 2.0
            binding.tvTaxExplanation.text = getString(R.string.tax_explanation_gst_format, rate, halfRate, halfRate)
        } else if (binding.chipVat.isChecked) {
            binding.tvTaxExplanation.text = getString(R.string.tax_explanation_vat_format, rate)
        } else {
            binding.tvTaxExplanation.text = getString(R.string.tax_explanation_sales_tax_format, rate)
        }
    }

    private fun saveTaxSettings() {
        val isTaxEnabled = binding.cardSwitchTax.switchMaster.isChecked
        val rateStr = binding.etTaxRate.text?.toString()?.trim() ?: ""
        val rate = rateStr.toDoubleOrNull() ?: 0.0
        val gstNumber = binding.etGstNumber.text?.toString()?.trim()?.uppercase(Locale.US) ?: ""
        val isPriceInclusive = binding.cardSwitchPriceIncludesTax.switchMaster.isChecked

        val taxLabel = when {
            binding.chipVat.isChecked -> "VAT"
            binding.chipSalesTax.isChecked -> "Sales Tax"
            else -> "GST"
        }

        if (isTaxEnabled && rate < 0.0) {
            Toast.makeText(requireContext(), "Please enter a valid tax rate percentage", Toast.LENGTH_SHORT).show()
            return
        }

        val ownerUid = sessionManager.getUid()
        val newSettings = TaxSettings(
            chargeTaxOnBills = isTaxEnabled,
            defaultTaxRate = rate,
            gstNumber = gstNumber,
            taxLabel = taxLabel,
            priceIncludesTax = isPriceInclusive
        )

        Log.d(TAG, "Initiating Tax Settings save via ViewModel...")
        viewModel.saveTaxSettings(ownerUid, newSettings, sessionManager)
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBarTaxConfig.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSaveTaxConfig.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

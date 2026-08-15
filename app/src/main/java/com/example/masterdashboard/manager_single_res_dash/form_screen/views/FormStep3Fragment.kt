package com.example.masterdashboard.manager_single_res_dash.form_screen.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentFormStep3Binding
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.manager_single_res_dash.form_screen.adapter.FormAdapter
import com.example.masterdashboard.manager_single_res_dash.form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.RegistrationDataViewModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.Step3ViewModel
import kotlinx.coroutines.launch

class FormStep3Fragment : Fragment() {

    private var _binding: FragmentFormStep3Binding? = null
    private val binding get() = _binding!!

    // Scoped to Activity (Bank)
    private val dataViewModel: RegistrationDataViewModel by activityViewModels()

    // Scoped to Fragment (Worker)
    private val stepViewModel: Step3ViewModel by viewModels()

    private var formAdapter: FormAdapter? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("FormStep3Fragment", "Navigation: Step 3 (Tax & Compliance) Opened")
        sessionManager = SessionManager(requireContext())
        
        stepViewModel.initFields(dataViewModel.registrationData)
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvFormStep3.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFormStep3.setHasFixedSize(true)

        // CRITICAL FIX: Attach adapter if it already exists (back navigation)
        formAdapter?.let { binding.rvFormStep3.adapter = it }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            Log.d("FormStep3Fragment", "Action: Back button clicked")
            parentFragmentManager.popBackStack()
        }

        binding.btnContinue.setOnClickListener {
            Log.i("FormStep3Fragment", "Action: Continue (Tax & Compliance) button clicked")
            stepViewModel.validate()
        }
    }

    private fun observeViewModel() {
        // 1. Observe UI State
        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.uiState.collect { state ->
                when (state) {
                    is RegistrationUiState.Success -> {
                        Log.i("FormStep3Fragment", "Step 3 Validation Passed. Ready for Step 4.")
                        stepViewModel.setIdle()

                        // Save local draft
                        sessionManager.saveRegistrationDraft(dataViewModel.registrationData)
                        dataViewModel.logCurrentData()

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.single_owner_fragmentContainer, FormStep4Fragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    is RegistrationUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        binding.rvFormStep3.smoothScrollToPosition(0)
                    }
                    else -> {}
                }
            }
        }

        // 2. Observe Form Fields
        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.formFields.collect { fields ->
                if (formAdapter == null) {
                    formAdapter = FormAdapter(fields) { key, value ->
                        updateBank(key, value)
                    }
                    binding.rvFormStep3.adapter = formAdapter
                } else {
                    formAdapter?.updateData(fields)
                }
            }
        }
    }

    /**
     * Updates the central "Bank" whenever the user types or toggles a switch in Step 3.
     */
    private fun updateBank(key: String, value: Any) {
        val data = dataViewModel.registrationData
        when (key) {
            "gst_number" -> data.gstNumber = value as String
            "pan_number" -> data.panNumber = value as String
            "charge_tax" -> data.chargeTaxOnBills = value as Boolean
            "tax_rate" -> data.defaultTaxRate = value as String
            "includes_tax" -> data.priceIncludesTax = value as Boolean
            "fssai_number" -> data.fssaiNumber = value as String
            "fssai_expiry" -> data.fssaiExpiryDate = value as String
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

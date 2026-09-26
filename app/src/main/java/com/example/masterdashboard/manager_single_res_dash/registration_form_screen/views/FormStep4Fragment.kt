package com.example.masterdashboard.manager_single_res_dash.registration_form_screen.views

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
import com.example.masterdashboard.databinding.FragmentFormStep4Binding
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.adapter.FormAdapter
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.viewModel.RegistrationDataViewModel
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.viewModel.Step4ViewModel
import com.example.masterdashboard.utils.DocumentUploadManager
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

class FormStep4Fragment : Fragment() {

    private var _binding: FragmentFormStep4Binding? = null
    private val binding get() = _binding!!

    private val dataViewModel: RegistrationDataViewModel by activityViewModels()
    private val stepViewModel: Step4ViewModel by viewModels()

    private var formAdapter: FormAdapter? = null
    private lateinit var sessionManager: SessionManager

    private val documentUploadManager by lazy {
        DocumentUploadManager(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormStep4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("FormStep4Fragment", "Navigation: Step 4 Screen Opened")

        sessionManager = SessionManager(requireContext())

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        stepViewModel.initFields(dataViewModel.registrationData)
    }

    private fun setupRecyclerView() {
        binding.rvFormStep4.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            Log.d("FormStep4Fragment", "Action: Back button clicked")
            parentFragmentManager.popBackStack()
        }

        binding.btnContinue.setOnClickListener {
            Log.i("FormStep4Fragment", "Action: Continue (Branding & Operations) button clicked")
            stepViewModel.validate()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.uiState.collect { state ->
                when (state) {
                    is RegistrationUiState.Success -> {
                        Log.i("FormStep4Fragment", "Step 4 Validation Passed. Navigating to Step 5.")
                        stepViewModel.setIdle()
                        sessionManager.saveRegistrationDraft(dataViewModel.registrationData)
                        
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.single_owner_fragmentContainer, FormStep5Fragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    is RegistrationUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        binding.rvFormStep4.smoothScrollToPosition(0)
                    }
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.formFields.collect { fields ->
                if (formAdapter == null) {
                    formAdapter = FormAdapter(fields) { key, value ->
                        updateData(key, value)
                    }
                    binding.rvFormStep4.adapter = formAdapter
                } else {
                    formAdapter?.updateData(fields)
                }
            }
        }
    }

    private fun updateData(key: String, value: Any) {
        val data = dataViewModel.registrationData
        when (key) {
            "logo" -> {
                Log.d("FormStep4Fragment", "Logo upload button clicked. Launching DocumentUploadManager...")
                documentUploadManager.selectDocument { uri ->
                    Log.i("FormStep4Fragment", "Logo image selected: $uri")
                    data.restaurantLogoUri = uri.toString()
                    data.billingPrinterSettings.restaurantLogoUri = uri.toString()
                    stepViewModel.initFields(data)
                    Toast.makeText(requireContext(), "Logo attached successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            "show_logo" -> {
                val isChecked = value as Boolean
                data.showLogoOnReceipts = isChecked
                data.billingPrinterSettings.showLogoOnReceipts = isChecked
            }
            "seating" -> {
                val seating = value as String
                data.seatingCapacity = seating
                data.restaurantProfile.seatingCapacity = seating
            }
            "open_days" -> {
                val openDays = value as String
                data.openDays = openDays
                data.restaurantProfile.openDays = openDays
            }
            "timezone" -> {
                val timezone = value as String
                data.timezone = timezone
                data.restaurantProfile.timezone = timezone
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

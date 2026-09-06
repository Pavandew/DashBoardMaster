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
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

class FormStep4Fragment : Fragment() {

    private var _binding: FragmentFormStep4Binding? = null
    private val binding get() = _binding!!

    private val dataViewModel: RegistrationDataViewModel by activityViewModels()
    private val stepViewModel: Step4ViewModel by viewModels()

    private var formAdapter: FormAdapter? = null
    private lateinit var sessionManager: SessionManager

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
                data.restaurantLogoUri = "logo_uploaded"
                Toast.makeText(requireContext(), "Logo Upload Simulated", Toast.LENGTH_SHORT).show()
                stepViewModel.initFields(data)
            }
            "show_logo" -> data.showLogoOnReceipts = value as Boolean
            "seating" -> data.seatingCapacity = value as String
            "open_days" -> data.openDays = value as String
            "timezone" -> data.timezone = value as String
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

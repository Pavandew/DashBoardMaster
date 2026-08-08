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
import com.example.masterdashboard.databinding.FragmentFormStep5Binding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.manager_single_res_dash.form_screen.adapter.FormAdapter
import com.example.masterdashboard.manager_single_res_dash.form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.RegistrationDataViewModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.Step5ViewModel
import kotlinx.coroutines.launch

class FormStep5Fragment : Fragment() {

    private var _binding: FragmentFormStep5Binding? = null
    private val binding get() = _binding!!

    private val dataViewModel: RegistrationDataViewModel by activityViewModels()
    private val stepViewModel: Step5ViewModel by viewModels()

    private var formAdapter: FormAdapter? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormStep5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("FormStep5Fragment", "Navigation: FormStep 5 Screen Opened")
        sessionManager = SessionManager(requireContext())
        
        stepViewModel.initFields(dataViewModel.registrationData)
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvFormStep5.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFormStep5.setHasFixedSize(true)
        formAdapter?.let { binding.rvFormStep5.adapter = it }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            Log.d("FormStep5Fragment", "Action: Back button clicked")
            parentFragmentManager.popBackStack()
        }

        binding.btnContinue.setOnClickListener {
            Log.i("FormStep5Fragment", "Action: Continue (Branding) button clicked")
            stepViewModel.validate()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.uiState.collect { state ->
                when (state) {
                    is RegistrationUiState.Success -> {
                        Log.i("FormStep5Fragment", "Step 5 Validation Passed. Navigating to Step 6.")
                        stepViewModel.setIdle()
                        sessionManager.saveRegistrationDraft(dataViewModel.registrationData)
                        
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.single_owner_fragmentContainer, FormStep6Fragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    is RegistrationUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        binding.rvFormStep5.smoothScrollToPosition(0)
                    }
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.formFields.collect { fields ->
                if (formAdapter == null) {
                    formAdapter = FormAdapter(fields) { key, value ->
                        updateBank(key, value)
                    }
                    binding.rvFormStep5.adapter = formAdapter
                } else {
                    formAdapter?.updateData(fields)
                }
            }
        }
    }

    private fun updateBank(key: String, value: Any) {
        val data = dataViewModel.registrationData
        when (key) {
            "logo" -> {
                data.restaurantLogoUri = "logo_uploaded" 
                Toast.makeText(requireContext(), "Logo Upload Simulated", Toast.LENGTH_SHORT).show()
                stepViewModel.initFields(data)
            }
            "show_logo" -> data.showLogoOnReceipts = value as Boolean
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

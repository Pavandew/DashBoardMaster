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
import com.example.masterdashboard.databinding.FragmentFormStep1Binding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.manager_single_res_dash.form_screen.adapter.FormAdapter
import com.example.masterdashboard.manager_single_res_dash.form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.RegistrationDataViewModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.Step1ViewModel
import kotlinx.coroutines.launch

class FormStep1Fragment : Fragment() {

    private var _binding: FragmentFormStep1Binding? = null
    private val binding get() = _binding!!

    // Scoped to Activity: Acts as our central "Bank"
    private val dataViewModel: RegistrationDataViewModel by activityViewModels()
    
    // Scoped to Fragment: Acts as our "Worker" for Step 1 logic
    private val stepViewModel: Step1ViewModel by viewModels()

    private var formAdapter: FormAdapter? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("FormStep1Fragment", "Navigation: Step 1 (Owner & Restaurant) Opened")
        sessionManager = SessionManager(requireContext())
        
        // Initialize fields with data currently in the Bank
        stepViewModel.initFields(dataViewModel.registrationData)
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvFormStep1.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFormStep1.setHasFixedSize(true)
        
        // CRITICAL FIX: If we are returning from backstack, the formAdapter already exists.
        // We MUST attach it to the new RecyclerView instance.
        formAdapter?.let { binding.rvFormStep1.adapter = it }
    }

    private fun setupListeners() {
        binding.btnNextStep.setOnClickListener {
            Log.i("FormStep1Fragment", "Action: Next Step (Location Details) button clicked")
            stepViewModel.validate()
        }
    }

    private fun observeViewModel() {
        // 1. Observe UI State (Success/Error/Loading)
        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.uiState.collect { state ->
                when (state) {
                    is RegistrationUiState.Success -> {
                        Log.i("FormStep1Fragment", "Step 1 Validation Passed. Navigating to Step 2.")
                        stepViewModel.setIdle()
                        
                        // Save local draft to SharedPreferences
                        sessionManager.saveRegistrationDraft(dataViewModel.registrationData)
                        dataViewModel.logCurrentData()
                        
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.single_owner_fragmentContainer, FormStep2Fragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    is RegistrationUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        binding.rvFormStep1.smoothScrollToPosition(0)
                    }
                    else -> {}
                }
            }
        }

        // 2. Observe Form Fields (Worker provides the list of items to show)
        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.formFields.collect { fields ->
                if (formAdapter == null) {
                    formAdapter = FormAdapter(fields) { key, value ->
                        updateBank(key, value as String)
                    }
                    binding.rvFormStep1.adapter = formAdapter
                } else {
                    formAdapter?.updateData(fields)
                }
            }
        }
    }

    /**
     * Updates the central "Bank" whenever the user types in Step 1.
     * This keeps the shared model in sync in real-time.
     */
    private fun updateBank(key: String, value: String) {
        val data = dataViewModel.registrationData
        when (key) {
            "owner_name" -> data.ownerFullName = value
            "owner_email" -> data.ownerEmail = value
            "owner_mobile" -> data.ownerMobile = value
            "res_name" -> data.restaurantName = value
            "business_type" -> data.businessType = value
            "legal_name" -> data.legalName = value
            "display_name" -> data.displayName = value
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

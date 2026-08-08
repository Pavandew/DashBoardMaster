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
import com.example.masterdashboard.databinding.FragmentFormStep2Binding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.manager_single_res_dash.form_screen.adapter.FormAdapter
import com.example.masterdashboard.manager_single_res_dash.form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.RegistrationDataViewModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.Step2ViewModel
import kotlinx.coroutines.launch

class FormStep2Fragment : Fragment() {

    private var _binding: FragmentFormStep2Binding? = null
    private val binding get() = _binding!!

    // Scoped to Activity (Bank)
    private val dataViewModel: RegistrationDataViewModel by activityViewModels()
    
    // Scoped to Fragment (Worker)
    private val stepViewModel: Step2ViewModel by viewModels()

    private var formAdapter: FormAdapter? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("FormStep2Fragment", "Navigation: Step 2 (Address & Contact) Opened")
        sessionManager = SessionManager(requireContext())
        
        stepViewModel.initFields(dataViewModel.registrationData)
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvFormStep2.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFormStep2.setHasFixedSize(true)
        
        // CRITICAL FIX: Attach adapter if it already exists (back navigation)
        formAdapter?.let { binding.rvFormStep2.adapter = it }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            Log.d("FormStep2Fragment", "Action: Back button clicked")
            parentFragmentManager.popBackStack()
        }

        binding.btnContinue.setOnClickListener {
            Log.i("FormStep2Fragment", "Action: Continue button clicked")
            stepViewModel.validate()
        }
    }

    private fun observeViewModel() {
        // 1. Observe UI State
        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.uiState.collect { state ->
                when (state) {
                    is RegistrationUiState.Success -> {
                        Log.i("FormStep2Fragment", "Step 2 Validation Passed. Navigating to Step 3.")
                        stepViewModel.setIdle()
                        
                        // Save local draft to SharedPreferences
                        sessionManager.saveRegistrationDraft(dataViewModel.registrationData)
                        dataViewModel.logCurrentData()
                        
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.single_owner_fragmentContainer, FormStep3Fragment())
                            .addToBackStack(null)
                            .commit()
                    }
                    is RegistrationUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        binding.rvFormStep2.smoothScrollToPosition(0)
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
                        updateBank(key, value as String)
                    }
                    binding.rvFormStep2.adapter = formAdapter
                } else {
                    formAdapter?.updateData(fields)
                }
            }
        }
    }

    /**
     * Updates the central "Bank" whenever the user types in Step 2.
     */
    private fun updateBank(key: String, value: String) {
        val data = dataViewModel.registrationData
        when (key) {
            "address" -> data.address = value
            "landmark" -> data.landmark = value
            "pin_code" -> data.pinCode = value
            "city" -> data.city = value
            "state" -> data.state = value
            "country" -> data.country = value
            "contact_number" -> data.contactNumber = value
            "contact_email" -> data.contactEmail = value
            "whatsapp" -> data.whatsappNumber = value
            "website" -> data.website = value
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

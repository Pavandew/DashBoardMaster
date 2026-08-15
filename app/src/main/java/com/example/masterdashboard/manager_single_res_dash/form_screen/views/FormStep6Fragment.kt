package com.example.masterdashboard.manager_single_res_dash.form_screen.views

import android.content.Intent
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
import com.example.masterdashboard.databinding.FragmentFormStep6Binding
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.form_screen.adapter.FormAdapter
import com.example.masterdashboard.manager_single_res_dash.form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.RegistrationDataViewModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel.Step6ViewModel
import kotlinx.coroutines.launch

class FormStep6Fragment : Fragment() {

    private var _binding: FragmentFormStep6Binding? = null
    private val binding get() = _binding!!

    private val dataViewModel: RegistrationDataViewModel by activityViewModels()
    private val stepViewModel: Step6ViewModel by viewModels()

    private var formAdapter: FormAdapter? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormStep6Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("FormStep6Fragment", "Navigation: Review & Launch Screen Opened")
        sessionManager = SessionManager(requireContext())
        
        binding.btnContinue.text = "Launch Restaurant"
        
        val currentData = dataViewModel.registrationData
        Log.d("FormStep6Fragment", "Compiling data for restaurant: ${currentData.restaurantName}")

        stepViewModel.initReviewData(currentData) { stepNumber ->
            Log.d("FormStep6Fragment", "Action: Edit Step $stepNumber clicked")
            navigateToStep(stepNumber)
        }
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvFormStep6.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFormStep6.setHasFixedSize(true)
        formAdapter?.let { binding.rvFormStep6.adapter = it }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            Log.d("FormStep6Fragment", "Action: Back button clicked")
            parentFragmentManager.popBackStack()
        }

        binding.btnContinue.setOnClickListener {
            Log.i("FormStep6Fragment", "Action: Launch Restaurant button clicked")
            val ownerUid = sessionManager.getUid()
            if (ownerUid.isNotEmpty()) {
                stepViewModel.launchRestaurant(dataViewModel.registrationData, ownerUid)
            } else {
                Toast.makeText(requireContext(), "Error: User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        // Observe Review Cards
        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.formFields.collect { fields ->
                if (formAdapter == null) {
                    formAdapter = FormAdapter(fields) { _, _ -> } // Review is read-only
                    binding.rvFormStep6.adapter = formAdapter
                } else {
                    formAdapter?.updateData(fields)
                }
            }
        }

        // Observe Final Submission State
        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.uiState.collect { state ->
                when (state) {
                    is RegistrationUiState.Loading -> {
                        binding.btnContinue.isEnabled = false
                        binding.btnContinue.text = "Launching..."
                    }
                    is RegistrationUiState.Success -> {
                        Log.i("FormStep6Fragment", "Success: Restaurant Launched ID: ${state.restaurantId}")
                        binding.btnContinue.isEnabled = true
                        binding.btnContinue.text = "Launch Restaurant"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        
                        // Save Restaurant ID to session
                        sessionManager.saveRestaurantId(state.restaurantId)
                        
                        // Clear draft and finish setup
                        sessionManager.clearRegistrationDraft()
                        sessionManager.setRestaurantSetup(true)
                        
                        // Restart activity completely fresh to load Dashboard and clear fragment stack
                        // Redirecting to ManagerHomeActivity as requested
                        val intent = Intent(requireContext(), ManagerHomeActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        activity?.finish()
                    }
                    is RegistrationUiState.Error -> {
                        binding.btnContinue.isEnabled = true
                        binding.btnContinue.text = "Launch Restaurant"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun navigateToStep(step: Int) {
        val fragment = when (step) {
            1 -> FormStep1Fragment()
            2 -> FormStep2Fragment()
            3 -> FormStep3Fragment()
            4 -> FormStep4Fragment()
            5 -> FormStep5Fragment()
            else -> null
        }
        
        fragment?.let {
            parentFragmentManager.beginTransaction()
                .replace(R.id.single_owner_fragmentContainer, it)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

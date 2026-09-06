package com.example.masterdashboard.manager_single_res_dash.registration_form_screen.views

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
import com.example.masterdashboard.databinding.FragmentFormStep5Binding
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.adapter.FormAdapter
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.viewModel.RegistrationDataViewModel
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.viewModel.Step5ViewModel
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.repository.RegistrationRepository

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
        Log.i("FormStep5Fragment", "Navigation: Review & Launch Screen Opened")

        sessionManager = SessionManager(requireContext())

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        val currentData = dataViewModel.registrationData
        Log.d("FormStep5Fragment", "Compiling data for restaurant: ${currentData.restaurantName}")

        stepViewModel.initReviewData(currentData) { stepNumber ->
            Log.d("FormStep5Fragment", "Action: Edit Step $stepNumber clicked")
            navigateToStep(stepNumber)
        }
    }

    private fun setupRecyclerView() {
        binding.rvFormStep5.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            Log.d("FormStep5Fragment", "Action: Back button clicked")
            parentFragmentManager.popBackStack()
        }

        binding.btnContinue.setOnClickListener {
            Log.i("FormStep5Fragment", "Action: Launch Restaurant button clicked")
            val finalData = dataViewModel.registrationData

            if (finalData.ownerUid.isEmpty()) {
                val cachedUid = sessionManager.getUid()
                if (cachedUid.isNotEmpty()) {
                    finalData.ownerUid = cachedUid
                } else {
                    Toast.makeText(requireContext(), "User Session Expired. Please login again.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            binding.btnContinue.isEnabled = false
            binding.btnContinue.text = "Creating Restaurant Profile..."

            lifecycleScope.launch {
                val repository = RegistrationRepository()
                val result = repository.saveFinalRegistration(finalData)

                result.fold(
                    onSuccess = { restaurantId ->
                        Log.i("FormStep5Fragment", "Success: Restaurant Transaction Complete. ID: $restaurantId")
                        sessionManager.setRestaurantSetup(true)
                        sessionManager.saveRestaurantId(restaurantId)
                        sessionManager.saveRestaurantName(finalData.restaurantName)
                        sessionManager.clearRegistrationDraft()

                        Toast.makeText(requireContext(), "Restaurant Setup Complete!", Toast.LENGTH_LONG).show()

                        val intent = Intent(requireContext(), ManagerHomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        activity?.finish()
                    },
                    onFailure = { e ->
                        binding.btnContinue.isEnabled = true
                        binding.btnContinue.text = "Launch Restaurant"
                        Toast.makeText(requireContext(), e.localizedMessage ?: "Registration failed", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            stepViewModel.formFields.collect { fields ->
                if (formAdapter == null) {
                    formAdapter = FormAdapter(fields) { _, _ -> }
                    binding.rvFormStep5.adapter = formAdapter
                } else {
                    formAdapter?.updateData(fields)
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

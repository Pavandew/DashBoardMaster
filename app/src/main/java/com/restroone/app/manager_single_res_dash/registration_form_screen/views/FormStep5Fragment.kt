package com.restroone.app.manager_single_res_dash.registration_form_screen.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.restroone.app.R
import com.restroone.app.databinding.FragmentFormStep5Binding
import com.restroone.app.manager_single_res_dash.registration_form_screen.adapter.FormAdapter
import com.restroone.app.manager_single_res_dash.registration_form_screen.repository.RegistrationRepository
import com.restroone.app.manager_single_res_dash.registration_form_screen.viewModel.RegistrationDataViewModel
import com.restroone.app.manager_single_res_dash.registration_form_screen.viewModel.Step5ViewModel
import com.restroone.app.subscription.views.SubscriptionPlansFragment
import com.restroone.app.utils.NetworkUtils
import com.restroone.app.utils.SessionManager
import kotlinx.coroutines.launch

class FormStep5Fragment : Fragment() {

    private var _binding: FragmentFormStep5Binding? = null
    private val binding get() = _binding!!

    private val dataViewModel: RegistrationDataViewModel by activityViewModels()
    private val stepViewModel: Step5ViewModel by viewModels()

    private var formAdapter: FormAdapter? = null
    private lateinit var sessionManager: SessionManager
    private var isSaving = false

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
        val isEdit = dataViewModel.isEditMode
        Log.i("FormStep5Fragment", "Navigation: Review Screen Opened (isEditMode: $isEdit)")

        sessionManager = SessionManager(requireContext())

        setupBackPressInterceptor()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        val currentData = dataViewModel.registrationData
        Log.d("FormStep5Fragment", "Compiling data for restaurant: ${currentData.restaurantName}")

        if (isEdit) {
            binding.btnContinue.text = "Save & Return to Settings"
        } else {
            binding.btnContinue.text = "Launch Restaurant"
        }

        stepViewModel.initReviewData(currentData, isEdit) { stepNumber ->
            if (isSaving) {
                Toast.makeText(requireContext(), "Save in progress... Please wait.", Toast.LENGTH_SHORT).show()
                return@initReviewData
            }
            Log.d("FormStep5Fragment", "Action: Edit Step $stepNumber clicked")
            navigateToStep(stepNumber)
        }
    }

    private fun setupBackPressInterceptor() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSaving) {
                    Toast.makeText(requireContext(), "Creating restaurant profile... Please wait.", Toast.LENGTH_SHORT).show()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupRecyclerView() {
        binding.rvFormStep5.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFormStep5.setHasFixedSize(true)
        formAdapter?.let { binding.rvFormStep5.adapter = it }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            if (isSaving) {
                Toast.makeText(requireContext(), "Creating restaurant profile... Please wait.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("FormStep5Fragment", "Action: Back button clicked")
            parentFragmentManager.popBackStack()
        }

        binding.btnContinue.setOnClickListener {
            if (isSaving) return@setOnClickListener

            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val isEdit = dataViewModel.isEditMode
            Log.i("FormStep5Fragment", "Action: Submit button clicked (isEditMode: $isEdit)")
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

            isSaving = true
            binding.btnContinue.isEnabled = false
            binding.btnBack.isEnabled = false
            binding.btnContinue.text = if (isEdit) "Saving Changes..." else "Creating Restaurant Profile..."

            lifecycleScope.launch {
                val repository = RegistrationRepository()
                val result = repository.saveFinalRegistration(requireContext(), finalData)

                if (!isAdded) return@launch

                result.fold(
                    onSuccess = { restaurantId ->
                        isSaving = false
                        Log.i("FormStep5Fragment", "Success: Transaction Complete. ID: $restaurantId (isEditMode: $isEdit)")
                        sessionManager.setRestaurantSetup(true)
                        sessionManager.saveRestaurantId(restaurantId)
                        sessionManager.saveRestaurantName(finalData.restaurantName)
                        val finalLogo = finalData.restaurantLogoUri ?: finalData.billingPrinterSettings.restaurantLogoUri ?: ""
                        sessionManager.saveRestaurantLogoUrl(finalLogo)
                        sessionManager.saveRestaurantDetails(finalData)
                        sessionManager.clearRegistrationDraft()

                        if (isEdit) {
                            Toast.makeText(requireContext(), "Restaurant details updated successfully!", Toast.LENGTH_SHORT).show()
                            activity?.finish()
                        } else {
                            Toast.makeText(requireContext(), "Restaurant Setup Complete!", Toast.LENGTH_SHORT).show()
                            Log.i("FormStep5Fragment", "Navigating to SubscriptionPlansFragment after registration complete")
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.single_owner_fragmentContainer, SubscriptionPlansFragment())
                                .commit()
                        }
                    },
                    onFailure = { e ->
                        isSaving = false
                        _binding?.let { b ->
                            b.btnContinue.isEnabled = true
                            b.btnBack.isEnabled = true
                            b.btnContinue.text = if (isEdit) "Save & Return to Settings" else "Launch Restaurant"
                        }
                        Toast.makeText(context ?: return@launch, e.localizedMessage ?: "Save failed", Toast.LENGTH_SHORT).show()
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

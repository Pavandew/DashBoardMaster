package com.example.masterdashboard.manager_single_res_dash.views

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentAddStaffBinding
import com.example.masterdashboard.manager_single_res_dash.adapter.AddStaffFormAdapter
import com.example.masterdashboard.manager_single_res_dash.viewModel.PhoneCheckState
import com.example.masterdashboard.manager_single_res_dash.viewModel.StaffFormViewModel
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

class AddStaffFragment : Fragment() {

    companion object {
        private const val TAG = "AddStaffFragment"
    }

    private var _binding: FragmentAddStaffBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: StaffFormViewModel by activityViewModels()
    private lateinit var formAdapter : AddStaffFormAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddStaffBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            parentFragmentManager.popBackStack()
        }

        Log.i(TAG, "Navigation: AddStaffFragment Opened")
        setupToolbar()
        setupRecyclerView()
        setupValidationObservers()
    }

    private fun setupToolbar() {
        val toolbar = binding.addStaffToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        
        // Dynamically set title based on Edit Mode
        val isEditMode = sharedViewModel.isEditMode.value
        toolbar.tvToolbarTitle.text = if (isEditMode) "Edit Staff Profile" else getString(R.string.add_staff)
        
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        val currentData = sharedViewModel.currentStaffData.value
        formAdapter = AddStaffFormAdapter(currentData) { name, mobile, email, gender, role, department, joiningDate, shift, salary ->

            // 1. Run required fields validations
            if (!isValidInput(name, mobile, email, role, department, joiningDate, shift)) {
                return@AddStaffFormAdapter
            }

            // 2. Cross-verify mobile data array sets to ensure entries do not already exist
            val ownerUid = sessionManager.getUid()
            sharedViewModel.verifyMobileAndProceed(ownerUid, mobile) {

                // 3. Save personal fields back to model cache flow states
                sharedViewModel.updateStep1Data(
                    name, mobile, email, gender, role, department, joiningDate, shift, salary
                )

                // 4. Navigate forward smoothly to Step 2 Layout Fragment Screen
                parentFragmentManager.beginTransaction()
                    .replace(R.id.manager_fragmentContainer, PermissionsAndDocumentsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.rvAddStaffForm.adapter = formAdapter
    }

    private fun setupValidationObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.phoneState.collect { state ->
                    when(state) {
                        is PhoneCheckState.AlreadyRegistered -> {
                            Toast.makeText(requireContext(), "This mobile number is already registered!", Toast.LENGTH_LONG).show()
                            sharedViewModel.resetPhoneState()
                        }
                        is PhoneCheckState.Error -> {
                            Toast.makeText(requireContext(), "Error checking records: ${state.errorMsg}", Toast.LENGTH_SHORT).show()
                            sharedViewModel.resetPhoneState()
                        }
                        else -> { /* No operation */ }
                    }
                }
            }
        }
    }

    private fun isValidInput(
        staffName: String, mobile: String, email: String, role: String,
        department: String, joiningDate: String, shift: String
    ): Boolean {
        if (staffName.isEmpty() || mobile.isEmpty() || role.isEmpty() ||
            department.isEmpty() || joiningDate.isEmpty() || shift.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields (*)", Toast.LENGTH_SHORT).show()
            return false
        }
        if (mobile.length < 10) {
            Toast.makeText(requireContext(), "Please enter a valid mobile number", Toast.LENGTH_SHORT).show()
            return false
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
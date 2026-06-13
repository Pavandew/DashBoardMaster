package com.example.masterdashboard.manager_single_res_dash.home.views

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
import com.example.masterdashboard.manager_single_res_dash.home.adapter.AddStaffFormAdapter
import com.example.masterdashboard.manager_single_res_dash.home.viewModel.PhoneCheckState
import com.example.masterdashboard.manager_single_res_dash.home.viewModel.StaffFormViewModel
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

class AddStaffFragment : Fragment() {

    companion object {
        private const val TAG = "AddStaffFragment"
    }

    private var _binding: FragmentAddStaffBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: StaffFormViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddStaffBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {
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
        toolbar.tvToolbarTitle.text = getString(R.string.add_staff)
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
        val formAdapter = AddStaffFormAdapter(currentData) { name, mobile, email, gender, role, department, joiningDate, shift, salary ->

            // 1. Run validation first
            if (!isValidInput(name, mobile, email, role, department, joiningDate, shift)) {
                return@AddStaffFormAdapter
            }

            // 2. Check if mobile number is already registered
            val ownerUid = sessionManager.getUid()
            sharedViewModel.verifyMobileAndProceed(ownerUid, mobile) {
                // This block runs only if phone is NOT already registered
                
                // Log incoming data only if validation passes
                Log.d(TAG, "================ STEP 1 VALIDATION PASSED ================")
                Log.d(TAG, "Full Name    : $name")
                Log.d(TAG, "Mobile No    : $mobile")
                Log.d(TAG, "Email        : $email")
                Log.d(TAG, "Gender       : $gender")
                Log.d(TAG, "Role         : $role")
                Log.d(TAG, "Department   : $department")
                Log.d(TAG, "Joining Date : $joiningDate")
                Log.d(TAG, "Shift        : $shift")
                Log.d(TAG, "Salary       : $salary")
                Log.d(TAG, "======================================================")

                // 3. Save data collected from fields into our shared ViewModel state
                sharedViewModel.updateStep1Data(name, mobile, email, gender, role, department, joiningDate, shift, salary)

                // 4. Navigate over to the step 2 screen fragment layout
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

                        is PhoneCheckState.Checking -> {

                        }
                        is PhoneCheckState.AlreadyRegistered -> {
                            Toast.makeText(requireContext(), "This mobile number is already registered!", Toast.LENGTH_LONG).show()
                            sharedViewModel.resetPhoneState() // Clear state to allow corrections
                        }
                        is PhoneCheckState.Error -> {
                            Toast.makeText(requireContext(), "Error checking records: ${state.errorMsg}", Toast.LENGTH_SHORT).show()
                            sharedViewModel.resetPhoneState()
                        }
                        is PhoneCheckState.Available, PhoneCheckState.Idle -> {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }

    // Extracted clean validation function returning a Boolean status flag
    private fun isValidInput(
        staffName: String, mobile: String, email: String, role: String,
        department: String, joiningDate: String, shift: String
    ): Boolean {

        // 1. Check required fields
        if (staffName.isEmpty() || mobile.isEmpty() || role.isEmpty() ||
            department.isEmpty() || joiningDate.isEmpty() || shift.isEmpty()) {

            Toast.makeText(requireContext(), "Please fill all required fields (*)", Toast.LENGTH_SHORT).show()
            return false
        }

        // 2. Check mobile number digits length restriction
        if (mobile.length < 10) {
            Toast.makeText(requireContext(), "Please enter a valid mobile number", Toast.LENGTH_SHORT).show()
            return false
        }

        // 3. Check email format if it's not empty
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }

        return true // All validation rules passed successfully
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        sharedViewModel.clearFormData()
        _binding = null
    }
}

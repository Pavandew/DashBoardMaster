package com.example.masterdashboard.login.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentChangePasswordBinding
import com.example.masterdashboard.master_dash.MasterHomeActivity
import com.example.masterdashboard.login.repo.ChangePasswordRepository
import com.example.masterdashboard.login.viewmodel.ChangePasswordState
import com.example.masterdashboard.login.viewmodel.ChangePasswordViewModel
import com.example.masterdashboard.login.viewmodel.ChangePasswordViewModelFactory
import com.example.masterdashboard.staff_dash.billing_screens.CashierHomeActivity
import com.example.masterdashboard.staff_dash.kitchen_screens.KitchenHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChangePasswordFragment : Fragment() {
    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val TAG = "ChangePasswordFrag"

    private val viewModel: ChangePasswordViewModel by viewModels {
        ChangePasswordViewModelFactory(ChangePasswordRepository())
    }
    private val sessionManager by lazy { SessionManager(requireContext()) }

    companion object {
        /**
         * Use this factory method to create a new instance of this fragment
         * for Forgot Password flow.
         */
        fun newInstance(phone: String, ownerUid: String, staffDocId: String, role: String): ChangePasswordFragment {
            val fragment = ChangePasswordFragment()
            val args = Bundle()
            args.putString(AppConstants.KEY_MOBILE, phone)
            args.putString(AppConstants.FIELD_UID, ownerUid)
            args.putString(AppConstants.KEY_STAFF_DOC_ID, staffDocId)
            args.putString(AppConstants.FIELD_ROLE, role)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Fragment launched. Initializing setup.")

        setupToolbar()
        setupClickListeners()
        observeViewModel()

        // Get phone from arguments (Forgot Password flow) or Session (Change Password flow)
        val argPhone = arguments?.getString(AppConstants.KEY_MOBILE)
        val phone = argPhone ?: sessionManager.getPhone() ?: ""

        if (phone.isNotEmpty()) {
            val maskedPhone = if (phone.length > 5) {
                "${phone.substring(0, 3)}XXXXXX${phone.substring(phone.length - 2)}"
            } else phone
            Log.d(TAG, "onViewCreated: Masked phone for UI: $maskedPhone")
            binding.tvOtpMessage.text = "We will send an OTP to $maskedPhone"
        } else {
            Log.w(TAG, "onViewCreated: Phone number missing")
        }
    }

    private fun setupToolbar() {
        binding.settingsToolbar.toolbarTvTitle.text = getString(R.string.change_password)
        binding.settingsToolbar.toolbarImgMenu.visibility = View.VISIBLE
        binding.settingsToolbar.toolbarImgProfile.visibility = View.GONE
        binding.settingsToolbar.toolbarImgNotification.visibility = View.GONE

        binding.settingsToolbar.toolbarImgMenu.setOnClickListener {
            Log.d(TAG, "Toolbar: Menu icon clicked")
            when (val act = activity) {
                is MasterHomeActivity -> act.openDrawer()
                is WaiterHomeActivity -> act.onBackPressedDispatcher.onBackPressed()
                is CashierHomeActivity -> act.onBackPressedDispatcher.onBackPressed()
                is KitchenHomeActivity -> act.onBackPressedDispatcher.onBackPressed()
                else -> act?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSendOtp.setOnClickListener {
            val argPhone = arguments?.getString(AppConstants.KEY_MOBILE)
            val phone = argPhone ?: sessionManager.getPhone()

            Log.i(TAG, "btnSendOtp clicked. Fetching phone: $phone")
            if (!phone.isNullOrEmpty()) {
                viewModel.sendOtp(phone, requireActivity())
            } else {
                Log.w(TAG, "btnSendOtp: Phone number is null or empty")
                Toast.makeText(requireContext(), "Phone number not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val code = binding.etOtp.text.toString().trim()
            Log.i(TAG, "btnVerifyOtp clicked. Entered code: $code")
            if (code.length == 6) {
                viewModel.verifyOtp(code)
            } else {
                Log.w(TAG, "btnVerifyOtp: Invalid code length: ${code.length}")
                Toast.makeText(requireContext(), "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnChangePassword.setOnClickListener {
            val newPass = binding.etNewPassword.text.toString().trim()
            val confirmPass = binding.etConfirmPassword.text.toString().trim()
            Log.i(TAG, "btnChangePassword clicked. Processing input validation.")

            binding.layoutNewPassword.error = null
            binding.layoutConfirmPassword.error = null

            if (newPass.length < 6) {
                Log.w(TAG, "Validation Failed: Password too short")
                binding.layoutNewPassword.error = "Minimum 6 characters"
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                Log.w(TAG, "Validation Failed: Passwords do not match")
                binding.layoutConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Get IDs from arguments if it's Forgot Password flow
            val uid = arguments?.getString(AppConstants.FIELD_UID) ?: sessionManager.getUid()
            val staffDocId = arguments?.getString(AppConstants.KEY_STAFF_DOC_ID) ?: sessionManager.getStaffDocId()
            val role = arguments?.getString(AppConstants.FIELD_ROLE) ?: sessionManager.getRole()

            Log.d(TAG, "Validation Success: Calling ViewModel updatePassword")
            viewModel.updatePassword(
                newPassword = newPass,
                role = role,
                uid = uid,
                staffDocId = staffDocId
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                Log.d(TAG, "New ViewModel State: ${state::class.java.simpleName}")
                when (state) {
                    is ChangePasswordState.Idle -> {
                        binding.btnSendOtp.isEnabled = true
                    }
                    is ChangePasswordState.Loading -> {
                        Log.d(TAG, "State: Loading...")
                    }
                    is ChangePasswordState.OtpSent -> {
                        Log.i(TAG, "State: OtpSent. Updating UI to Verification mode.")
                        binding.btnSendOtp.visibility = View.GONE
                        binding.layoutOtp.visibility = View.VISIBLE
                        binding.btnVerifyOtp.visibility = View.VISIBLE
                        binding.btnVerifyOtp.isEnabled = true
                        Toast.makeText(requireContext(), "OTP Sent Successfully", Toast.LENGTH_SHORT).show()
                    }
                    is ChangePasswordState.OtpVerified -> {
                        Log.i(TAG, "State: OtpVerified. Updating UI to Password Entry mode.")
                        binding.sectionOtp.visibility = View.GONE
                        binding.sectionNewPassword.visibility = View.VISIBLE
                        binding.btnChangePassword.isEnabled = true
                        Toast.makeText(requireContext(), "Identity Verified", Toast.LENGTH_SHORT).show()
                    }
                    is ChangePasswordState.Success -> {
                        Log.i(TAG, "State: Success! Password updated. Closing fragment.")
                        Toast.makeText(requireContext(), "Password Updated Successfully", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                    is ChangePasswordState.Error -> {
                        Log.e(TAG, "State: Error -> ${state.message}")
                        binding.btnSendOtp.isEnabled = true
                        binding.btnVerifyOtp.isEnabled = true
                        binding.btnChangePassword.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Cleaning up binding")
        _binding = null
    }
}
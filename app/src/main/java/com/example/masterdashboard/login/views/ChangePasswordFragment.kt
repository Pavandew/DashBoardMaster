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
         * for Forgot Password flow or explicit user arguments.
         */
        fun newInstance(
            phone: String,
            ownerUid: String,
            staffDocId: String,
            role: String,
            isForgotPassword: Boolean = false
        ): ChangePasswordFragment {
            val fragment = ChangePasswordFragment()
            val args = Bundle()
            args.putString(AppConstants.KEY_MOBILE, phone)
            args.putString(AppConstants.FIELD_UID, ownerUid)
            args.putString(AppConstants.KEY_STAFF_DOC_ID, staffDocId)
            args.putString(AppConstants.FIELD_ROLE, role)
            args.putBoolean("is_forgot_password", isForgotPassword)
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

        val isForgotPassword = arguments?.getBoolean("is_forgot_password", false) == true
        if (isForgotPassword) {
            binding.settingsToolbar.toolbarTvTitle.text = "Reset Password"
            binding.sectionCurrentPassword.visibility = View.GONE
            binding.sectionOtp.visibility = View.VISIBLE
            binding.btnUseCurrentPassword.visibility = View.GONE
        }

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
        binding.settingsToolbar.toolbarImgMenu.setImageResource(R.drawable.ic_arrow_back_24dp)
        binding.settingsToolbar.toolbarImgMenu.visibility = View.VISIBLE
        binding.settingsToolbar.toolbarImgProfile.visibility = View.GONE
        binding.settingsToolbar.toolbarImgNotification.visibility = View.GONE

        binding.settingsToolbar.toolbarImgMenu.setOnClickListener {
            Log.d(TAG, "Toolbar: Back/Menu icon clicked")
            when (val act = activity) {
                is MasterHomeActivity -> act.openDrawer()
                else -> act?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private fun setupClickListeners() {
        // Option 1: Current Password Verification
        binding.btnVerifyCurrentPassword.setOnClickListener {
            val currentPass = binding.etCurrentPassword.text.toString().trim()
            binding.layoutCurrentPassword.error = null

            if (currentPass.isEmpty()) {
                binding.layoutCurrentPassword.error = "Enter current password"
                return@setOnClickListener
            }

            val uid = arguments?.getString(AppConstants.FIELD_UID) ?: sessionManager.getUid()
            val staffDocId = arguments?.getString(AppConstants.KEY_STAFF_DOC_ID) ?: sessionManager.getStaffDocId()
            val role = arguments?.getString(AppConstants.FIELD_ROLE) ?: sessionManager.getRole()

            Log.i(TAG, "btnVerifyCurrentPassword clicked. Verifying current password for role: $role, UID: $uid")
            viewModel.verifyCurrentPassword(
                currentPassword = currentPass,
                role = role,
                uid = uid,
                staffDocId = staffDocId
            )
        }

        // Toggle to Option 2: Mobile OTP
        binding.btnForgotCurrentPassword.setOnClickListener {
            Log.i(TAG, "btnForgotCurrentPassword clicked. Switching to Mobile OTP section.")
            binding.sectionCurrentPassword.visibility = View.GONE
            binding.sectionOtp.visibility = View.VISIBLE
        }

        // Toggle back to Option 1: Current Password
        binding.btnUseCurrentPassword.setOnClickListener {
            Log.i(TAG, "btnUseCurrentPassword clicked. Switching to Current Password section.")
            binding.sectionOtp.visibility = View.GONE
            binding.sectionCurrentPassword.visibility = View.VISIBLE
        }

        // Send Mobile OTP
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

        // Verify Mobile OTP
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

        // Submit New Password
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
                        binding.btnVerifyCurrentPassword.isEnabled = true
                        binding.btnSendOtp.isEnabled = true
                        binding.btnVerifyOtp.isEnabled = true
                        binding.btnChangePassword.isEnabled = true
                    }
                    is ChangePasswordState.Loading -> {
                        Log.d(TAG, "State: Loading...")
                        binding.btnVerifyCurrentPassword.isEnabled = false
                        binding.btnSendOtp.isEnabled = false
                        binding.btnVerifyOtp.isEnabled = false
                        binding.btnChangePassword.isEnabled = false
                    }
                    is ChangePasswordState.CurrentPasswordVerified,
                    is ChangePasswordState.OtpVerified -> {
                        Log.i(TAG, "State: Verified! Showing New Password Section.")
                        binding.sectionCurrentPassword.visibility = View.GONE
                        binding.sectionOtp.visibility = View.GONE
                        binding.sectionNewPassword.visibility = View.VISIBLE
                        binding.btnChangePassword.isEnabled = true
                        Toast.makeText(requireContext(), "Identity Verified Successfully", Toast.LENGTH_SHORT).show()
                    }
                    is ChangePasswordState.OtpSent -> {
                        Log.i(TAG, "State: OtpSent. Updating UI to Verification mode.")
                        binding.btnSendOtp.visibility = View.GONE
                        binding.layoutOtp.visibility = View.VISIBLE
                        binding.btnVerifyOtp.visibility = View.VISIBLE
                        binding.btnVerifyOtp.isEnabled = true
                        Toast.makeText(requireContext(), "OTP Sent Successfully", Toast.LENGTH_SHORT).show()
                    }
                    is ChangePasswordState.Success -> {
                        Log.i(TAG, "State: Success! Password updated. Closing fragment.")
                        Toast.makeText(requireContext(), "Password Updated Successfully", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                    is ChangePasswordState.Error -> {
                        Log.e(TAG, "State: Error -> ${state.message}")
                        binding.btnVerifyCurrentPassword.isEnabled = true
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

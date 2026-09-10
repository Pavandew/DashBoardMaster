package com.example.masterdashboard.manager_single_res_dash.settings

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
import com.example.masterdashboard.databinding.FragmentManagerChangePasswordBinding
import com.example.masterdashboard.login.repo.ChangePasswordRepository
import com.example.masterdashboard.login.viewmodel.ChangePasswordState
import com.example.masterdashboard.login.viewmodel.ChangePasswordViewModel
import com.example.masterdashboard.login.viewmodel.ChangePasswordViewModelFactory
import com.example.masterdashboard.manager_single_res_dash.ManagerHomeActivity
import com.example.masterdashboard.manager_single_res_dash.SingleResOwnerHomeActivity
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManagerChangePasswordFragment : Fragment() {

    companion object {
        private const val TAG = "ManagerChangePassFrag"
    }

    private var _binding: FragmentManagerChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChangePasswordViewModel by viewModels {
        ChangePasswordViewModelFactory(ChangePasswordRepository())
    }
    private val sessionManager by lazy { SessionManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: Manager Change Password fragment launched")

        setupToolbar()
        setupClickListeners()
        observeViewModel()

        val phone = sessionManager.getPhone() ?: ""
        if (phone.isNotEmpty()) {
            val maskedPhone = if (phone.length > 5) {
                "${phone.substring(0, 3)}XXXXXX${phone.substring(phone.length - 2)}"
            } else phone
            binding.tvOtpMessage.text = "We will send an OTP to $maskedPhone"
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.settingsToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.change_password)

        toolbar.btnBack.setOnClickListener {
            Log.d(TAG, "Toolbar back clicked")
            parentFragmentManager.popBackStack()
        }

        toolbar.btnDrawerMenu.setOnClickListener {
            Log.d(TAG, "Toolbar drawer clicked")
            when (val act = activity) {
                is ManagerHomeActivity -> act.openNavigationDrawer()
                is SingleResOwnerHomeActivity -> act.openNavigationDrawer()
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

            val uid = sessionManager.getUid()
            val staffDocId = sessionManager.getStaffDocId()
            val role = sessionManager.getRole()

            Log.i(TAG, "btnVerifyCurrentPassword clicked. Verifying for UID: $uid, role: $role")
            viewModel.verifyCurrentPassword(
                currentPassword = currentPass,
                role = role,
                uid = uid,
                staffDocId = staffDocId
            )
        }

        // Toggle to Option 2: Mobile OTP
        binding.btnForgotCurrentPassword.setOnClickListener {
            Log.i(TAG, "Switching to Mobile OTP section")
            binding.sectionCurrentPassword.visibility = View.GONE
            binding.sectionOtp.visibility = View.VISIBLE
        }

        // Toggle back to Option 1: Current Password
        binding.btnUseCurrentPassword.setOnClickListener {
            Log.i(TAG, "Switching to Current Password section")
            binding.sectionOtp.visibility = View.GONE
            binding.sectionCurrentPassword.visibility = View.VISIBLE
        }

        // Send Mobile OTP
        binding.btnSendOtp.setOnClickListener {
            val phone = sessionManager.getPhone()
            if (!phone.isNullOrEmpty()) {
                viewModel.sendOtp(phone, requireActivity())
            } else {
                Toast.makeText(requireContext(), "Phone number not found", Toast.LENGTH_SHORT).show()
            }
        }

        // Verify Mobile OTP
        binding.btnVerifyOtp.setOnClickListener {
            val code = binding.etOtp.text.toString().trim()
            if (code.length == 6) {
                viewModel.verifyOtp(code)
            } else {
                Toast.makeText(requireContext(), "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        // Submit New Password
        binding.btnChangePassword.setOnClickListener {
            val newPass = binding.etNewPassword.text.toString().trim()
            val confirmPass = binding.etConfirmPassword.text.toString().trim()

            binding.layoutNewPassword.error = null
            binding.layoutConfirmPassword.error = null

            if (newPass.length < 6) {
                binding.layoutNewPassword.error = "Minimum 6 characters"
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                binding.layoutConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            val uid = sessionManager.getUid()
            val staffDocId = sessionManager.getStaffDocId()
            val role = sessionManager.getRole()

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
                when (state) {
                    is ChangePasswordState.Idle -> {
                        setLoading(false)
                    }
                    is ChangePasswordState.Loading -> {
                        setLoading(true)
                    }
                    is ChangePasswordState.CurrentPasswordVerified,
                    is ChangePasswordState.OtpVerified -> {
                        setLoading(false)
                        binding.sectionCurrentPassword.visibility = View.GONE
                        binding.sectionOtp.visibility = View.GONE
                        binding.sectionNewPassword.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Identity Verified Successfully", Toast.LENGTH_SHORT).show()
                    }
                    is ChangePasswordState.OtpSent -> {
                        setLoading(false)
                        binding.btnSendOtp.visibility = View.GONE
                        binding.layoutOtp.visibility = View.VISIBLE
                        binding.btnVerifyOtp.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "OTP Sent Successfully", Toast.LENGTH_SHORT).show()
                    }
                    is ChangePasswordState.Success -> {
                        setLoading(false)
                        Toast.makeText(requireContext(), "Password Updated Successfully", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack()
                    }
                    is ChangePasswordState.Error -> {
                        setLoading(false)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnVerifyCurrentPassword.isEnabled = !isLoading
        binding.btnSendOtp.isEnabled = !isLoading
        binding.btnVerifyOtp.isEnabled = !isLoading
        binding.btnChangePassword.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

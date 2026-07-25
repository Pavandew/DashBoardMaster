package com.example.masterdashboard.staff_dash.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentStaffProfileBinding
import com.example.masterdashboard.login.utils.LogoutManager
import com.example.masterdashboard.login.utils.SessionManager
import kotlinx.coroutines.launch

class StaffProfileFragment : Fragment() {

    companion object {
        private const val TAG = "StaffProfile_Debug"
    }

    private var _binding: FragmentStaffProfileBinding? = null
    private val binding get() = _binding!!

    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val logoutManager by lazy { LogoutManager(requireContext()) }

    private val viewModel: StaffProfileViewModel by viewModels {
        StaffProfileViewModelFactory(StaffProfileRepository(sessionManager = sessionManager))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "📱 [FRAGMENT] onViewCreated: Initializing profile screen.")

        setupToolbar()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.staffProfileToolbar.apply {
            // Set the main title text
            tvToolbarTitle.text = "Profile"

            toolbarImgMenu.visibility = View.GONE
            toolbarImgNotification.visibility = View.GONE
            tvToolbarEndText.visibility = View.GONE
            llSubtitleContainer.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.waiterLogoutBtn.setOnClickListener {
            Log.d(TAG, "📱 [FRAGMENT] Logout clicked.")
            logoutManager.showLogoutConfirmation()
        }

        binding.btnForgotPassword.setOnClickListener {
            Log.d(TAG, "📱 [FRAGMENT] Forgot Password clicked.")
            Toast.makeText(requireContext(), "Password reset link requested", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d(TAG, "📱 [FRAGMENT] New UI State: ${state::class.java.simpleName}")
                    when (state) {
                        is StaffProfileUiState.Loading -> {
                            // Optionally show progress bar
                        }
                        is StaffProfileUiState.Success -> {
                            Log.i(TAG, "📱 [FRAGMENT] State Success: Binding data for '${state.profile.staffName}'")
                            bindProfileData(state.profile)
                        }
                        is StaffProfileUiState.Error -> {
                            Log.e(TAG, "📱 [FRAGMENT] State Error: ${state.message}")
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun bindProfileData(profile: StaffProfileModel) {
        binding.apply {
            tvProfileName.text = profile.staffName.ifEmpty { "N/A" }
            tvRoleBadge.text = profile.role.ifEmpty { "WAITER" }.uppercase()
            tvShiftValue.text = profile.shift.ifEmpty { "Not Assigned" }
            tvPhoneValue.text = profile.mobile.ifEmpty { "N/A" }
            tvEmailValue.text = profile.email.ifEmpty { "N/A" }
            tvGenderValue.text = profile.gender.ifEmpty { "N/A" }
            tvDateJoinedValue.text = profile.joiningDate.ifEmpty { "N/A" }

            Log.d(TAG, "📱 [FRAGMENT] Data Bound: Role=${profile.role}, Shift=${profile.shift}, Gender=${profile.gender}, Joined=${profile.joiningDate}")

            if (profile.profileImageUrl.isNotEmpty()) {
                Log.d(TAG, "📱 [FRAGMENT] Loading Avatar: ${profile.profileImageUrl}")
                Glide.with(requireContext())
                    .load(profile.profileImageUrl)
                    .placeholder(R.drawable.person)
                    .error(R.drawable.person)
                    .into(imgAvatar)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

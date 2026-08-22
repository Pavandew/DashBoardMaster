package com.example.masterdashboard.manager_single_res_dash.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentStaffDetailBinding
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.manager_single_res_dash.models.StaffDataModel
import com.example.masterdashboard.manager_single_res_dash.uistate.StaffDetailUiState
import com.example.masterdashboard.manager_single_res_dash.viewModel.StaffDetailViewModel
import kotlinx.coroutines.launch

class StaffDetailFragment : Fragment() {

    companion object {
        private const val TAG = "StaffDetailFragment"
    }

    private var _binding: FragmentStaffDetailBinding? = null
    private val binding get() = _binding!!

    // ✅ Instantiate using modern decoupled delegation extensions
    private val viewModel: StaffDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            parentFragmentManager.popBackStack()
        }

        setupToolbar()
        observeDetailUiPipeline()

        val staffDocId = arguments?.getString("STAFF_DOCUMENT_ID") ?: ""
        val ownerUid = SessionManager(requireContext()).getUid()

        Log.d(TAG, "Requesting details matching Staff ID: '$staffDocId' under Owner: '$ownerUid'")
        viewModel.loadStaffProfile(ownerUid, staffDocId)
    }

    private fun setupToolbar() {
        val toolbar = binding.detailToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = "Staff Complete Profile"
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun observeDetailUiPipeline() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is StaffDetailUiState.Loading -> {
                            binding.detailProgressBar.visibility = View.VISIBLE
                            binding.detailScrollView.visibility = View.GONE
                        }
                        is StaffDetailUiState.Error -> {
                            binding.detailProgressBar.visibility = View.GONE
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                        is StaffDetailUiState.Success -> {
                            binding.detailProgressBar.visibility = View.GONE
                            binding.detailScrollView.visibility = View.VISIBLE
                            populateUI(state.staff)
                        }
                    }
                }
            }
        }
    }

    private fun populateUI(data: StaffDataModel) {
        binding.tvDetailName.text = data.staffName
        binding.tvDetailRoleAndId.text = "${data.role} • ID: ${data.staffId}"
        binding.tvDetailStatusTag.text = data.status

        binding.tvDetailJoinDate.text = data.joiningDate.ifEmpty { "Not Specified" }
        binding.tvDetailDepartment.text = data.department.ifEmpty { "General" }
        binding.tvDetailShift.text = data.shift.ifEmpty { "Not Allocated" }
        binding.tvDetailSalary.text = if (data.salary.isNotEmpty()) "₹ ${data.salary}" else "Confidential"

        binding.tvDetailMobile.text = data.mobile.ifEmpty { "N/A" }
        binding.tvDetailEmail.text = data.email.ifEmpty { "N/A" }
        binding.tvDetailGender.text = data.gender

        binding.tvDetailDocType.text = data.documentType.ifEmpty { "Official ID Card" }
        binding.tvDetailDocNumber.text = data.documentNumber.ifEmpty { "Unverified/Missing" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


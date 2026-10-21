package com.restroone.app.manager_single_res_dash.views

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.restroone.app.R
import com.restroone.app.databinding.FragmentStaffDetailBinding
import com.restroone.app.utils.SessionManager
import com.restroone.app.manager_single_res_dash.models.StaffDataModel
import com.restroone.app.manager_single_res_dash.uistate.StaffDetailUiState
import com.restroone.app.manager_single_res_dash.viewModel.StaffDetailViewModel
import com.restroone.app.manager_single_res_dash.viewModel.StaffFormViewModel
import coil.load
import kotlinx.coroutines.launch

class StaffDetailFragment : Fragment() {

    companion object {
        private const val TAG = "StaffDetailFragment"
    }

    private var _binding: FragmentStaffDetailBinding? = null
    private val binding get() = _binding!!

    // ✅ Instantiate using modern decoupled delegation extensions
    private val viewModel: StaffDetailViewModel by viewModels()
    private val staffFormViewModel: StaffFormViewModel by activityViewModels()

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

        // Fast-Load: If StaffDataModel was passed directly from list, render instantly at 0ms speed!
        @Suppress("DEPRECATION")
        val initialStaff = arguments?.getSerializable("STAFF_MODEL") as? StaffDataModel
        if (initialStaff != null) {
            Log.d(TAG, "Fast-Load: Instantly populating UI from passed staff model for '${initialStaff.staffName}'")
            binding.detailProgressBar.visibility = View.GONE
            binding.detailScrollView.visibility = View.VISIBLE
            populateUI(initialStaff)
        }

        val staffDocId = arguments?.getString("STAFF_DOCUMENT_ID") ?: initialStaff?.id ?: ""
        val ownerUid = SessionManager(requireContext()).getRestaurantId()

        if (ownerUid.isNotEmpty() && staffDocId.isNotEmpty()) {
            Log.d(TAG, "Background sync matching Staff ID: '$staffDocId' under Owner: '$ownerUid'")
            viewModel.loadStaffProfile(ownerUid, staffDocId)
        }
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
        
        // Use Notification icon slot as Edit button for Details view
        toolbar.toolbarImgNotification.apply {
            visibility = View.VISIBLE
            setImageResource(R.drawable.person) // I will use 'person' for now if edit icon isn't found, but user should change it
            setColorFilter(whiteColor)
            setOnClickListener {
                val state = viewModel.uiState.value
                if (state is StaffDetailUiState.Success) {
                    staffFormViewModel.setEditMode(true, state.staff)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.manager_fragmentContainer, AddStaffFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

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
        if (data.profilePhotoUrl.isNotEmpty()) {
            binding.imgStaffAvatar.load(data.profilePhotoUrl) {
                crossfade(true)
                placeholder(R.drawable.person)
                error(R.drawable.person)
            }
        } else {
            binding.imgStaffAvatar.setImageResource(R.drawable.person)
        }

        binding.tvDetailName.text = data.staffName.ifEmpty { data.staffId }
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

        if (data.documentImageUrl.isNotEmpty()) {
            binding.btnViewDocProof.visibility = View.VISIBLE
            binding.btnViewDocProof.setOnClickListener {
                showDocumentImagePopup(data.documentImageUrl)
            }
        } else {
            binding.btnViewDocProof.visibility = View.GONE
        }
    }

    private fun showDocumentImagePopup(imageUrl: String) {
        val context = requireContext()
        val popupBinding = com.restroone.app.databinding.DialogImagePreviewBinding.inflate(LayoutInflater.from(context))

        popupBinding.ivPreview.load(imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_badge_24dp)
            error(R.drawable.ic_badge_24dp)
        }

        val dialog = AlertDialog.Builder(context)
            .setView(popupBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        popupBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

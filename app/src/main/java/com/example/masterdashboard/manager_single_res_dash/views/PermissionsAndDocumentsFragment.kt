package com.example.masterdashboard.manager_single_res_dash.views

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentPermissionsAndDocumentsBinding
import com.example.masterdashboard.manager_single_res_dash.uistate.FirebaseUiState
import com.example.masterdashboard.manager_single_res_dash.adapter.PermissionsDocumentsAdapter
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.Step2FormItem
import com.example.masterdashboard.utils.DocumentUploadManager
import com.example.masterdashboard.manager_single_res_dash.viewModel.StaffFormViewModel
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PermissionsAndDocumentsFragment : Fragment() {

    companion object {
        private const val TAG = "PermissionsAndDocumentsFragment"
    }

    private var _binding: FragmentPermissionsAndDocumentsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: StaffFormViewModel by activityViewModels()
    private lateinit var adapter: PermissionsDocumentsAdapter
    private lateinit var documentUploadManger: DocumentUploadManager
    private lateinit var formItems: List<Step2FormItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermissionsAndDocumentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: PermissionsAndDocumentsFragment Opened")

        documentUploadManger = DocumentUploadManager(this)

        setupToolbar()
        setupFormItemsList()
        setupObservers()

        binding.btnSubmit.setOnClickListener {
            handleSubmitAction()
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.perDocToolbar
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

    private fun setupFormItemsList() {
        formItems = listOf(
            Step2FormItem.Header,
            Step2FormItem.SectionTitle("Set Permissions", "Choose the access level for this staff member"),
            Step2FormItem.PermissionItem("dash_access", "Dashboard Access", "View dashboard and reports", R.drawable.person),
            Step2FormItem.PermissionItem("menu_access", "Menu Management", "Add / Edit menu items", R.drawable.person),
            Step2FormItem.PermissionItem("order_access", "Order Management", "Manage customer orders", R.drawable.person),
            Step2FormItem.PermissionItem("staff_access", "Staff Management", "Add / Edit staff details", R.drawable.person),
            Step2FormItem.PermissionItem("billing_access", "Billing & Payments", "Manage bills and payments", R.drawable.person),

            Step2FormItem.SectionTitle("Upload Documents", "Upload necessary documents for verification"),
            Step2FormItem.DocumentItem("aadhar", "Identity Verification Document *", "Upload Verification Copy", R.drawable.person),
            Step2FormItem.DocumentItem("pan", "Tax Card (Optional)", "Upload Identification Copy", R.drawable.person),
            Step2FormItem.DocumentItem("photo", "Photo *", "Upload Profile Photo", R.drawable.person),
            Step2FormItem.DocumentItem("address", "Address Proof (Optional)", "Upload Address Proof", R.drawable.person)
        )

        adapter = PermissionsDocumentsAdapter(formItems) { item, position ->
            documentUploadManger.selectDocument { selectedUri: Uri ->
                item.isUploaded = true
                item.fileUri = selectedUri
                Log.d(TAG, "ATTACHMENT REGISTERED: [FieldID: ${item.id} | UriPath: $selectedUri]")
                adapter.notifyItemChanged(position)
            }
        }

        binding.rvPermissionsDocuments.adapter = adapter
    }

    private fun handleSubmitAction() {
        val selectedPermissions = formItems.filterIsInstance<Step2FormItem.PermissionItem>()
            .filter { it.isChecked }
            .map { it.id }

        val identityDoc = formItems.filterIsInstance<Step2FormItem.DocumentItem>().find { it.id == "aadhar" }
        val photoDoc = formItems.filterIsInstance<Step2FormItem.DocumentItem>().find { it.id == "photo" }

        if (identityDoc?.isUploaded == false || photoDoc?.isUploaded == false) {
            Toast.makeText(requireContext(), "Please upload all mandatory documents (*)", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Register Staff?")
            .setMessage("Are you sure you want to register this staff member? Account credentials will be generated automatically upon confirmation.")
            .setPositiveButton("Yes, Register") { dialog, _ ->
                dialog.dismiss()
                val ownerUid = SessionManager(requireContext()).getUid()
                sharedViewModel.submitFinalStaffData(
                    ownerUid = ownerUid,
                    documentType = "National Identity Card Bundle",
                    documentNumber = "VERIFIED_AT_SUBMIT",
                    permission = selectedPermissions
                )
            }
            .setNegativeButton("Review Again", null)
            .show()
    }

    private fun showSuccessDialog(staffId: String, pass: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registration Successful!")
            .setMessage("Staff account created successfully. Please share these login credentials with the employee:\n\n" +
                    "👤 Staff Login ID: $staffId\n" +
                    "🔑 Password PIN : $pass\n\n" +
                    "Make sure they save these details securely.")
            .setCancelable(false)
            .setPositiveButton("Done") { dialog, _ ->
                dialog.dismiss()
                sharedViewModel.clearFormData()
                parentFragmentManager.popBackStack(AppConstants.BACKSTACK_ADD_STAFF, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            .show()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.uiState.collect { state ->
                    when (state) {
                        is FirebaseUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnSubmit.isEnabled = false
                            binding.btnSubmit.text = "Uploading..."
                        }
                        is FirebaseUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            val finalData = sharedViewModel.currentStaffData.value
                            showSuccessDialog(finalData.staffId, finalData.password)
                            sharedViewModel.resetState()
                        }
                        is FirebaseUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSubmit.isEnabled = true
                            binding.btnSubmit.text = "Submit"
                            Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            sharedViewModel.resetState()
                        }
                        is FirebaseUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSubmit.isEnabled = true
                            binding.btnSubmit.text = "Submit"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


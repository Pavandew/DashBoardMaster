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
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.Step2FormItem
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
        
        val isEditMode = sharedViewModel.isEditMode.value
        toolbar.tvToolbarTitle.text = if (isEditMode) "Edit Staff" else getString(R.string.add_staff)
        
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupFormItemsList() {
        val currentPermissions = sharedViewModel.currentStaffData.value.permissions
        val staffRole = sharedViewModel.currentStaffData.value.role.lowercase()
        val isEditMode = sharedViewModel.isEditMode.value

        fun hasPerm(key: String): Boolean {
            if (currentPermissions.isNotEmpty()) {
                return currentPermissions.contains(key)
            }
            // Smart defaults for NEW staff based on selected Role
            return when (key) {
                "order_access" -> true // All staff need order access
                "billing_access" -> staffRole.contains("cashier") || staffRole.contains("manager") || staffRole.contains("billing")
                "menu_access" -> staffRole.contains("manager") || staffRole.contains("master chef") || staffRole.contains("head chef") || staffRole.contains("head staff")
                "staff_access" -> staffRole.contains("manager")
                "dash_access" -> staffRole.contains("manager")
                else -> false
            }
        }

        formItems = listOf(
            Step2FormItem.Header,
            Step2FormItem.SectionTitle("Set Permissions", "Choose the access level for this staff member"),
            Step2FormItem.PermissionItem("dash_access", "Dashboard Access", "View dashboard and sales reports", R.drawable.ic_dashboard_24dp).apply {
                isChecked = hasPerm("dash_access")
            },
            Step2FormItem.PermissionItem("menu_access", "Menu Management", "Add / Edit dishes and prices", R.drawable.ic_restaurant_24dp).apply {
                isChecked = hasPerm("menu_access")
            },
            Step2FormItem.PermissionItem("order_access", "Order Management", "Take orders and manage tables", R.drawable.ic_order_approve_24dp).apply {
                isChecked = hasPerm("order_access")
            },
            Step2FormItem.PermissionItem("staff_access", "Staff Management", "Add / Edit staff and roles", R.drawable.ic_staffs_24dp).apply {
                isChecked = hasPerm("staff_access")
            },
            Step2FormItem.PermissionItem("billing_access", "Billing & Payments", "Generate bills and collect payments", R.drawable.ic_payments_24dp).apply {
                isChecked = hasPerm("billing_access")
            },

            Step2FormItem.SectionTitle("Upload Documents", "Upload necessary documents for verification"),
            Step2FormItem.DocumentItem("aadhar", "Identity Verification Document *", "Upload Verification Copy", R.drawable.ic_badge_24dp).apply {
                if (isEditMode) isUploaded = true
            },
            Step2FormItem.DocumentItem("pan", "Tax Card (Optional)", "Upload Identification Copy", R.drawable.ic_logs_24dp).apply {
                if (isEditMode) isUploaded = true
            },
            Step2FormItem.DocumentItem("photo", "Photo *", "Upload Profile Photo", R.drawable.ic_person_24dp).apply {
                if (isEditMode) isUploaded = true
            },
            Step2FormItem.DocumentItem("address", "Address Proof (Optional)", "Upload Address Proof", R.drawable.ic_inventory_24dp).apply {
                if (isEditMode) isUploaded = true
            }
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

        val isEditMode = sharedViewModel.isEditMode.value
        val dialogTitle = if (isEditMode) "Update Staff Profile?" else "Register Staff?"
        val dialogMsg = if (isEditMode) "Are you sure you want to save changes to this staff member's profile?"
                       else "Are you sure you want to register this staff member? Account credentials will be generated automatically upon confirmation."

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setMessage(dialogMsg)
            .setPositiveButton(if (isEditMode) "Update" else "Yes, Register") { dialog, _ ->
                dialog.dismiss()
                val ownerUid = SessionManager(requireContext()).getUid()
                sharedViewModel.submitFinalStaffData(
                    ownerUid = ownerUid,
                    documentType = if (isEditMode) sharedViewModel.currentStaffData.value.documentType else "National Identity Card Bundle",
                    documentNumber = if (isEditMode) sharedViewModel.currentStaffData.value.documentNumber else "VERIFIED_AT_SUBMIT",
                    permission = selectedPermissions
                )
            }
            .setNegativeButton("Review Again", null)
            .show()
    }

    private fun showSuccessDialog(staffId: String, pass: String) {
        val isEditMode = sharedViewModel.isEditMode.value
        val title = if (isEditMode) "Update Successful!" else "Registration Successful!"
        val message = if (isEditMode) "Staff profile has been updated successfully."
                     else "Staff account created successfully. Please share these login credentials with the employee:\n\n" +
                          "👤 Staff Login ID: $staffId\n" +
                          "🔑 Password PIN : $pass\n\n" +
                          "Make sure they save these details securely."

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Done") { dialog, _ ->
                dialog.dismiss()
                val wasEdit = sharedViewModel.isEditMode.value
                sharedViewModel.clearFormData()
                if (wasEdit) {
                    // If editing, pop back twice to reach list (or pop back once to reach detail which will reload)
                    // Better to pop back to the list to see the update
                    parentFragmentManager.popBackStack(AppConstants.BACKSTACK_ADD_STAFF, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    // Detail view is also in backstack, we might want to pop it too or let it stay.
                    // popBackStack("staff_detail_view", FragmentManager.POP_BACK_STACK_INCLUSIVE)
                } else {
                    parentFragmentManager.popBackStack(AppConstants.BACKSTACK_ADD_STAFF, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                }
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


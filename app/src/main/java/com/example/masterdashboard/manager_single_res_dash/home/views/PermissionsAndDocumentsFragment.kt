package com.example.masterdashboard.manager_single_res_dash.home.views

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
import com.example.masterdashboard.manager_single_res_dash.home.uistate.FirebaseUiState
import com.example.masterdashboard.manager_single_res_dash.home.adapter.PermissionsDocumentsAdapter
import com.example.masterdashboard.manager_single_res_dash.home.models.Step2FormItem
import com.example.masterdashboard.utils.DocumentUploadManager
import com.example.masterdashboard.manager_single_res_dash.home.viewModel.StaffFormViewModel
import com.example.masterdashboard.utils.AppConstants
import kotlinx.coroutines.launch

class PermissionsAndDocumentsFragment : Fragment() {

    companion object{
        private const val TAG = "PermissionsAndDocumentsFragment"
    }

    private var _binding: FragmentPermissionsAndDocumentsBinding? = null
    private val binding get() = _binding!!

    // Uses activityViewModels() to connect directly into the shared state tracking database bundle from Step 1
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

        // Initialize the upload manager utility explicitly
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
        // Build dataset explicit collection arrays containing structural groupings
        formItems = listOf(
            Step2FormItem.Header,
            Step2FormItem.SectionTitle("Set Permissions", "Choose the access level for this staff member"),
            Step2FormItem.PermissionItem("dash_access", "Dashboard Access", "View dashboard and reports", R.drawable.person),
            Step2FormItem.PermissionItem("menu_access", "Menu Management", "Add / Edit menu items", R.drawable.person),
            Step2FormItem.PermissionItem("order_access", "Order Management", "Manage customer orders", R.drawable.person),
            Step2FormItem.PermissionItem("staff_access", "Staff Management", "Add / Edit staff details", R.drawable.person),
            Step2FormItem.PermissionItem("billing_access", "Billing & Payments", "Manage bills and payments", R.drawable.person),

            Step2FormItem.SectionTitle("Upload Documents", "Upload necessary documents for verification"),
            Step2FormItem.DocumentItem("aadhar", "Aadhar Card *", "Upload Aadhar Card", R.drawable.person),
            Step2FormItem.DocumentItem("pan", "PAN Card (Optional)", "Upload PAN Card", R.drawable.person),
            Step2FormItem.DocumentItem("photo", "Photo *", "Upload Profile Photo", R.drawable.person),
            Step2FormItem.DocumentItem("address", "Address Proof (Optional)", "Upload Address Proof", R.drawable.person)
        )

        adapter = PermissionsDocumentsAdapter(formItems) { item, position ->
            // trigger permission, dialog selection sheet, camera capture, or storage picker instantly !
           documentUploadManger.selectDocument { selectedUri: Uri ->
               item.isUploaded = true
               item.fileUri = selectedUri // Cache local access path inside data model references

               Log.d(TAG, "ATTACHMENT REGISTERED: [FieldID: ${item.id} | UriPath: $selectedUri]")
               adapter.notifyItemChanged(position)
           }
        }

        binding.rvPermissionsDocuments.adapter = adapter
    }

    private fun handleSubmitAction() {
        // 1. Extract only checked permission IDs dynamically out of list items
        val selectedPermissions = formItems.filterIsInstance<Step2FormItem.PermissionItem>()
            .filter { it.isChecked }
            .map { it.id }

        // 2. Simple verification rules assertion matching mandatory documents check requirements
        val aadharDoc = formItems.filterIsInstance<Step2FormItem.DocumentItem>().find { it.id == "aadhar" }
        val photoDoc = formItems.filterIsInstance<Step2FormItem.DocumentItem>().find { it.id == "photo" }

        if (aadharDoc?.isUploaded == false || photoDoc?.isUploaded == false) {
            Toast.makeText(requireContext(), "Please upload all mandatory documents (*)", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Fire complete combined database submission requests directly downstream
        val ownerUid = com.example.masterdashboard.utils.SessionManager(requireContext()).getUid()
        sharedViewModel.submitFinalStaffData(
            ownerUid = ownerUid,
            documentType = "National Identity Card Bundle",
            documentNumber = "VERIFIED_AT_SUBMIT",
            permission = selectedPermissions
        )
    }

    private fun setupObservers() {


        // High-level safe Coroutines structure observing execution state results
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
                            Toast.makeText(requireContext(), "Staff added successfully!", Toast.LENGTH_SHORT).show()
                            sharedViewModel.resetState()

                            // Pop entire backstack sequence safely returning to staff listing screen
                            parentFragmentManager.popBackStack(AppConstants.BACKSTACK_ADD_STAFF, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
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
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
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentStaffDetailBinding
import com.example.masterdashboard.manager_single_res_dash.models.StaffDataModel
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.login.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore

class StaffDetailFragment : Fragment() {

    companion object {
        private const val TAG = "StaffDetailFragment_Debug"
    }

    private var _binding: FragmentStaffDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

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

        val staffDocId = arguments?.getString("STAFF_DOCUMENT_ID") ?: ""
        val ownerUid = SessionManager(requireContext()).getUid()

        Log.d(TAG, "onViewCreated: Received STAFF_DOCUMENT_ID = '$staffDocId', Owner UID = '$ownerUid'")

        if (staffDocId.isNotEmpty() && ownerUid.isNotEmpty()) {
            fetchStaffCompleteDetails(ownerUid, staffDocId)
        } else {
            Toast.makeText(context, "Error: Invalid Profile Identity Data Reference", Toast.LENGTH_SHORT).show()
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
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun fetchStaffCompleteDetails(ownerUid: String, docId: String) {
        binding.detailProgressBar.visibility = View.VISIBLE
        binding.detailScrollView.visibility = View.GONE

        val targetDocRef = db.collection(AppConstants.COLLECTION_USERS)
            .document(ownerUid)
            .collection(AppConstants.ROLE_STAFF)
            .document(docId)

        Log.i(TAG, "Firebase Fetch Request initiated for path: ${targetDocRef.path}")

        targetDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                binding.detailProgressBar.visibility = View.GONE
                binding.detailScrollView.visibility = View.VISIBLE

                if (documentSnapshot.exists()) {
                    val rawDataMap = documentSnapshot.data
                    Log.i(TAG, "🟢 Document snapshot received successfully. Raw Payload data Map from Firestore:")
                    rawDataMap?.forEach { (key, value) ->
                        Log.d(TAG, "   👉 Field Key: '$key' -> Raw Value: '$value' (Type: ${value?.javaClass?.simpleName})")
                    }

                    // Strategy 1: Attempt standard Firebase automatic reflection mapping mechanism
                    var staffDetail = documentSnapshot.toObject(StaffDataModel::class.java)

                    // Strategy 2 (Robust Fallback): If auto-mapping returned null due to structure/formatting issues, parse keys manually
                    if (staffDetail == null && rawDataMap != null) {
                        Log.w(TAG, "⚠️ Strategy 1 (Auto-mapping) failed. Executing custom type-safe parsing fallback map layout...")
                        try {
                            staffDetail = StaffDataModel(
                                id = documentSnapshot.id,
                                staffId = documentSnapshot.getString("staffId") ?: "",
                                password = documentSnapshot.getString("password") ?: "",
                                staffName = documentSnapshot.getString("staffName") ?: "",
                                mobile = documentSnapshot.getString("mobile") ?: "",
                                email = documentSnapshot.getString("email") ?: "",
                                gender = documentSnapshot.getString("gender") ?: "",
                                role = documentSnapshot.getString("role") ?: "",
                                department = documentSnapshot.getString("department") ?: "",
                                joiningDate = documentSnapshot.getString("joiningDate") ?: "",
                                shift = documentSnapshot.getString("shift") ?: "",
                                salary = documentSnapshot.getString("salary") ?: "",
                                status = documentSnapshot.getString("status") ?: "Active",
                                permissions = (documentSnapshot.get("permissions") as? List<*>)?.map { it.toString() } ?: emptyList(),
                                documentType = documentSnapshot.getString("documentType") ?: "",
                                documentNumber = documentSnapshot.getString("documentNumber") ?: ""
                            )
                            Log.i(TAG, "✅ Fallback parsing compiled successfully.")
                        } catch (ex: Exception) {
                            Log.e(TAG, "❌ Fallback parsing structure crashed entirely", ex)
                        }
                    }

                    if (staffDetail != null) {
                        Log.i(TAG, "🎉 Success: Finalized parsed data model instance configuration verified. Binding data objects to UI views.")
                        populateUI(staffDetail)
                    } else {
                        Log.e(TAG, "❌ Error: Parsing architecture failed to build model mapping payload on both strategies.")
                        Toast.makeText(context, "Profile Document is blank or corrupted.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "❌ Error: Firestore document path does not exist on servers.")
                    Toast.makeText(context, "Profile Document does not exist.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.detailProgressBar.visibility = View.GONE
                Log.e(TAG, "❌ Cloud transaction task completely crashed", e)
                Toast.makeText(context, "Cloud read execution error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateUI(data: StaffDataModel) {
        // Header Overview Card Info
        binding.tvDetailName.text = data.staffName
        binding.tvDetailRoleAndId.text = "${data.role} • ID: ${data.staffId}"
        binding.tvDetailStatusTag.text = data.status

        // Section 1: Employment Details
        binding.tvDetailJoinDate.text = if(data.joiningDate.isNotEmpty()) data.joiningDate else "Not Specified"
        binding.tvDetailDepartment.text = if(data.department.isNotEmpty()) data.department else "General"
        binding.tvDetailShift.text = if(data.shift.isNotEmpty()) data.shift else "Not Allocated"
        binding.tvDetailSalary.text = if(data.salary.isNotEmpty()) "₹ ${data.salary}" else "Confidential"

        // Section 2: Contact Details
        binding.tvDetailMobile.text = if(data.mobile.isNotEmpty()) data.mobile else "N/A"
        binding.tvDetailEmail.text = if(data.email.isNotEmpty()) data.email else "N/A"
        binding.tvDetailGender.text = data.gender
//        binding.tvDetailPassword.text = if(data.password.isNotEmpty()) data.password else "No Password Set"

        // Section 3: Identity Documents Details
        binding.tvDetailDocType.text = if(data.documentType.isNotEmpty()) data.documentType else "Official ID Card"
        binding.tvDetailDocNumber.text = if(data.documentNumber.isNotEmpty()) data.documentNumber else "Unverified/Missing"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
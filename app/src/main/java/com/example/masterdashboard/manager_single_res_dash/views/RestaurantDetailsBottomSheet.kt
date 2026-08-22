package com.example.masterdashboard.manager_single_res_dash.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.databinding.BottomSheetRestaurantDetailsBinding
import com.example.masterdashboard.manager_single_res_dash.form_screen.adapter.FormAdapter
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.FormItem
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.RegistrationDataModel
import com.example.masterdashboard.manager_single_res_dash.uistate.RestaurantDetailsUiState
import com.example.masterdashboard.manager_single_res_dash.viewModel.RestaurantDetailsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class RestaurantDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRestaurantDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RestaurantDetailsViewModel by viewModels()
    private var ownerUid: String = ""

    companion object {
        private const val ARG_OWNER_UID = "arg_owner_uid"
        private const val TAG = "ResDetailsBS"

        fun newInstance(ownerUid: String): RestaurantDetailsBottomSheet {
            val fragment = RestaurantDetailsBottomSheet()
            val args = Bundle()
            args.putString(ARG_OWNER_UID, ownerUid)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ownerUid = arguments?.getString(ARG_OWNER_UID) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRestaurantDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Bottom Sheet Opened for Owner UID: $ownerUid")
        
        binding.btnClose.setOnClickListener { dismiss() }
        
        setupRecyclerView()
        observeViewModel()

        if (ownerUid.isNotEmpty()) {
            viewModel.fetchRestaurantDetails(ownerUid)
        } else {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        binding.rvDetails.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is RestaurantDetailsUiState.Loading -> { }
                    is RestaurantDetailsUiState.Success -> {
                        displayData(state.data)
                    }
                    is RestaurantDetailsUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun displayData(data: RegistrationDataModel) {
        val reviewItems = mutableListOf<FormItem>()

        // 1. OWNER CARD - Using unified getters to handle old and new data formats
        reviewItems.add(FormItem.ReviewCard(
            title = "OWNER INFORMATION",
            details = listOf(
                "Name" to data.getUnifiedFullName(),
                "Email" to data.getUnifiedEmail(),
                "Mobile" to data.getUnifiedMobile()
            ),
            onEditClick = { }
        ))

        // 2. RESTAURANT CARD
        reviewItems.add(FormItem.ReviewCard(
            title = "RESTAURANT DETAILS",
            details = listOf(
                "Name" to data.restaurantName,
                "Type" to data.businessType,
                "Legal Name" to data.legalName,
                "Display Name" to data.displayName
            ),
            onEditClick = { }
        ))

        // 3. LOCATION CARD
        reviewItems.add(FormItem.ReviewCard(
            title = "LOCATION",
            details = listOf(
                "Address" to "${data.address}, ${data.city}, ${data.state}",
                "PIN Code" to data.pinCode,
                "Contact" to data.contactNumber
            ),
            onEditClick = { }
        ))

        // 4. COMPLIANCE & BILLING (Filtered: No PAN, No Currency as requested)
        reviewItems.add(FormItem.ReviewCard(
            title = "TAX & BILLING",
            details = listOf(
                "GST Number" to data.gstNumber,
                "FSSAI No." to data.fssaiNumber,
                "Inv Prefix" to data.invoicePrefix,
                "Tax Rate" to "${data.defaultTaxRate}%"
            ),
            onEditClick = { }
        ))

        binding.rvDetails.adapter = FormAdapter(reviewItems) { _, _ -> }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

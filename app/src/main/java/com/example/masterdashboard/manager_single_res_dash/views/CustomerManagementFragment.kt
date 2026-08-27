package com.example.masterdashboard.manager_single_res_dash.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentCustomerManagementBinding
import com.example.masterdashboard.manager_single_res_dash.adapter.CustomerAdapter
import com.example.masterdashboard.manager_single_res_dash.viewModel.CustomerViewModel
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

class CustomerManagementFragment : Fragment() {

    companion object {
        private const val TAG = "CustomerManagementFrag"
    }

    private var _binding: FragmentCustomerManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomerViewModel by viewModels()
    private lateinit var customerAdapter: CustomerAdapter
    private val sessionManager by lazy { SessionManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Customer Management Screen opened")

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        observeViewModel()

        val managerId = sessionManager.getUid()
        if (managerId.isNotEmpty()) {
            viewModel.observeCustomers(managerId)
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.customerToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = "Customer Insights"
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setImageResource(R.drawable.ic_arrow_back_24dp)
        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        customerAdapter = CustomerAdapter { customer ->
            Log.d(TAG, "Customer clicked: ${customer.customerName}")
            // Optional: Navigate to Customer Details/History
        }

        binding.rvCustomers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = customerAdapter
        }
    }

    private fun setupSearch() {
        binding.searchBar.etSearchOrder.hint = "Search by name or phone..."
        binding.searchBar.etSearchOrder.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 1. Update List
                    customerAdapter.submitList(state.customers)

                    // 2. Handle Loader
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // 3. Handle Empty State (Only show if NOT loading and list is empty)
                    val showEmpty = !state.isLoading && state.customers.isEmpty()
                    binding.tvEmptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.masterdashboard.staff_dash.home.table.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // Explicitly scoped to parent Activity lifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentViewCartDetailsBinding
import com.example.masterdashboard.staff_dash.home.StaffHomeActivity
import com.example.masterdashboard.staff_dash.home.table.adapter.FoodMenuAdapter
import com.example.masterdashboard.staff_dash.home.table.viewModels.OrderViewModel
import kotlinx.coroutines.launch

class ViewCartDetailsFragment : Fragment() {

    companion object {
        private const val TAG = "ViewCartDetailsFragment"
    }

    private var _binding: FragmentViewCartDetailsBinding? = null
    private val binding get() = _binding!!

    // FIXED: Clean initialization without repeating factory parameter logic.
    // This safely pulls the pre-existing state engine straight from the activity baseline layer.
    private val viewModel: OrderViewModel by activityViewModels()

    private lateinit var cartAdapter: FoodMenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewCartDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupClickListener()
        setupRecyclerView()
        observeCartStateFlow()
    }

    override fun onStart() {
        super.onStart()
        // Kept inside lifecycle start to guarantee structural coverage across container resizes
        (activity as? StaffHomeActivity)?.hideBottomNavigation()
    }

    private fun setupToolbar() {
        val tableId = arguments?.getString("tableId") ?: "Unknown Table"

        val toolbar = binding.viewCartToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.cart) + " - Table $tableId"
        toolbar.tvToolbarEndText.visibility = View.VISIBLE
        toolbar.toolbarImgNotification.visibility = View.GONE

        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        toolbar.tvToolbarEndText.setOnClickListener {
            Log.d(TAG, "setupToolbar: Clear Cart clicked for Table $tableId")
            viewModel.clearCart()
        }
    }

    private fun setupClickListener() {
        binding.btnSendToKitchen.setOnClickListener {
            Log.d(TAG, "Send to Kitchen clicked: Dispatching metrics validation workflows.")
            Toast.makeText(context, "Order sent to kitchen!", Toast.LENGTH_SHORT).show()

            val tableId = arguments?.getString("tableId") ?: "N/A"
            val currentState = viewModel.uiState.value

            val selectedCartItems = currentState.menuItems.filter { it.currentQuantity > 0 }
            val totalCount = selectedCartItems.sumOf { it.currentQuantity }
            val subtotal = currentState.cartSummary.totalPrice
            val grandTotal = subtotal + (subtotal * 0.05)

            val successBundle = Bundle().apply {
                putString("tableId", tableId)
                putInt("totalItems", totalCount)
                putDouble("totalPrice", grandTotal)
            }

            val successFragment = OrderSuccessFragment().apply {
                arguments = successBundle
            }

            // Clean transaction routing target verified against your app context
            parentFragmentManager.beginTransaction()
                .replace(this@ViewCartDetailsFragment.id, successFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = FoodMenuAdapter(
            onQuantityIncreased = { viewModel.updateItemQuantity(it.id, true) },
            onQuantityDecreased = { viewModel.updateItemQuantity(it.id, false) }
        )
        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = cartAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeCartStateFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val selectedCartItems = state.menuItems.filter { it.currentQuantity > 0 }

                    Log.d(TAG, "observeCartStateFlow: Updating cart with ${selectedCartItems.size} items")
                    cartAdapter.submitList(selectedCartItems)

                    // Financial Aggregations
                    val subtotal = state.cartSummary.totalPrice
                    val gst = subtotal * 0.05
                    val grandTotal = subtotal + gst

                    binding.tvSubtotalPrice.text = getString(R.string.currency_symbol) + " $subtotal"
                    binding.tvGstPrice.text = getString(R.string.currency_symbol) + " ${String.format("%.2f", gst)}"
                    binding.tvGrandTotalPrice.text = getString(R.string.currency_symbol) + " ${String.format("%.2f", grandTotal)}"

                    if (selectedCartItems.isEmpty()) {
                        Log.d(TAG, "observeCartStateFlow: Cart is empty, navigating back to home screen layouts automatically.")
                        parentFragmentManager.popBackStack()
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
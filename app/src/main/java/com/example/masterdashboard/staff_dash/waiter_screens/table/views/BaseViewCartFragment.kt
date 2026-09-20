package com.example.masterdashboard.staff_dash.waiter_screens.table.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.databinding.FragmentViewCartDetailsBinding
import com.example.masterdashboard.utils.SessionManager
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.ViewCartDetailAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import com.example.masterdashboard.utils.TaxCalculator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Base logic for the Cart screen.
 * Displays selected items, calculates GST/Total, and handles cart clearance.
 * Shared between Waiter (KOT submission) and Cashier (Quick-Pay) roles.
 */
abstract class BaseViewCartFragment : Fragment() {

    protected var _binding: FragmentViewCartDetailsBinding? = null
    protected val binding get() = _binding!!

    protected val sessionManager by lazy { SessionManager(requireContext()) }
    
    // Shared activity-scoped ViewModel to access the current session cart
    protected val viewModel: OrderTakingViewModel by activityViewModels()

    private lateinit var cartAdapter: ViewCartDetailAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentViewCartDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("CartFrag", "onViewCreated: Cart screen opened.")
        
        // Notify the ViewModel that we are in cart mode to prevent session reset on back-navigation
        viewModel.isViewingCart = true
        
        setupToolbar()
        setupRecyclerView()
        observeCartStateFlow()
        setupBottomButton()
    }

    /**
     * Abstract button setup to be implemented by role-specific cart fragments.
     */
    abstract fun setupBottomButton()

    private fun setupToolbar() {
        val tableName = arguments?.getString("tableName") ?: "Unknown"
        binding.viewCartToolbar.tvToolbarTitle.text = "Cart - Table $tableName"
        
        // Back navigation
        binding.viewCartToolbar.toolbarImgMenu.setOnClickListener { parentFragmentManager.popBackStack() }
        
        // Clear Cart feature
        binding.viewCartToolbar.tvToolbarEndText.visibility = View.VISIBLE
        binding.viewCartToolbar.tvToolbarEndText.setOnClickListener {
            Log.d("CartFrag", "User action: Clear Cart clicked.")
            viewModel.clearCart()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = ViewCartDetailAdapter()
        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = cartAdapter
        }
    }

    /**
     * Observes the lightweight cart summary to update the financial breakdown.
     */
    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun observeCartStateFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe the lightweight cart summary instead of the full heavy UI state
                viewModel.cartSummary.collectLatest { summary ->
                    Log.v("CartFrag", "Cart Summary Update: ${summary.totalItems} items, Total: ₹${summary.totalPrice}")
                    
                    // Filter and display only items that have a positive quantity
                    val itemsInCart = viewModel.originalFoodList.value.filter { it.currentQuantity > 0 }

                    // Sort: New items and items with new additions float to the VERY TOP, previously sent items sit at the BOTTOM
                    val sortedCartItems = itemsInCart.sortedWith(
                        compareByDescending<FoodItemData> {
                            it.currentQuantity > it.previousQuantity 
                        }.thenByDescending { 
                            it.previousQuantity == 0 
                        }.thenBy { 
                            it.name 
                        }
                    )

                    cartAdapter.submitList(sortedCartItems)

                    // Financial Calculations (Base Price + Configured Tax)
                    val subtotal = summary.totalPrice.toDouble()
                    val sessionManager = SessionManager(requireContext())
                    val taxInfo = TaxCalculator(sessionManager).calculateTax(subtotal)

                    binding.tvSubtotalPrice.text = "₹ ${subtotal.toInt()}"
                    binding.tvGstLabel.text = "${taxInfo.taxLabel} (${String.format("%.1f", taxInfo.taxRate)}%)"
                    binding.tvGstPrice.text = "₹ ${String.format("%.2f", taxInfo.taxAmount)}"
                    binding.tvGrandTotalPrice.text = "₹ ${String.format("%.2f", taxInfo.grandTotal)}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.v("CartFrag", "onDestroyView: Releasing View Binding.")
        _binding = null
    }
}

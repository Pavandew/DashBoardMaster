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
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentViewCartDetailsBinding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.ViewCartDetailAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import kotlinx.coroutines.launch

/**
 * Base logic for the Cart screen.
 */
abstract class BaseViewCartFragment : Fragment() {

    protected var _binding: FragmentViewCartDetailsBinding? = null
    protected val binding get() = _binding!!

    protected val sessionManager by lazy { SessionManager(requireContext()) }
    protected val viewModel: OrderTakingViewModel by activityViewModels()

    private lateinit var cartAdapter: ViewCartDetailAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentViewCartDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.isViewingCart = true
        setupToolbar()
        setupRecyclerView()
        observeCartStateFlow()
        setupBottomButton()
    }

    abstract fun setupBottomButton()

    private fun setupToolbar() {
        val tableName = arguments?.getString("tableName") ?: "Unknown"
        binding.viewCartToolbar.tvToolbarTitle.text = "Cart - Table $tableName"
        binding.viewCartToolbar.toolbarImgMenu.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.viewCartToolbar.tvToolbarEndText.visibility = View.VISIBLE
        binding.viewCartToolbar.tvToolbarEndText.setOnClickListener {
            viewModel.clearCart()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = ViewCartDetailAdapter()
        binding.rvCartItems.layoutManager = LinearLayoutManager(context)
        binding.rvCartItems.adapter = cartAdapter
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun observeCartStateFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val items = viewModel.originalFoodList.filter { it.currentQuantity > 0 }
                    cartAdapter.submitList(items)

                    val subtotal = state.cartSummary.totalPrice
                    val gst = subtotal * 0.05
                    val total = subtotal + gst

                    binding.tvSubtotalPrice.text = "₹ $subtotal"
                    binding.tvGstPrice.text = "₹ ${String.format("%.2f", gst)}"
                    binding.tvGrandTotalPrice.text = "₹ ${String.format("%.2f", total)}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
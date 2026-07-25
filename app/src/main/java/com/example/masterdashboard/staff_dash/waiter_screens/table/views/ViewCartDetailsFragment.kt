package com.example.masterdashboard.staff_dash.waiter_screens.table.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentViewCartDetailsBinding
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.staff_dash.waiter_screens.WaiterHomeActivity
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.ViewCartDetailAdapter
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels.OrderTakingViewModel
import kotlinx.coroutines.launch

class ViewCartDetailsFragment : Fragment() {
    companion object {
        private const val TAG = "ViewCartDetailsFragment"
    }

    private var _binding: FragmentViewCartDetailsBinding? = null
    private val binding get() = _binding!!

    private val sessionManager by lazy { SessionManager(requireContext()) }

    // Safely shares the persistent state machine instance straight from the parent activity layer
    private val viewModel: OrderTakingViewModel by activityViewModels()

    private lateinit var cartAdapter: ViewCartDetailAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewCartDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "📱 [FRAGMENT] ViewCartDetailsFragment Opened")

        // Informs the shared ViewModel that we are browsing the checkout state to protect variables
        viewModel.isViewingCart = true

        setupToolbar()
        setupClickListener()
        setupRecyclerView()
        observeCartStateFlow()
    }

    override fun onStart() {
        super.onStart()
        (activity as? WaiterHomeActivity)?.hideBottomNavigation()
    }

    private fun setupToolbar() {
        val tableName = arguments?.getString("tableName") ?: "Unknown Table"

        val toolbar = binding.viewCartToolbar
        toolbar.tvToolbarTitle.text = getString(R.string.cart) + " - Table $tableName"
        toolbar.tvToolbarEndText.visibility = View.VISIBLE
        toolbar.toolbarImgNotification.visibility = View.GONE

        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        toolbar.tvToolbarEndText.setOnClickListener {
            Log.d(TAG, "📱 [FRAGMENT] Clear Cart clicked for Table $tableName")
            viewModel.clearCart()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupClickListener() {
        binding.btnSendToKitchen.setOnClickListener {
            Log.d(TAG, "📱 [FRAGMENT] Send to Kitchen clicked: Initializing backend upload pipeline.")

            val managerId = sessionManager.getUid()
            val managerName = sessionManager.getUserName() ?: "Unknown Manager"
            val tableId = arguments?.getString("tableId") ?: "N/A"
            val floorId = arguments?.getString("floorId") ?: "N/A"

            val specialNotes = binding.etOrderNotes.text.toString().trim()
            Log.d(TAG, "📱 [FRAGMENT] Session Context: [ManagerName: $managerName | ID: $managerId]")
            Log.d(TAG, "📱 [FRAGMENT] Dispatching Path Check: managers/$managerId/floors/$floorId/tables/$tableId")

            // Trigger the remote Firebase write method directly inside our shared state engine
            viewModel.submitActiveOrderToKitchen(managerId, floorId, tableId, specialNotes)
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = ViewCartDetailAdapter()
        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = cartAdapter
            setHasFixedSize(true)
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun observeCartStateFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Pipeline 1: Track UI layout changes, total calculations, and item listings
                launch {
                    viewModel.uiState.collect { state ->
                        val selectedCartItems = viewModel.originalFoodList.filter { it.currentQuantity > 0 }

                        Log.d(TAG, "📱 [FRAGMENT] Updating cart layout view with ${selectedCartItems.size} items")
                        cartAdapter.submitList(selectedCartItems)

                        val subtotal = state.cartSummary.totalPrice
                        val gst = subtotal * 0.05
                        val grandTotal = subtotal + gst

                        binding.tvSubtotalPrice.text = getString(R.string.currency_symbol) + " $subtotal"
                        binding.tvGstPrice.text = getString(R.string.currency_symbol) + " ${String.format("%.2f", gst)}"
                        binding.tvGrandTotalPrice.text = getString(R.string.currency_symbol) + " ${String.format("%.2f", grandTotal)}"
                    }
                }

                // Pipeline 2: Track active transactional progress updates coming from Firestore
                launch {
                    viewModel.orderUploadStatus.collect { resource ->
                        when (resource) {
                            is ResourceUiState.Loading -> {
                                binding.btnSendToKitchen.isEnabled = false
                                binding.btnSendToKitchen.text = "Sending Order..."
                            }
                            is ResourceUiState.Success -> {
                                Log.i(TAG, "📱 [FRAGMENT] KOT successfully created. Navigating to Success layout screen.")
                                Toast.makeText(context, "Order sent to kitchen!", Toast.LENGTH_SHORT).show()

                                val tableName = arguments?.getString("tableName") ?: "N/A"
                                val tableId = arguments?.getString("tableId") ?: "N/A"
                                val selectedCartItems = viewModel.originalFoodList.filter { it.currentQuantity > 0 }
                                val totalCount = selectedCartItems.sumOf { it.currentQuantity }
                                val subtotal = viewModel.uiState.value.cartSummary.totalPrice
                                val grandTotal = subtotal + (subtotal * 0.05)

                                val successBundle = Bundle().apply {
                                    putString("tableId", tableId)
                                    putString("tableName", tableName)
                                    putString("orderId", viewModel.lastOrderId)
                                    putInt("totalItems", totalCount)
                                    putDouble("totalPrice", grandTotal)
                                }

                                val successFragment = OrderSuccessFragment().apply {
                                    arguments = successBundle
                                }
                                // Trigger the UI navigation block safely while upload status is SUCCESS
                                parentFragmentManager.beginTransaction()
                                    .replace(this@ViewCartDetailsFragment.id, successFragment)
                                    .addToBackStack(null)
                                    .commit()
                            }
                            is ResourceUiState.Error -> {
                                binding.btnSendToKitchen.isEnabled = true
                                binding.btnSendToKitchen.text = "Send to Kitchen"
                                Toast.makeText(context, "Upload Failed: ${resource.message}", Toast.LENGTH_LONG).show()
                                viewModel.resetUploadStatus()
                            }
                            else -> {}
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
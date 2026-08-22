package com.example.masterdashboard.staff_dash.kitchen_screens.views

import android.os.Bundle
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.masterdashboard.databinding.FragmentKitchenInventoryBinding
import com.example.masterdashboard.staff_dash.kitchen_screens.adapter.KitchenInventoryAdapter
import com.example.masterdashboard.staff_dash.kitchen_screens.model.InventoryItem
import com.example.masterdashboard.staff_dash.kitchen_screens.viewModel.KitchenInventoryViewModel
import com.example.masterdashboard.staff_dash.kitchen_screens.utils.KitchenDialogHelper
import com.example.masterdashboard.staff_dash.waiter_screens.table.adapter.FloorChipsAdapter
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KitchenInventoryFragment : Fragment() {

    companion object {
        private const val TAG = "KitchenInventoryFrag"
    }

    private var _binding: FragmentKitchenInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KitchenInventoryViewModel by viewModels()
    private lateinit var adapter: KitchenInventoryAdapter
    private lateinit var filterAdapter: FloorChipsAdapter
    private lateinit var sessionManager: SessionManager
    private var restaurantId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKitchenInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        
        restaurantId = sessionManager.getRestaurantId().ifEmpty { sessionManager.getUid() }
        
        Log.d(TAG, "onViewCreated: Initialized for Restaurant: $restaurantId")

        val userRole = sessionManager.getRole().lowercase()
        val canEdit = userRole == "manager" || userRole == "chef" || userRole == "owner_single" || userRole == "owner_multi"

        setupToolbar()
        setupFilters()
        setupRecyclerView(canEdit)
        setupSearchBar()
        observeViewModel()

        if (canEdit) {
            binding.fabAddItem.visibility = View.VISIBLE
            binding.fabAddItem.setOnClickListener {
                triggerAddEditFlow(null)
            }
        } else {
            binding.fabAddItem.visibility = View.GONE
        }

        if (restaurantId.isNotEmpty()) {
            viewModel.fetchInventory(restaurantId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.tvToolbarTitle.text = "Inventory Management"
        binding.toolbar.toolbarImgMenu.visibility = View.GONE
    }

    private fun setupFilters() {
        filterAdapter = FloorChipsAdapter { chip ->
            val filterName = chip.name.substringBefore(" (").trim()
            viewModel.setFilter(filterName)
        }
        binding.rvFilters.adapter = filterAdapter
    }

    private fun setupRecyclerView(canEdit: Boolean) {
        adapter = KitchenInventoryAdapter { item ->
            if (canEdit) {
                triggerAddEditFlow(item)
            } else {
                Toast.makeText(context, "View Only Mode", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvInventory.adapter = adapter
    }

    private fun setupSearchBar() {
        binding.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.inventoryState.collectLatest { (items, filters) ->
                adapter.submitList(items)
                filterAdapter.submitList(filters)
                binding.tvEmptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun triggerAddEditFlow(item: InventoryItem?) {
        val currentCategories = viewModel.categories.value
        
        KitchenDialogHelper.showInventoryAddEditDialog(
            context = requireContext(),
            item = item,
            categories = currentCategories,
            onSave = { updatedItem ->
                val staffName = sessionManager.getUserName() ?: "Staff"
                if (item == null) {
                    viewModel.addItem(restaurantId, updatedItem, staffName)
                } else {
                    viewModel.updateItem(restaurantId, updatedItem, staffName)
                }
            },
            onDelete = { inventoryId ->
                viewModel.deleteItem(restaurantId, inventoryId)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

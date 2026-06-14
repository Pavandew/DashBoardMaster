package com.example.masterdashboard.manager_single_res_dash.home.views

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentMenuItemListBinding
import com.example.masterdashboard.manager_single_res_dash.home.adapter.FoodItemListAdapter
import com.example.masterdashboard.manager_single_res_dash.home.models.MenuFoodItemsData
import com.example.masterdashboard.manager_single_res_dash.home.uistate.MenuItemUiState
import com.example.masterdashboard.manager_single_res_dash.home.viewModel.MenuItemViewModel
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.MenuDialogHelper
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MenuItemListFragment : Fragment() {
    companion object {
        private const val TAG = "FoodItemListFragment"
    }

    private var _binding: FragmentMenuItemListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MenuItemViewModel by viewModels()
    private lateinit var foodItemListAdapter: FoodItemListAdapter
    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val userRole by lazy { sessionManager.getRole() }

    private var categoryId: String = ""
    private var categoryName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString("CATEGORY_ID", "")
            categoryName = it.getString("CATEGORY_NAME", "Items")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: MenuItemListFragment Opened")
        Log.i(TAG, "Navigation: Entered MenuItemListFragment for Category: $categoryName (ID: $categoryId)")

        setupToolbar()
        setupRecyclerView()
        setupRoleBasedAccess()
        setupOnClickActions()

        val ownerUid = sessionManager.getUid()
        if (ownerUid.isNotEmpty() && categoryId.isNotEmpty()) {
            Log.d(TAG, "Streaming food subcollection path elements inside: $categoryName")
            viewModel.observeFoodItems(ownerUid, categoryId)
        } else {
            Log.e(TAG, "Session configuration failure. Missing parameters context keys tokens.")
        }

        observeFoodItemsStream()
    }

    private fun setupToolbar() {
        val toolbar = binding.foodItemsToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))

        // FIXED: Displaying the dynamic category name ("Pizza") instead of static global header text
        toolbar.tvToolbarTitle.text = categoryName
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        foodItemListAdapter = FoodItemListAdapter(
            onItemClick = { selectedFoodItem ->
                Log.d(TAG, "Selected food item: ${selectedFoodItem.itemName} -> ID: ${selectedFoodItem.id}")
                Toast.makeText(requireContext(), "Edit: ${selectedFoodItem.itemName}", Toast.LENGTH_SHORT).show()
            },
            onItemLongClick = { targetFoodItem ->
                // triggers when user hold down on any dish item card row
                if(userRole != AppConstants.ROLE_STAFF) {
                    showDeleteConfirmationPopup(targetFoodItem)
                } else {
                    Log.w(TAG, "Action Denied: Staff roles are unauthorized to delete menu items.")
                }
            }
        )

        binding.rvFoodItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = foodItemListAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupRoleBasedAccess() {
        // hide add button  at runtime if accessed via Staff Profile
        if(userRole == AppConstants.ROLE_STAFF) {
            Log.i(TAG, "Staff member role detected. Applying layout view restrictions.")
            binding.btnAddNewItem.visibility = View.GONE
        }
    }

    private fun setupOnClickActions() {
        // fetch the user's role from SessionManager
        binding.btnAddNewItem.setOnClickListener {
            Log.d(TAG, "🔘 '+ Add New Item' button clicked. Forwarding keys to AddMenuItemFragment")

            // FIXED: Creating a brand new argument bundle to pass data downstream to the creation form screen
            val addMenuItemFragment = AddMenuItemFragment().apply {
                arguments = Bundle().apply {
                    putString("CATEGORY_ID", categoryId)
                    putString("CATEGORY_NAME", categoryName)
                }
            }

            Log.i(TAG, "Navigation: Transitioning to AddMenuItemFragment for Category: $categoryName")

            // UNCOMMENTED: Triggering transaction to display Screen 3 layout
            parentFragmentManager.beginTransaction()
                .replace(R.id.manager_fragmentContainer, addMenuItemFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observeFoodItemsStream() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.foodItemsState.collect { state ->
                    when (state) {
                        is MenuItemUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvFoodItems.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }
                        is MenuItemUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvFoodItems.visibility = View.VISIBLE
                            binding.tvEmptyState.visibility = View.GONE
                            Log.i(TAG, "Success: Received updates. Population list count: ${state.foodList.size}")
                            foodItemListAdapter.submitList(state.foodList)
                        }
                        is MenuItemUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvFoodItems.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                            Log.w(TAG, "Returned empty structural subcollection models array data blocks sets.")
                            foodItemListAdapter.submitList(emptyList())
                        }
                        is MenuItemUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                            Log.e(TAG, " Failed parsing stream pipeline values context profiles: ${state.message}")
                            Toast.makeText(requireContext(), "Error linking menu items matching database references: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationPopup(item: MenuFoodItemsData) {
        val ownerUid = sessionManager.getUid()

        // Clean & Reusable Utility Call!
        MenuDialogHelper.showDeleteConfirmation(
            context = requireContext(),
            title = "Delete Menu Item?",
            message = "Are you sure you want to permanently remove \"${item.itemName}\" from the menu? This action cannot be undone.",
            onConfirm = {
                if (ownerUid.isNotEmpty() && categoryId.isNotEmpty()) {
                    // OPTIMISTIC UI FIX: Create a temporary list excluding the deleted item
                    val currentList = foodItemListAdapter.currentList.toMutableList()
                    val indexToRemove = currentList.indexOfFirst { it.id == item.id }

                    if (indexToRemove != -1) {
                        currentList.removeAt(indexToRemove)
                        Log.d(TAG, "Optimistic UI: Instantly sliding '${item.itemName}' out of active view memory layout.")
                        // Submit the reduced list immediately so it vanishes from the UI instantly
                        foodItemListAdapter.submitList(currentList)
                    }

                    // Execute the explicit Firebase cloud transaction task
                    viewModel.deleteMenuFoodItem(ownerUid, categoryId, item.id, item.itemName)
                    Toast.makeText(requireContext(), "${item.itemName} removed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
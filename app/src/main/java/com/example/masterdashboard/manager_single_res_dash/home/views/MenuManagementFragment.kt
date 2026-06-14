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
import com.example.masterdashboard.databinding.FragmentMenuManagementBinding
import com.example.masterdashboard.manager_single_res_dash.home.adapter.MenuCategoryAdapter
import com.example.masterdashboard.manager_single_res_dash.home.models.MenuCategory
import com.example.masterdashboard.manager_single_res_dash.home.uistate.MenuUiState
import com.example.masterdashboard.manager_single_res_dash.home.utils.AddCategoryBottomSheet
import com.example.masterdashboard.manager_single_res_dash.home.viewModel.MenuManagementViewModel
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.MenuDialogHelper
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

class MenuManagementFragment : Fragment() {
    companion object {
        private const val TAG = AppConstants.TAG_MENU_MANAGEMENT
    }

    private var _binding: FragmentMenuManagementBinding? = null
    private val binding get() = _binding !!

    private val viewModel: MenuManagementViewModel by viewModels()
    private lateinit var menuAdapter: MenuCategoryAdapter
    private val sessionManager by lazy { SessionManager(requireContext()) }

private val userRole by lazy{ sessionManager.getRole() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMenuManagementBinding.inflate(inflater, container, false)
        return binding.root
     }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: MenuManagementFragment Opened")
        Log.i(TAG, "Navigation: Entered MenuManagementFragment")

        setupToolbar()
        setupRecyclerView()
        setupRoleBasedAccess()
        setupPermissionAndAction()

        // fetch values via active session manager key token
        val ownerUid = sessionManager.getUid()
        if (ownerUid.isNotEmpty()) {
            viewModel.observeCategories(ownerUid)
        }
        observeMenuChanges()
    }

    private fun setupToolbar() {
        val toolbar = binding.menuManToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = AppConstants.TITLE_MENU_MANAGEMENT
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuCategoryAdapter(
            onItemClick = { selectedCategory ->
                Log.d(TAG, "Selected category: ${selectedCategory.name} -> ID: ${selectedCategory.id}")

                // Create a new instance of FoodItemListFragment and bundle arguments
                val menuItemListFragment = MenuItemListFragment().apply {
                    arguments = Bundle().apply {
                        putString("CATEGORY_ID", selectedCategory.id)
                        putString("CATEGORY_NAME", selectedCategory.name)
                    }
                }

                Log.i(TAG, "Navigation: Transitioning to MenuItemListFragment for Category: ${selectedCategory.name}")

                // Navigate over to FoodItemListFragment with the data bundle attached
                parentFragmentManager.beginTransaction()
                    .replace(R.id.manager_fragmentContainer, menuItemListFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onItemLongClick = { targetFoodItem ->
                // triggers when user hold down on any dish item card row
                if(userRole != AppConstants.ROLE_STAFF) {
                    showCategoryDeletePopup(targetFoodItem)
                } else {
                    Log.w(TAG, "Action Denied: Staff roles are unauthorized to delete menu items.")
                }
            }
        )

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = menuAdapter
            setHasFixedSize(true)
        }
    }
    private fun setupRoleBasedAccess() {
        // Renamed text to display "Add Category" dynamically matching button requirement
        binding.btnAddCategory.text = AppConstants.BTN_ADD_CATEGORY

        // ROLE SECURITY: Hide the Add Button immediately if accessed via Staff Profile
        if (userRole == AppConstants.ROLE_STAFF) {
            Log.i(TAG, "Staff member role detected. Applying category view restrictions.")
            binding.btnAddCategory.visibility = View.GONE
        }
    }
    private fun setupPermissionAndAction() {
        // Renamed text to display "Add Category" dynamically matching button requirement
        binding.btnAddCategory.text = AppConstants.BTN_ADD_CATEGORY

        // Rule Handling: Check if logged-in session has permission to alter menu
        val role = sessionManager.getRole()

        // if it's a restricted staff member with no menu management permission, hide access buttons
        if(role == AppConstants.ROLE_STAFF) {
            binding.btnAddCategory.visibility = View.GONE
        }

        binding.btnAddCategory.setOnClickListener {
            Log.d(TAG, "🔘 '+ Add Category' button clicked.")
            val ownerUid = sessionManager.getUid()

            if (ownerUid.isEmpty()) {
                Log.e(TAG, "Access Denied: Session Manager returned an empty owner UID token string.")
                Toast.makeText(requireContext(), "User not authenticated. Please log in again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Session verified. Displaying AddCategoryBottomSheet dialog view interface wrapper.")

            val addCategorySheet = AddCategoryBottomSheet { enteredCategoryName ->
                Log.i(TAG, "Bottom sheet callback fired! User submitted Category Name: '$enteredCategoryName'")

                // Trigger the viewModel to create the document and update it to firestore
                viewModel.addNewCategory(ownerUid, enteredCategoryName)

                Toast.makeText(requireContext(), "Adding category: $enteredCategoryName", Toast.LENGTH_SHORT).show()
            }

            addCategorySheet.show(childFragmentManager, "ADD_CATEGORY_SHEET")
            Log.d(TAG, "AddCategoryBottomSheet shown on screen.")
        }
    }

    private fun observeMenuChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when(state) {

                        is MenuUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvCategories.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }

                        is MenuUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvCategories.visibility = View.VISIBLE
                            binding.tvEmptyState.visibility = View.GONE
                            Log.d(TAG, "🟢 Menu Categories synchronized successfully: ${state.menuItems.size} items found.")
                            menuAdapter.submitList(state.menuItems)
                        }
                        is MenuUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvCategories.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                            Log.w(TAG, "🟡 Empty state collection payload received.")
                            menuAdapter.submitList(emptyList())
                        }
                        is MenuUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                            Toast.makeText(requireContext(), "Error linking menu: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    // Example look at how you will drop it inside your Category screen layout:
    private fun showCategoryDeletePopup(category: MenuCategory) {
        val ownerUid = sessionManager.getUid()

        MenuDialogHelper.showDeleteConfirmation(
            context = requireContext(),
            title = "Delete Category?",
            message = "Are you sure you want to delete \"${category.name}\"? This will also clear out all dishes under it.",
            onConfirm = {
                // OPTIMISTIC UI FIX: Create a temporary list excluding the deleted item
                val currentList = menuAdapter.currentList.toMutableList()
                val indexToRemove = currentList.indexOfFirst { it.id == category.id }

                if (indexToRemove != -1) {
                    currentList.removeAt(indexToRemove)
                    Log.d(TAG, "Optimistic UI: Instantly sliding '${category.name}' out of active view memory layout.")
                    // Submit the reduced list immediately so it vanishes from the UI instantly
                    menuAdapter.submitList(currentList)
                }

                // Trigger your category viewmodel delete function here!
                 viewModel.deleteCategoryItem(ownerUid, category.id, category.name)

                Toast.makeText(requireContext(), "Category ${category.name} removed", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
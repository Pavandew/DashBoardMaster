package com.example.masterdashboard.master_dash.res_lists.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentRestaurentListsBinding
import com.example.masterdashboard.master_dash.MasterHomeActivity
import com.example.masterdashboard.master_dash.utils.SearchQueryManager
import com.example.masterdashboard.master_dash.res_lists.adapter.RestaurantListAdapter
import com.example.masterdashboard.master_dash.res_lists.models.RestaurantData

class RestaurantListsFragment : Fragment() {

    private var _binding: FragmentRestaurentListsBinding? = null
    private val binding get() = _binding!!

    private lateinit var restaurantListAdapter: RestaurantListAdapter
    private val restaurantList = arrayListOf<RestaurantData>()

    private var searchManager: SearchQueryManager<RestaurantData>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestaurentListsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("RestaurantListsFragment", "Navigation: RestaurantListsFragment Opened")

        setupToolbar()
        setupCardItems()
        setupRecyclerView() // Initialize adapter first
        loadDummyData() // Then load data
        setupClickListeners()

        setupSearchBar()
    }

    private fun setupToolbar() {
        binding.resListToolbar.toolbarTvTitle.text = getString(R.string.restaurant_list)

        binding.resListToolbar.toolbarImgMenu.visibility = View.VISIBLE
    }

    private fun setupSearchBar() {
        // Initialize the generic search manager
        searchManager = SearchQueryManager(
            searchEditText = binding.resListSearchbar.etSearch,
            originalList = restaurantList,
            onResultFiltered = { filteredList ->
                if (::restaurantListAdapter.isInitialized) {
                    restaurantListAdapter.updateData(filteredList)
                    updateEmptyState(filteredList.isEmpty())
                }
            },
            filterRule = { restaurant, query ->
                // Define how this specific fragment decides a match
                restaurant.restaurantName.contains(query, ignoreCase = true) ||
                        restaurant.ownerName.contains(query, ignoreCase = true) ||
                        restaurant.userName.contains(query, ignoreCase = true)
            }
        )
    }
    private fun setupClickListeners() {

        // Example
        binding.createNewBtn.setOnClickListener {
            (activity as? MasterHomeActivity)?.openCreateRestaurant()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.rvRestaurants.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutEmptyState.root.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun setupCardItems() {

        binding.apply {

            resListCard1.apply {
                cardTvTitle.text = "Total Restaurants"
                cardTvCount.text = "25"
                cardSubtitle.text = "All Registered"
            }

            resListCard2.apply {
                cardTvTitle.text = "Active Restaurants"
                cardTvCount.text = "18"
                cardSubtitle.text = "Currently Active"
            }

            resListCard3.apply {
                cardTvTitle.text = "Disabled Restaurants"
                cardTvCount.text = "0"
                cardSubtitle.text = "Currently Disabled"
            }

            resListCard4.apply {
                cardTvTitle.text = "Deleted Restaurants"
                cardTvCount.text = "6"
                cardSubtitle.text = "In trash"
            }
        }
    }

    private fun setupRecyclerView() {

        restaurantListAdapter = RestaurantListAdapter(
            restaurantData = restaurantList,

            onClick = { item ->
                // View details
            },

            onEditClick = { item ->
                // Fragment handles its own navigation - no HomeActivity dependency!
                navigateToEdit(item)
            },

            onDeleteClick = { item ->

                // Show delete dialog
            }
        )

        binding.rvRestaurants.apply {

            layoutManager = LinearLayoutManager(requireContext())

            adapter = restaurantListAdapter

            // Important for NestedScrollView
            isNestedScrollingEnabled = false

            setHasFixedSize(false)
        }
    }

    private fun loadDummyData() {

        restaurantList.clear()

        restaurantList.add(
            RestaurantData(
                1,
                "Pizza Hut",
                "user1",
                "John Doe",
                "Active",
                "2024-06-01"
            )
        )

        restaurantList.add(
            RestaurantData(
                2,
                "McDonald's",
                "user2",
                "Jane Smith",
                "Inactive",
                "2024-06-02"
            )
        )

        restaurantList.add(
            RestaurantData(
                3,
                "KFC",
                "user3",
                "Bob Johnson",
                "Active",
                "2024-06-03"
            )
        )
        restaurantList.add(
            RestaurantData(
                1,
                "Pizza Hut",
                "user1",
                "John Doe",
                "Active",
                "2024-06-01"
            )
        )

        restaurantList.add(
            RestaurantData(
                2,
                "McDonald's",
                "user2",
                "Jane Smith",
                "Inactive",
                "2024-06-02"
            )
        )

        restaurantList.add(
            RestaurantData(
                3,
                "KFC",
                "user3",
                "Bob Johnson",
                "Active",
                "2024-06-03"
            )
        )
        restaurantList.add(
            RestaurantData(
                1,
                "Pizza Hut",
                "user1",
                "John Doe",
                "Active",
                "2024-06-01"
            )
        )

        restaurantList.add(
            RestaurantData(
                2,
                "McDonald's",
                "user2",
                "Jane Smith",
                "Inactive",
                "2024-06-02"
            )
        )

        restaurantList.add(
            RestaurantData(
                3,
                "KFC",
                "user3",
                "Bob Johnson",
                "Active",
                "2024-06-03"
            )
        )

        restaurantList.add(
            RestaurantData(
                4,
                "Subway",
                "user4",
                "Alice Brown",
                "Inactive",
                "2024-06-04"
            )
        )

        restaurantList.add(
            RestaurantData(
                5,
                "Burger King",
                "user5",
                "Charlie Davis",
                "Active",
                "2024-06-05"
            )
        )

        restaurantList.add(
            RestaurantData(
                6,
                "Dominos",
                "user6",
                "Mike Ross",
                "Active",
                "2024-06-06"
            )
        )

        // Safe check before notify
        if (::restaurantListAdapter.isInitialized) {
            restaurantListAdapter.notifyDataSetChanged()
            updateEmptyState(restaurantList.isEmpty())
        }
    }

    override fun onDestroyView() {
        // clean up the text listener to avoid memory leaks
        searchManager?.removeListener()
        searchManager = null
        _binding = null
        super.onDestroyView()
    }

    // ========== FRAGMENT NAVIGATION ==========
    // Each fragment handles its own navigation - scalable approach!
    private fun navigateToEdit(restaurantData: RestaurantData) {

        // Create EditResFragment and pass data via Bundle
        val editFragment = EditResFragment().apply {
            arguments = Bundle().apply {
                putInt("id", restaurantData.id)
                putString("restaurantName", restaurantData.restaurantName)
                putString("userName", restaurantData.userName)
                putString("ownerName", restaurantData.ownerName)
                putString("status", restaurantData.status)
                putString("date", restaurantData.date)
            }
        }

        // Push to fragment stack
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.home_fragment_container, editFragment, "EditResFragment")
            addToBackStack("EditResFragment")  // User can press back to return
            commit()
        }
    }
}
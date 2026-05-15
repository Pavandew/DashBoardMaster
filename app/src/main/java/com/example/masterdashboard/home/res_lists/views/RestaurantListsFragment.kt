package com.example.masterdashboard.home.res_lists.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentRestaurentListsBinding
import com.example.masterdashboard.home.HomeActivity
import com.example.masterdashboard.home.res_lists.adapter.RestaurantListAdapter
import com.example.masterdashboard.home.res_lists.models.RestaurantData

class RestaurantListsFragment : Fragment() {

    // ViewBinding
    private var _binding: FragmentRestaurentListsBinding? = null
    private val binding get() = _binding!!

    private lateinit var restaurantListAdapter: RestaurantListAdapter

    private val restaurantList = arrayListOf<RestaurantData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRestaurentListsBinding.inflate(inflater, container, false)

        setupToolbar()
        loadDummyData()
        setupRecyclerView()
        clickListeners()
        setupCardItem()

        return binding.root
    }

    // Toolbar
    private fun setupToolbar() {

        binding.resListToolbar.toolbarTvTitle.setText(
            R.string.restaurant_list
        )
    }

    private fun clickListeners() {
        binding.btnCreateNew.setOnClickListener {
            (activity as HomeActivity).openCreateRestaurant()
        }
    }

    private fun setupCardItem() {
        binding.resListCard1.cardTvTitle.text = "Total Restaurants"
        binding.resListCard1.cardTvCount.text = "25"
        binding.resListCard1.cardSubtitle.text = "All Registered"

        binding.resListCard2.cardTvTitle.text = "Active Restaurants"
        binding.resListCard2.cardTvCount.text = "18"
        binding.resListCard2.cardSubtitle.text = "Currently Active"

        binding.resListCard3.cardTvTitle.text = "Disabled Restaurants"
        binding.resListCard3.cardTvCount.text = "0"
        binding.resListCard3.cardSubtitle.text = "Currently Disabled"

        binding.resListCard4.cardTvTitle.text = "Deleted Restaurants"
        binding.resListCard4.cardTvCount.text = "6"
        binding.resListCard4.cardSubtitle.text = "In trash"
    }

    // RecyclerView Setup
    private fun setupRecyclerView() {

        restaurantListAdapter =
            RestaurantListAdapter(

                restaurantData = restaurantList,

                onClick = { item ->
                    // View details
                },

                onEditClick = { item ->

                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.home_fragment_container,
                            EditResFragment()
                        )
                        .addToBackStack(null)
                        .commit()
                },

                onDeleteClick = { item ->
                    // Delete item
                }
            )

        binding.rvRestaurants.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvRestaurants.setHasFixedSize(true)

        binding.rvRestaurants.adapter =
            restaurantListAdapter
    }

    // Dummy Data
    private fun loadDummyData() {

        restaurantList.add(
            RestaurantData(
                1,
                "Pizza HutPizza Pizza Hut",
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
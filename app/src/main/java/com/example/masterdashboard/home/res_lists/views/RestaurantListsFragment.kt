package com.example.masterdashboard.home.res_lists.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.home.res_lists.adapter.RestaurantListAdapter
import com.example.masterdashboard.home.res_lists.models.RestaurantData

class RestaurantListsFragment : Fragment() {

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView
    private lateinit var restaurantListAdapter: RestaurantListAdapter
    private lateinit var recyclerView: RecyclerView
    private  var restaurantList = arrayListOf<RestaurantData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_restaurent_lists, container, false)

        initializeViews(view)
        setupToolbar(view)
        loadDummyData()
        setupRecyclerView()


        return view
    }

    private fun setupToolbar(view: View) {

        toolbar = view.findViewById(R.id.res_list_toolbar)
        toolbarTitle = view.findViewById(R.id.toolbar_tvTitle)
        toolbarTitle.setText(R.string.restaurant_list)

    }

    // Initialize the Views
    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.rvRestaurants)
    }

    // Set the RecyclerView
    private fun setupRecyclerView() {

        restaurantListAdapter = RestaurantListAdapter(
            restaurantData = restaurantList,
            onClick = { item ->
                //View Details
            },
            onEditClick = { item ->
                // Edit click
                parentFragmentManager.beginTransaction()
                    .replace(R.id.home_fragment_container, EditResFragment())
                    .addToBackStack(null)
                    .commit()
            },

            onDeleteClick = { item ->
                // Delete click
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = restaurantListAdapter
    }

    // Sample Data for RecyclerView
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
        restaurantList.add(RestaurantData(3, "KFC", "user3", "Bob Johnson", "Active", "2024-06-03"))
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
}
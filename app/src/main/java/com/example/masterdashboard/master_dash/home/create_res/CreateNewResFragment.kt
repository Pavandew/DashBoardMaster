package com.example.masterdashboard.master_dash.home.create_res

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentCreateNewResBinding
import com.example.masterdashboard.master_dash.home.create_res.adapter.CreateRestaurantAdapter
import com.example.masterdashboard.master_dash.home.create_res.viewmodel.CreateRestaurantViewModel

class CreateNewResFragment : Fragment() {

    private var _binding: FragmentCreateNewResBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateRestaurantViewModel by viewModels()
    private lateinit var createRestaurantAdapter: CreateRestaurantAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateNewResBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        // Set Title
        binding.newResToolbar.toolbarTvTitle.text = getString(R.string.create_new_res)

        // Hide menu, show back button if your custom_toolbar has one
        binding.newResToolbar.toolbarImgMenu.visibility = View.GONE

        // Professional Touch: Handle back navigation
//        binding.newResToolbar.toolbarImgBack?.setOnClickListener {
//            requireActivity().onBackPressedDispatcher.onBackPressed()
//        }

    }

    private fun setupRecyclerView() {
        // Initialize Adapter with data from ViewModel
        createRestaurantAdapter = CreateRestaurantAdapter(viewModel.getFormItems())

        binding.rvCreateRestaurant.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = createRestaurantAdapter

            // Optimization: Since form items are static in size
            setHasFixedSize(true)

            // UX Improvement: Removes the flicker when notifying data changes
            itemAnimator = null

            // Improvement: Ensures smooth scrolling even with complex layouts
            setItemViewCacheSize(20)
        }
    }

    override fun onDestroyView() {
        // Essential to avoid memory leaks with ViewBinding
        super.onDestroyView()
        _binding = null
    }
}
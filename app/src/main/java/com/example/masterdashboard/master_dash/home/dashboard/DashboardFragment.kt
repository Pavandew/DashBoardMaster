package com.example.masterdashboard.master_dash.home.dashboard

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.masterdashboard.databinding.FragmentDashboardBinding
import com.example.masterdashboard.databinding.ItemDashboardCardBinding
import com.example.masterdashboard.databinding.ItemQuickActionBinding
import com.example.masterdashboard.master_dash.home.MasterHomeActivity
import com.example.masterdashboard.master_dash.home.dashboard.model.DashboardCardModel
import com.example.masterdashboard.master_dash.home.dashboard.model.QuickActionModel
import com.example.masterdashboard.master_dash.home.dashboard.utils.DashboardUiData
import com.example.masterdashboard.master_dash.home.dashboard.viewmodel.DashboardViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val TAG = "DashboardFragment"

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var adapter: RecentActivityAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: UI inflating")

        _binding =
            FragmentDashboardBinding.inflate(
                inflater,
                container,
                false
            )

        setupDashboardCards()
        setupQuickActions()
        setupRecycler()
        setupClicks()
        observeData()

        Log.i(TAG, "onCreateView: Triggering data load")
        viewModel.loadData()

        return binding.root
    }

    // Dashboard Cards
    private fun setupDashboardCards() {
        Log.d(TAG, "setupDashboardCards: Initializing cards")

        val cards = DashboardUiData.statsCards

        binding.dashCardTotalRes.setCardStyle(cards[0])
        binding.dashCardActiveRes.setCardStyle(cards[1])
        binding.dashCardDisableRes.setCardStyle(cards[2])
        binding.dashCardTotalAdmin.setCardStyle(cards[3])
    }

    // Quick Action Buttons
    private fun setupQuickActions() {
        Log.d(TAG, "setupQuickActions: Setting up quick action listeners")

        val actions = DashboardUiData.quickActions

        binding.dashQuickCardCreteRes.setQuickAction(actions[0]) {
            (activity as MasterHomeActivity).openCreateRestaurant()
        }

        binding.dashQuickCardAllRes.setQuickAction(actions[1]) {
            (activity as MasterHomeActivity).openRestaurantList()
        }

        binding.dashQuickCardLogs.setQuickAction(actions[2]) {
            (activity as MasterHomeActivity).openLogs()
        }

        binding.dashQuickCardSettings.setQuickAction(actions[3]) {
            (activity as MasterHomeActivity).openProfile()
        }
    }

    // RecyclerView
    private fun setupRecycler() {

        adapter = RecentActivityAdapter()

        binding.recyclerRecentActivity.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerRecentActivity.adapter = adapter
    }

    // Click Events
    private fun setupClicks() {

        binding.dashTvViewAll.setOnClickListener {
            (activity as MasterHomeActivity).openLogs()
        }
    }

    // Observe Firebase LiveData
    private fun observeData() {

        viewModel.total.observe(viewLifecycleOwner) {
            Log.v(TAG, "observeData: total restaurants = $it")
            binding.dashCardTotalRes.cardTvCount.text =
                it.toString()
        }

        viewModel.active.observe(viewLifecycleOwner) {
            Log.v(TAG, "observeData: active restaurants = $it")
            binding.dashCardActiveRes.cardTvCount.text =
                it.toString()
        }

        viewModel.disabled.observe(viewLifecycleOwner) {
            Log.v(TAG, "observeData: disabled restaurants = $it")
            binding.dashCardDisableRes.cardTvCount.text =
                it.toString()
        }

        viewModel.admins.observe(viewLifecycleOwner) {
            Log.v(TAG, "observeData: total admins = $it")
            binding.dashCardTotalAdmin.cardTvCount.text =
                it.toString()
        }

        viewModel.recentLogs.observe(viewLifecycleOwner) {
            Log.v(TAG, "observeData: recent logs count = ${it.size}")
            adapter.submitList(it)
        }
    }

    // Reusable Dashboard Card
    private fun ItemDashboardCardBinding.setCardStyle(
        item: DashboardCardModel
    ) {

        val context = root.context

        cardTvTitle.text = item.title
        cardSubtitle.text = item.subtitle

        cardIcon.setImageResource(item.icon)

        cardIcon.imageTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    item.iconColor
                )
            )

        cardImgBg.backgroundTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    item.bgColor
                )
            )
    }

    // Reusable Quick Action
    private fun ItemQuickActionBinding.setQuickAction(
        item: QuickActionModel,
        onClick: () -> Unit
    ) {

        val context = root.context

        quickActionTitle.text = item.title

        quickActionIcon.setImageResource(item.icon)

        cardQuickAction.backgroundTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    item.bgColor
                )
            )

        cardQuickAction.setOnClickListener {
            onClick()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
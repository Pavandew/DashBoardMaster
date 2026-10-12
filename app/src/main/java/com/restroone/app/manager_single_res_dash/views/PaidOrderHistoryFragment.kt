package com.restroone.app.manager_single_res_dash.views

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.restroone.app.R
import com.restroone.app.databinding.FragmentPaidOrderHistoryBinding
import com.restroone.app.manager_single_res_dash.adapter.ClosedOrdersAdapter
import com.restroone.app.manager_single_res_dash.settings.repo.ReportsRepository
import com.restroone.app.manager_single_res_dash.utils.DateFilterBottomSheet
import com.restroone.app.manager_single_res_dash.viewModel.PaidOrderHistoryViewModel
import com.restroone.app.staff_dash.billing_screens.views.CashierBillSummaryFragment
import com.restroone.app.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.restroone.app.utils.NavigationUtils
import com.restroone.app.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PaidOrderHistoryFragment : Fragment() {

    companion object {
        private const val TAG = "PaidOrderHistoryFrag"
    }

    private var _binding: FragmentPaidOrderHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PaidOrderHistoryViewModel by viewModels()
    private val sessionManager by lazy { SessionManager(requireContext()) }

    private lateinit var closedOrdersAdapter: ClosedOrdersAdapter
    private var activeTimeFilter = ReportsRepository.TimeFilter.TODAY

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaidOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: Entered PaidOrderHistoryFragment")

        setupToolbar()
        setupDateFilterPicker()
        setupRecyclerView()
        setupSearchInput()
        observeViewModel()

        val managerId = sessionManager.getUid()
        Log.d(TAG, "Initializing paid order history for managerId: '$managerId'")
        viewModel.loadPaidOrders(managerId, ReportsRepository.TimeFilter.TODAY)
    }

    private fun setupToolbar() {
        val toolbar = binding.paidOrdersToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = "Paid Order History"
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setImageResource(R.drawable.ic_arrow_back_24dp)
        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE

        toolbar.toolbarImgMenu.setOnClickListener {
            Log.d(TAG, "Toolbar Back clicked - popping fragment stack")
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupDateFilterPicker() {
        updateFilterButtonLabels(activeTimeFilter, "Today (${getFormattedDate(0)})")

        binding.btnDateFilterPicker.setOnClickListener {
            Log.d(TAG, "Opening DateFilterBottomSheet")
            val filterBottomSheet = DateFilterBottomSheet(activeTimeFilter) { selectedFilter, isCustom ->
                val managerId = sessionManager.getUid()

                if (isCustom) {
                    showDateRangePicker { startDisplay, endDisplay, startIso, endIso ->
                        Log.i(TAG, "Custom Date Range Selected: $startDisplay to $endDisplay")
                        binding.tvActiveDateFilter.text = "Custom Range"
                        binding.tvActiveDateRange.text = "Showing orders for: $startDisplay - $endDisplay"
                        viewModel.loadPaidOrders(managerId, activeTimeFilter, startIso, endIso)
                    }
                } else {
                    activeTimeFilter = selectedFilter
                    val label = when (selectedFilter) {
                        ReportsRepository.TimeFilter.TODAY -> "Today (${getFormattedDate(0)})"
                        ReportsRepository.TimeFilter.WEEK -> "This Week (Last 7 Days)"
                        ReportsRepository.TimeFilter.MONTH -> "This Month (${getFormattedMonth()})"
                    }
                    updateFilterButtonLabels(selectedFilter, label)
                    viewModel.loadPaidOrders(managerId, selectedFilter)
                }
            }
            filterBottomSheet.show(childFragmentManager, "DATE_FILTER_SHEET")
        }
    }

    private fun updateFilterButtonLabels(filter: ReportsRepository.TimeFilter, fullLabel: String) {
        val buttonText = when (filter) {
            ReportsRepository.TimeFilter.TODAY -> "Today"
            ReportsRepository.TimeFilter.WEEK -> "This Week"
            ReportsRepository.TimeFilter.MONTH -> "This Month"
        }
        binding.tvActiveDateFilter.text = buttonText
        binding.tvActiveDateRange.text = "Showing orders for: $fullLabel"
    }

    private fun setupRecyclerView() {
        closedOrdersAdapter = ClosedOrdersAdapter { clickedOrder ->
            Log.d(TAG, "Paid Order clicked: ${clickedOrder.orderId}, Path: ${clickedOrder.docPath}")

            val formattedDateTime = clickedOrder.timestamp?.toDate()?.let {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
            } ?: ""

            val summaryFragment = CashierBillSummaryFragment.newInstance(
                orderDocPath = clickedOrder.docPath,
                orderId = clickedOrder.orderId,
                totalAmount = clickedOrder.grandTotal,
                received = clickedOrder.grandTotal,
                change = 0.0,
                discount = clickedOrder.discount,
                itemCount = clickedOrder.totalItems,
                method = clickedOrder.paymentMethod,
                dateTime = formattedDateTime
            )

            val containerId = NavigationUtils.getHostContainerId(activity)
            if (containerId != 0) {
                parentFragmentManager.beginTransaction()
                    .replace(containerId, summaryFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.rvPaidOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = closedOrdersAdapter
        }
    }

    private fun setupSearchInput() {
        binding.etSearchOrders.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filterOrders(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ResourceUiState.Loading -> {
                            binding.layoutOrdersEmptyState.visibility = View.GONE
                        }
                        is ResourceUiState.Success -> {
                            val list = state.data
                            closedOrdersAdapter.submitList(list)
                            if (list.isEmpty()) {
                                binding.rvPaidOrders.visibility = View.GONE
                                binding.layoutOrdersEmptyState.visibility = View.VISIBLE
                            } else {
                                binding.rvPaidOrders.visibility = View.VISIBLE
                                binding.layoutOrdersEmptyState.visibility = View.GONE
                            }
                        }
                        is ResourceUiState.Error -> {
                            binding.rvPaidOrders.visibility = View.GONE
                            binding.layoutOrdersEmptyState.visibility = View.VISIBLE
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun showDateRangePicker(onRangeSelected: (startDisplay: String, endDisplay: String, startIso: String, endIso: String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, startYear, startMonth, startDay ->
                val startCal = Calendar.getInstance().apply { set(startYear, startMonth, startDay) }

                DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDay ->
                        val endCal = Calendar.getInstance().apply { set(endYear, endMonth, endDay) }

                        if (endCal.before(startCal)) {
                            Toast.makeText(context, "End date cannot be before start date.", Toast.LENGTH_SHORT).show()
                            return@DatePickerDialog
                        }

                        val diffMillis = endCal.timeInMillis - startCal.timeInMillis
                        val diffDays = diffMillis / (1000 * 60 * 60 * 24)

                        if (diffDays > 90) {
                            Toast.makeText(context, "Maximum custom date range allowed is 3 months (90 days).", Toast.LENGTH_LONG).show()
                            return@DatePickerDialog
                        }

                        val displaySdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val isoSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                        onRangeSelected(
                            displaySdf.format(startCal.time),
                            displaySdf.format(endCal.time),
                            isoSdf.format(startCal.time),
                            isoSdf.format(endCal.time)
                        )
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setTitle("Select End Date")
                    show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select Start Date")
            show()
        }
    }

    private fun getFormattedDate(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }

    private fun getFormattedMonth(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

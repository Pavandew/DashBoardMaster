package com.example.masterdashboard.manager_single_res_dash.views

import android.app.DatePickerDialog
import android.os.Bundle
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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentReportsAnalyticsBinding
import com.example.masterdashboard.manager_single_res_dash.adapter.ReportKpiAdapter
import com.example.masterdashboard.manager_single_res_dash.models.ReportKpiModel
import com.example.masterdashboard.manager_single_res_dash.repo.ReportsRepository
import com.example.masterdashboard.manager_single_res_dash.utils.DateFilterBottomSheet
import com.example.masterdashboard.manager_single_res_dash.viewModel.ReportsViewModel
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsAnalyticsFragment : Fragment() {

    companion object {
        private const val TAG = "ReportsAnalytics"
    }

    private var _binding: FragmentReportsAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels()
    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val currencyFormatter by lazy { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    private lateinit var kpiAdapter: ReportKpiAdapter
    private var activeTimeFilter = ReportsRepository.TimeFilter.TODAY

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: Entered ReportsAnalyticsFragment")

        setupToolbar()
        setupDateFilterPicker()
        setupKpiRecyclerView()
        setupActionButtons()
        observeReportData()

        val managerId = sessionManager.getUid()
        Log.d(TAG, "Initializing report screen for managerId: '$managerId'")
        viewModel.loadReportData(managerId, ReportsRepository.TimeFilter.TODAY)
    }

    private fun setupToolbar() {
        val toolbar = binding.reportsToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = "Reports & Analytics"
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
                    showDatePicker { selectedDate ->
                        Log.i(TAG, "Custom Date Selected: $selectedDate")
                        activeTimeFilter = ReportsRepository.TimeFilter.TODAY
                        updateFilterButtonLabels(activeTimeFilter, "Custom Date: $selectedDate")
                        viewModel.loadReportData(managerId, ReportsRepository.TimeFilter.TODAY)
                    }
                } else {
                    activeTimeFilter = selectedFilter
                    val label = when (selectedFilter) {
                        ReportsRepository.TimeFilter.TODAY -> "Today (${getFormattedDate(0)})"
                        ReportsRepository.TimeFilter.WEEK -> "This Week (Last 7 Days)"
                        ReportsRepository.TimeFilter.MONTH -> "This Month (${getFormattedMonth()})"
                    }
                    updateFilterButtonLabels(selectedFilter, label)
                    viewModel.loadReportData(managerId, selectedFilter)
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
        binding.tvActiveDateRange.text = "Showing report for: $fullLabel"
    }

    private fun setupKpiRecyclerView() {
        kpiAdapter = ReportKpiAdapter(emptyList())
        binding.rvKpiCards.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = kpiAdapter
        }
    }

    private fun observeReportData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportState.collect { summary ->
                    Log.d(TAG, "UI Update: Displaying report metrics - Total Revenue: ₹${summary.totalRevenue}, Total Orders: ${summary.totalOrders}")

                    // 1. Update KPI RecyclerView items
                    val revenueSubtitle = if (summary.totalOrders > 0) "↑ Active period sales" else "No sales recorded"
                    val orderSubtitle = if (summary.totalOrders > 0) "↑ Completed orders" else "No orders completed"

                    val kpiList = listOf(
                        ReportKpiModel(
                            title = "Total Collection",
                            value = formatCurrency(summary.totalRevenue),
                            subtitle = revenueSubtitle,
                            iconRes = R.drawable.ic_sales_report_24dp,
                            iconTintRes = R.color.green_growth
                        ),
                        ReportKpiModel(
                            title = "Orders Completed",
                            value = "${summary.totalOrders} Orders",
                            subtitle = orderSubtitle,
                            iconRes = R.drawable.ic_history_24dp,
                            iconTintRes = R.color.accent_blue
                        ),
                        ReportKpiModel(
                            title = "Avg Order Value",
                            value = formatCurrency(summary.avgOrderValue),
                            subtitle = "Per transaction",
                            iconRes = R.drawable.ic_payments_24dp,
                            iconTintRes = R.color.accent_purple
                        ),
                        ReportKpiModel(
                            title = "Total Discounts",
                            value = formatCurrency(summary.totalDiscounts),
                            subtitle = "Applied on bills",
                            iconRes = R.drawable.ic_discount_24dp,
                            iconTintRes = R.color.accent_orange
                        )
                    )
                    kpiAdapter.updateKpis(kpiList)

                    // 2. Update Payment Breakdown sub-layout
                    val paymentCard = binding.cardPaymentBreakdown
                    val hasRevenue = summary.totalRevenue > 0
                    val cashPct = if (hasRevenue) ((summary.cashAmount / summary.totalRevenue) * 100).toInt() else 0
                    val upiPct = if (hasRevenue) ((summary.upiAmount / summary.totalRevenue) * 100).toInt() else 0
                    val cardPct = if (hasRevenue) ((summary.cardAmount / summary.totalRevenue) * 100).toInt() else 0

                    paymentCard.tvCashAmount.text = "${formatCurrency(summary.cashAmount)} ($cashPct%)"
                    paymentCard.pbCashPercentage.progress = cashPct

                    paymentCard.tvUpiAmount.text = "${formatCurrency(summary.upiAmount)} ($upiPct%)"
                    paymentCard.pbUpiPercentage.progress = upiPct

                    paymentCard.tvCardAmount.text = "${formatCurrency(summary.cardAmount)} ($cardPct%)"
                    paymentCard.pbCardPercentage.progress = cardPct

                    // 3. Update Order Channels sub-layout
                    val channelsCard = binding.cardOrderChannels
                    channelsCard.tvDineInSales.text = formatCurrency(summary.dineInSales)
                    channelsCard.tvDineInOrders.text = "${summary.dineInOrders} Orders"

                    channelsCard.tvTakeawaySales.text = formatCurrency(summary.takeawaySales)
                    channelsCard.tvTakeawayOrders.text = "${summary.takeawayOrders} Orders"

                    // 4. Update Tax & Summary sub-layout
                    val taxCard = binding.cardTaxSummary
                    taxCard.tvGrossSubtotal.text = formatCurrency(summary.grossSubtotal)
                    taxCard.tvTotalGst.text = formatCurrency(summary.totalGst)
                    taxCard.tvTotalDiscountSummary.text = "- ${formatCurrency(summary.totalDiscounts)}"
                    taxCard.tvNetCollection.text = formatCurrency(summary.totalRevenue)
                }
            }
        }
    }

    private fun setupActionButtons() {
        val actions = binding.layoutReportActions

        actions.btnPrintReport.setOnClickListener {
            Log.i(TAG, "Action: 'Print Summary' button clicked")
            Toast.makeText(requireContext(), "🖨️ Printing Daily Closure Report on POS Printer...", Toast.LENGTH_SHORT).show()
        }

        actions.btnExportReport.setOnClickListener {
            Log.i(TAG, "Action: 'Export PDF' button clicked")
            Toast.makeText(requireContext(), "📥 Report PDF exported successfully to Downloads folder!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        Log.d(TAG, "Opening DatePickerDialog")
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                onDateSelected(sdf.format(selectedCal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val formatted = currencyFormatter.format(amount)
            if (!formatted.startsWith("₹")) "₹ $amount" else formatted.replace("₹", "₹ ")
        } catch (e: Exception) {
            "₹ $amount"
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
        Log.d(TAG, "onDestroyView called")
        _binding = null
    }
}

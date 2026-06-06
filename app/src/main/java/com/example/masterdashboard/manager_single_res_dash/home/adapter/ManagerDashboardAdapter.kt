package com.example.masterdashboard.manager_single_res_dash.home.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemDahsCardTrendBinding
import com.example.masterdashboard.databinding.ItemDashQuickActionsBinding
import com.example.masterdashboard.databinding.ItemDashboardOverviewBinding
import com.example.masterdashboard.databinding.ItemDashboardSummaryBinding
import com.example.masterdashboard.databinding.ItemTopSellingCardsBinding
import com.example.masterdashboard.manager_single_res_dash.home.models.DashboardSummary
import com.example.masterdashboard.manager_single_res_dash.home.models.FoodItem
import com.example.masterdashboard.manager_single_res_dash.home.models.StatMetric
import com.google.android.material.card.MaterialCardView

class ManagerDashboardAdapter (
    private var metricsList: List<StatMetric>,
    private var summaryData: DashboardSummary,
    private var topSellingItems: List<FoodItem>,
    private val onQuickActionClicked: (actionType: QuickActionType) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun updateData(
        newMetrics: List<StatMetric>,
        newSummary: DashboardSummary,
        newTopSelling: List<FoodItem>
    ) {
        this.metricsList = newMetrics
        this.summaryData = newSummary
        this.topSellingItems = newTopSelling
        notifyDataSetChanged()
    }

    // Enum representing your distinct quick action options cleanly
    enum class QuickActionType {
        ADD_STAFF, MENU, FLOOR_TABLE, ORDERS, REPORTS
    }

    companion object {
        private const val TYPE_OVERVIEW = 0
        private const val TYPE_SUMMARY = 1
        private const val TYPE_QUICK_ACTIONS = 2
        private const val TYPE_TREND = 3
        private const val TYPE_TOP_SELLING = 4

        private const val SECTIONS_COUNT = 5
    }

    override fun getItemCount(): Int = SECTIONS_COUNT

    override fun getItemViewType(position: Int): Int {
        return when(position) {
            0 -> TYPE_OVERVIEW
            1 -> TYPE_SUMMARY
            2 -> TYPE_QUICK_ACTIONS
            3 -> TYPE_TREND
            else -> TYPE_TOP_SELLING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_OVERVIEW -> OverviewViewHolder(ItemDashboardOverviewBinding.inflate(inflater, parent, false))
            TYPE_SUMMARY -> SummaryViewHolder(ItemDashboardSummaryBinding.inflate(inflater, parent, false))
            TYPE_QUICK_ACTIONS -> QuickActionsViewHolder(ItemDashQuickActionsBinding.inflate(inflater, parent, false))
            TYPE_TREND -> TrendViewHolder(ItemDahsCardTrendBinding.inflate(inflater, parent, false))
            else -> TopSellingViewHolder(ItemTopSellingCardsBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is OverviewViewHolder -> holder.bind(metricsList)
            is SummaryViewHolder -> holder.bind(summaryData)
            is QuickActionsViewHolder -> holder.bind(onQuickActionClicked)
            is TopSellingViewHolder -> holder.bind(topSellingItems)
        }
    }

    // VIEW HOLDERS IMPLEMENTATION

    class OverviewViewHolder(val binding: ItemDashboardOverviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(metrics: List<StatMetric>) {
            val rootGrid = binding.gridOverviewContainer

            val colors = listOf(
                Pair("#1D2D4B", "#121826"), // Blue
                Pair("#44321A", "#1C1917"), // Orange
                Pair("#461F33", "#1A1420"), // Pink
                Pair("#2B204D", "#151426")  // Purple
            )

            for (i in 0 until minOf(metrics.size, rootGrid.childCount)) {
                val cardView = rootGrid.getChildAt(i) as? MaterialCardView ?: continue

                val title = cardView.findViewById<TextView>(R.id.lblStatTitle)
                val value = cardView.findViewById<TextView>(R.id.lblStatValue)
                val trend = cardView.findViewById<TextView>(R.id.lblStatTrend)
                val icon = cardView.findViewById<ImageView>(R.id.lblImageIcon)

                title.text = metrics[i].title
                value.text = metrics[i].value
                trend.text = metrics[i].trend

                val colorScheme = colors[i % colors.size]
                cardView.strokeColor = colorScheme.first.toColorInt()
                cardView.setCardBackgroundColor(colorScheme.second.toColorInt())

                val context = cardView.context
                trend.setTextColor(
                    if (metrics[i].isPositiveTrend) context.getColor(R.color.green_growth)
                    else context.getColor(R.color.red_alert)
                )

                val tintColor = when(i % colors.size) {
                    0 -> context.getColor(R.color.accent_blue)
                    1 -> context.getColor(R.color.accent_orange)
                    2 -> context.getColor(R.color.accent_pink)
                    else -> context.getColor(R.color.accent_purple)
                }
                icon.setColorFilter(tintColor)
            }
        }
    }

    class SummaryViewHolder(val binding: ItemDashboardSummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(summary: DashboardSummary) {
            val context = binding.root.context
            binding.summaryNew.apply {
                txtSummaryLabel.text = context.getString(R.string.summary_new)
                txtSummaryCount.text = summary.newCount
                imgSummaryIcon.setImageResource(R.drawable.ic_person_24dp)
                imgSummaryIcon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.accent_blue))
            }
            binding.summaryKitchen.apply {
                txtSummaryLabel.text = context.getString(R.string.summary_kitchen)
                txtSummaryCount.text = summary.kitchenCount
                imgSummaryIcon.setImageResource(R.drawable.ic_person_24dp)
                imgSummaryIcon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.accent_purple))
            }
            binding.summaryReady.apply {
                txtSummaryLabel.text = context.getString(R.string.summary_ready)
                txtSummaryCount.text = summary.readyCount
                imgSummaryIcon.setImageResource(R.drawable.ic_person_24dp)
                imgSummaryIcon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.green_growth))
            }
            binding.summaryServed.apply {
                txtSummaryLabel.text = context.getString(R.string.summary_served)
                txtSummaryCount.text = summary.servedCount
                imgSummaryIcon.setImageResource(R.drawable.ic_person_24dp)
                imgSummaryIcon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.accent_blue))
            }
            binding.summaryCancelled.apply {
                txtSummaryLabel.text = context.getString(R.string.summary_cancelled)
                txtSummaryCount.text = summary.cancelledCount
                imgSummaryIcon.setImageResource(R.drawable.ic_person_24dp)
                imgSummaryIcon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.red_alert))
                summaryDivider.visibility = View.GONE
            }
        }
    }

    class QuickActionsViewHolder(val binding: ItemDashQuickActionsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(onItemAction: (QuickActionType) -> Unit) {
            val context = binding.root.context

            // Add Staff Click handling
            binding.actionAddStaff.apply {
                quickActionTitle.text = "Add Staff"
                quickActionIcon.setImageResource(R.drawable.ic_staffs_24dp)
                quickActionIcon.setColorFilter(context.getColor(R.color.accent_purple))
                cardQuickAction.strokeColor = "#2B204D".toColorInt()
                cardQuickAction.setCardBackgroundColor("#151426".toColorInt())
                root.setOnClickListener { onItemAction(QuickActionType.ADD_STAFF) }
            }

            // Menu Click handling
            binding.actionMenu.apply {
                quickActionTitle.text = "Menu"
                quickActionIcon.setImageResource(R.drawable.biling)
                quickActionIcon.setColorFilter(context.getColor(R.color.accent_orange))
                cardQuickAction.strokeColor = "#44321A".toColorInt()
                cardQuickAction.setCardBackgroundColor("#1C1917".toColorInt())
                root.setOnClickListener { onItemAction(QuickActionType.MENU) }
            }

            // Floor/Table Click handling
            binding.actionFloorTable.apply {
                quickActionTitle.text = "Floor / Table"
                quickActionIcon.setImageResource(R.drawable.ic_table_24dp)
                quickActionIcon.setColorFilter(context.getColor(R.color.green_growth))
                cardQuickAction.strokeColor = "#173B2C".toColorInt()
                cardQuickAction.setCardBackgroundColor("#111818".toColorInt())
                root.setOnClickListener { onItemAction(QuickActionType.FLOOR_TABLE) }
            }

            // Orders Click handling
            binding.actionOrders.apply {
                quickActionTitle.text = "Orders"
                quickActionIcon.setImageResource(R.drawable.bg_order_notes)
                quickActionIcon.setColorFilter(context.getColor(R.color.accent_pink))
                cardQuickAction.strokeColor = "#461F33".toColorInt()
                cardQuickAction.setCardBackgroundColor("#1A1420".toColorInt())
                root.setOnClickListener { onItemAction(QuickActionType.ORDERS) }
            }

            // Reports Click handling
            binding.actionReports.apply {
                quickActionTitle.text = "Reports"
                quickActionIcon.setImageResource(R.drawable.ic_sales_report_24dp)
                quickActionIcon.setColorFilter(context.getColor(R.color.accent_blue))
                cardQuickAction.strokeColor = "#1D2D4B".toColorInt()
                cardQuickAction.setCardBackgroundColor("#121826".toColorInt())
                root.setOnClickListener { onItemAction(QuickActionType.REPORTS) }
            }
        }
    }

    class TrendViewHolder(val binding: ItemDahsCardTrendBinding) : RecyclerView.ViewHolder(binding.root)

    class TopSellingViewHolder(val binding: ItemTopSellingCardsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(items: List<FoodItem>) {
            val itemViews = listOf(
                binding.topSellingCard1,
                binding.topSellingCard2,
                binding.topSellingCard3
            )

            for (i in itemViews.indices) {
                if (i < items.size) {
                    val item = items[i]
                    itemViews[i].root.visibility = View.VISIBLE
                    itemViews[i].txtFoodName.text = item.name
                    itemViews[i].txtOrderCount.text = itemViews[i].root.context.getString(R.string.order_count_format, item.orderCount)
                    itemViews[i].txtFoodPrice.text = item.totalPriceText
                    itemViews[i].imgFoodItem.setImageResource(item.imageResId)
                } else {
                    itemViews[i].root.visibility = View.GONE
                }
            }
        }
    }
}
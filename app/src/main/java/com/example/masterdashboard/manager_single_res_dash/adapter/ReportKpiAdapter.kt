package com.example.masterdashboard.manager_single_res_dash.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.ItemReportKpiCardBinding
import com.example.masterdashboard.manager_single_res_dash.models.ReportKpiModel

class ReportKpiAdapter(
    private var kpiList: List<ReportKpiModel>
) : RecyclerView.Adapter<ReportKpiAdapter.KpiViewHolder>() {

    fun updateKpis(newList: List<ReportKpiModel>) {
        kpiList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KpiViewHolder {
        val binding = ItemReportKpiCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KpiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KpiViewHolder, position: Int) {
        holder.bind(kpiList[position])
    }

    override fun getItemCount(): Int = kpiList.size

    inner class KpiViewHolder(private val binding: ItemReportKpiCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReportKpiModel) {
            val context = binding.root.context
            binding.tvKpiTitle.text = item.title
            binding.tvKpiValue.text = item.value
            binding.tvKpiSubtitle.text = item.subtitle
            binding.imgKpiIcon.setImageResource(item.iconRes)
            binding.imgKpiIcon.setColorFilter(ContextCompat.getColor(context, item.iconTintRes))
        }
    }
}

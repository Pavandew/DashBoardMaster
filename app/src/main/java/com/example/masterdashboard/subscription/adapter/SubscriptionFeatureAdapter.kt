package com.example.masterdashboard.subscription.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R

class SubscriptionFeatureAdapter(
    private val features: List<String>
) : RecyclerView.Adapter<SubscriptionFeatureAdapter.FeatureViewHolder>() {

    class FeatureViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFeatureText: TextView = view.findViewById(R.id.tvFeatureText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription_feature, parent, false)
        return FeatureViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.tvFeatureText.text = features[position]
    }

    override fun getItemCount(): Int = features.size
}

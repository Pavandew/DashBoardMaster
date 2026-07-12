package com.example.masterdashboard.master_dash.res_lists.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemRestaurantRowBinding
import com.example.masterdashboard.master_dash.res_lists.models.RestaurantData

class RestaurantListAdapter(
    private var restaurantData: List<RestaurantData>,
    private val onClick: (RestaurantData) -> Unit,
    private val onEditClick: (RestaurantData) -> Unit,
    private val onDeleteClick: (RestaurantData) -> Unit
) : RecyclerView.Adapter<RestaurantListAdapter.RestaurantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val binding = ItemRestaurantRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RestaurantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        holder.bind(restaurantData[position])
    }

    override fun getItemCount(): Int = restaurantData.size

    // Professional touch: Adding a method to update data easily
    fun updateData(newList: List<RestaurantData>) {
        this.restaurantData = newList
        notifyDataSetChanged()
    }

    inner class RestaurantViewHolder(
        private val binding: ItemRestaurantRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RestaurantData) {
            val context = binding.root.context

            // 1. Bind basic text data
            binding.tvId.text = "#${item.id}"
            binding.tvRestaurantName.text = item.restaurantName
            binding.tvUsername.text = "@${item.userName}" // Assuming username exists in model
            binding.tvOwner.text = "Owner: ${item.ownerName}"
            binding.tvCreated.text = "Joined ${item.date}"
            binding.tvStatus.text = item.status

            // 2. Professional Status Pill Logic
            if (item.status.equals("Active", true)) {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green))
            } else {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_inactive) // Create a light red version
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.red))
            }

            // 3. Setup Click Listeners
            binding.root.setOnClickListener {
                onClick(item)
            }

            binding.resBtnEye.setOnClickListener {
                onClick(item)
            }

            binding.resBtnEdit.setOnClickListener {
                onEditClick(item)
            }

            binding.resBtnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }
}
package com.example.masterdashboard.home.res_lists.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemRestaurantRowBinding
import com.example.masterdashboard.home.res_lists.models.RestaurantData

class RestaurantListAdapter(
    private val restaurantData: List<RestaurantData>,
    private val onClick: (RestaurantData) -> Unit,
    private val onEditClick: (RestaurantData) -> Unit,
    private val onDeleteClick: (RestaurantData) -> Unit
) : RecyclerView.Adapter<RestaurantListAdapter.RestaurantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        // Inflate your item layout and create a ViewHolder

        val binding = ItemRestaurantRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return RestaurantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        // Bind data to your ViewHolder

        holder.bind(restaurantData[position])
    }

    override fun getItemCount(): Int {
        // Return the total number of items in the list
        return restaurantData.size
    }

    inner class RestaurantViewHolder(
        private val binding: ItemRestaurantRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        // Initialize your views here

        fun bind(item: RestaurantData) {
            binding.tvId.text = item.id.toString()
            binding.tvRestaurantName.text = item.restaurantName
            binding.tvOwner.text = item.ownerName
            binding.tvCreated.text = item.date

            binding.tvStatus.text = item.status

            // set color when active or other
            if(item.status.equals("Active", true)) {
                binding.tvStatus.setBackgroundColor(
                    R.drawable.bg_card_2
                )
            } else {
                binding.tvStatus.setBackgroundColor(
                    R.drawable.bg_card
                )
            }

            // onClicks
            binding.root.setOnClickListener {
                onClick(item)
            }

            binding.resBtnEdit.setOnClickListener{
                onEditClick(item)
            }
        }

    }
}
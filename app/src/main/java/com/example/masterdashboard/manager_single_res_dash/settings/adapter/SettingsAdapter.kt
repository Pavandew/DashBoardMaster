package com.example.masterdashboard.manager_single_res_dash.settings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemSettingsCategoryBinding
import com.example.masterdashboard.databinding.ItemSettingsOptionBinding
import com.example.masterdashboard.manager_single_res_dash.settings.model.SettingsCategory
import com.example.masterdashboard.manager_single_res_dash.settings.model.SettingsOption

class SettingsAdapter(
    private var categories: List<SettingsCategory>,
    private val onOptionClick: (option: SettingsOption) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.CategoryViewHolder>() {

    fun updateCategories(newCategories: List<SettingsCategory>) {
        this.categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemSettingsCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(
        private val binding: ItemSettingsCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: SettingsCategory) {
            val context = binding.root.context

            binding.tvCategoryTitle.text = category.title
            binding.tvCategorySubtitle.text = category.subtitle
            binding.ivCategoryIcon.setImageResource(category.iconRes)
            binding.ivCategoryIcon.setColorFilter(
                ContextCompat.getColor(context, category.iconTintRes)
            )

            // Clear previously inflated option views
            binding.llOptionsContainer.removeAllViews()

            val inflater = LayoutInflater.from(context)

            category.options.forEachIndexed { index, option ->
                val optionBinding = ItemSettingsOptionBinding.inflate(
                    inflater,
                    binding.llOptionsContainer,
                    false
                )

                optionBinding.tvOptionTitle.text = option.title
                optionBinding.tvOptionSubtitle.text = option.subtitle
                optionBinding.ivOptionIcon.setImageResource(option.iconRes)

                if (option.isDestructive) {
                    val redColor = ContextCompat.getColor(context, R.color.red_alert)
                    optionBinding.tvOptionTitle.setTextColor(redColor)
                    optionBinding.ivOptionIcon.setColorFilter(redColor)
                    optionBinding.ivOptionChevron.setColorFilter(redColor)
                } else {
                    val primaryColor = ContextCompat.getColor(context, R.color.text_primary)
                    val secondaryColor = ContextCompat.getColor(context, R.color.text_secondary)
                    optionBinding.tvOptionTitle.setTextColor(primaryColor)
                    optionBinding.ivOptionIcon.setColorFilter(secondaryColor)
                    optionBinding.ivOptionChevron.setColorFilter(secondaryColor)
                }

                optionBinding.llOptionRoot.setOnClickListener {
                    onOptionClick(option)
                }

                binding.llOptionsContainer.addView(optionBinding.root)

                // Add a divider line between option items (except after the last item)
                if (index < category.options.size - 1) {
                    val divider = View(context).apply {
                        layoutParams = ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            1
                        ).apply {
                            topMargin = 4
                            bottomMargin = 4
                        }
                        setBackgroundColor(0xFF2C2E3B.toInt())
                    }
                    binding.llOptionsContainer.addView(divider)
                }
            }
        }
    }
}
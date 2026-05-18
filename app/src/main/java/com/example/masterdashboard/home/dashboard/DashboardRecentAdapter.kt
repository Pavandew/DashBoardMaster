package com.example.masterdashboard.home.dashboard

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemRecentActivityBinding
import com.example.masterdashboard.home.dashboard.model.ActivityLogsModel

class RecentActivityAdapter :
    ListAdapter<ActivityLogsModel, RecentActivityAdapter.VH>(Diff()) {

    class VH(
        val binding: ItemRecentActivityBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {

        val binding = ItemRecentActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return VH(binding)
    }

    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {

        val item = getItem(position)

        with(holder.binding) {

            // set text
            recentActivityTitle.text = item.title
            recentActivitySubtitle.text = item.subtitle

            // set icon + colors according to firebase type
            when (item.type.lowercase()) {

                "create" -> {
                    setActionStyle(
                        icon = R.drawable.ic_add_24dp,
                        iconColor = R.color.green,
                        bgColor = R.color.light_green
                    )
                }

                "update" -> {
                    setActionStyle(
                        icon = R.drawable.ic_edit_24dp,
                        iconColor = R.color.green,
                        bgColor = R.color.light_blue
                    )
                }

                "disabled" -> {
                    setActionStyle(
                        icon = R.drawable.ic_disabled_24dp,
                        iconColor = R.color.red,
                        bgColor = R.color.light_orange
                    )
                }

                "delete" -> {
                    setActionStyle(
                        icon = R.drawable.ic_delete_24dp,
                        iconColor = R.color.red,
                        bgColor = R.color.light_red
                    )
                }

                else -> {
                    setActionStyle(
                        icon = R.drawable.ic_logs_24dp,
                        iconColor = R.color.black,
                        bgColor = R.color.gray
                    )
                }
            }
        }
    }

    // reusable function
    private fun ItemRecentActivityBinding.setActionStyle(
        @DrawableRes icon: Int,
        @ColorRes iconColor: Int,
        @ColorRes bgColor: Int
    ) {

        val context = root.context

        recentCardIcon.setImageResource(icon)

        recentCardIcon.imageTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(context, iconColor)
            )

        recentImgBg.backgroundTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(context, bgColor)
            )
    }

    class Diff : DiffUtil.ItemCallback<ActivityLogsModel>() {

        override fun areItemsTheSame(
            oldItem: ActivityLogsModel,
            newItem: ActivityLogsModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ActivityLogsModel,
            newItem: ActivityLogsModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
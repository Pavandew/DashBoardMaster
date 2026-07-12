package com.example.masterdashboard.manager_single_res_dash.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FormSectionTitleSubtitleBinding
import com.example.masterdashboard.databinding.ItemFormHeaderBinding
import com.example.masterdashboard.databinding.RowDocumentItemBinding
import com.example.masterdashboard.databinding.RowPermissionItemBinding
import com.example.masterdashboard.manager_single_res_dash.models.Step2FormItem

class PermissionsDocumentsAdapter(
    private val items: List<Step2FormItem>,
    private val onUploadClicked: (Step2FormItem.DocumentItem, position: Int) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SECTION = 1
        private const val TYPE_PERMISSION = 2
        private const val TYPE_DOCUMENT = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when(items[position]) {
            is Step2FormItem.Header -> TYPE_HEADER
            is Step2FormItem.SectionTitle -> TYPE_SECTION
            is Step2FormItem.PermissionItem -> TYPE_PERMISSION
            is Step2FormItem.DocumentItem -> TYPE_DOCUMENT
        }

    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemFormHeaderBinding.inflate(inflater, parent, false))
            TYPE_SECTION -> SectionViewHolder(FormSectionTitleSubtitleBinding.inflate(inflater, parent, false))
            TYPE_PERMISSION -> PermissionViewHolder(RowPermissionItemBinding.inflate(inflater, parent, false))
            TYPE_DOCUMENT -> DocumentViewHolder(RowDocumentItemBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type configuration")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Step2FormItem.Header -> {
                val h = holder as HeaderViewHolder
                val context = h.itemView.context

                // Set connector as active
                h.binding.viewProgressLine.setBackgroundColor(context.getColor(R.color.blue_gradient_end))
                
                // Set Step 2 as active
                h.binding.tvStepTwoCircle.setBackgroundResource(R.drawable.bg_circle_primary)
                h.binding.tvStepTwoCircle.setTextColor(context.getColor(android.R.color.white))
                h.binding.tvStepTwoLabel.setTextColor(context.getColor(R.color.blue_gradient_end))
                h.binding.tvStepTwoLabel.setTypeface(null, android.graphics.Typeface.BOLD)
            }

            is Step2FormItem.SectionTitle -> {
                val h = holder as SectionViewHolder
                h.binding.tvSectionTitle.text = item.title
                h.binding.tvSectionSubtitle.text = item.subtitle
            }

            is Step2FormItem.PermissionItem -> {
                val h = holder as PermissionViewHolder
                h.binding.apply {
                    tvPermissionTitle.text = item.title
                    tvPermissionSubtitle.text = item.subtitle
                    ivPermissionIcon.setImageResource(item.iconRes)

                    // Clear previous listener to prevent recycling selection state bugs
                    cbPermission.setOnCheckedChangeListener(null)
                    cbPermission.isChecked = item.isChecked
                    cbPermission.setOnCheckedChangeListener { _, isChecked ->
                        item.isChecked = isChecked
                    }
                }
            }
            is Step2FormItem.DocumentItem -> {
                val h = holder as DocumentViewHolder
                h.binding.apply {
                    tvDocTitle.text = item.title
                    // Dynamically displays confirmation status text once a file URI is successfully attached
                    tvDocSubtitle.text = if (item.isUploaded) "Document Selected Successfully" else item.subtitle
                    ivDocIcon.setImageResource(item.iconRes)

                    btnUploadDoc.setOnClickListener { onUploadClicked(item, position) }
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(val binding: ItemFormHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class SectionViewHolder(val binding: FormSectionTitleSubtitleBinding) : RecyclerView.ViewHolder(binding.root)
    class PermissionViewHolder(val binding: RowPermissionItemBinding) : RecyclerView.ViewHolder(binding.root)
    class DocumentViewHolder(val binding: RowDocumentItemBinding) : RecyclerView.ViewHolder(binding.root)
}
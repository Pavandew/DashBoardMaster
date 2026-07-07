package com.example.masterdashboard.master_dash.create_res.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.*
import com.example.masterdashboard.master_dash.create_res.models.CreateRestaurantItem
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputLayout

class CreateRestaurantAdapter(
    private val items: List<CreateRestaurantItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SECTION = 1
        private const val TYPE_INPUT = 2
        private const val TYPE_BUTTON = 3
        private const val TYPE_LOCATION = 4
        private const val TYPE_UPLOAD = 5
        private const val TYPE_PERMISSION = 6
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CreateRestaurantItem.SectionTitle -> TYPE_SECTION
            is CreateRestaurantItem.InputField -> TYPE_INPUT
            is CreateRestaurantItem.ButtonSection -> TYPE_BUTTON
            is CreateRestaurantItem.LocationSection -> TYPE_LOCATION
            is CreateRestaurantItem.UploadSection -> TYPE_UPLOAD
            is CreateRestaurantItem.PermissionSection -> TYPE_PERMISSION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SECTION -> SectionViewHolder(ItemCreateResSectionBinding.inflate(inflater, parent, false))
            TYPE_INPUT -> InputViewHolder(ItemCreateResInputBinding.inflate(inflater, parent, false))
            TYPE_LOCATION -> LocationViewHolder(ItemCreateResLocationBinding.inflate(inflater, parent, false))
            TYPE_UPLOAD -> UploadViewHolder(ItemCreateResDocumentsBinding.inflate(inflater, parent, false))
            TYPE_PERMISSION -> PermissionViewHolder(ItemCreateResPermissionBinding.inflate(inflater, parent, false))
            else -> ButtonViewHolder(ItemCreateResButtonBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is SectionViewHolder -> holder.bind(item as CreateRestaurantItem.SectionTitle)
            is InputViewHolder -> holder.bind(item as CreateRestaurantItem.InputField)
            is LocationViewHolder -> holder.bind()
            is UploadViewHolder -> holder.bind()
            is PermissionViewHolder -> holder.bind(item as CreateRestaurantItem.PermissionSection)
            is ButtonViewHolder -> holder.bind(item as CreateRestaurantItem.ButtonSection)
        }
    }

    override fun getItemCount() = items.size

    // --- VIEW HOLDERS ---

    inner class SectionViewHolder(private val binding: ItemCreateResSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CreateRestaurantItem.SectionTitle) {
            binding.tvSectionNumber.text = item.sectionNumber
            binding.tvSectionTitle.text = item.title.uppercase() // Professional look
        }
    }

    inner class InputViewHolder(private val binding: ItemCreateResInputBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CreateRestaurantItem.InputField) {
            binding.tvLabel.text = item.label
            binding.etInput.hint = item.hint
            binding.etInput.inputType = item.inputType

            binding.textInputLayout.endIconMode = if (item.isPassword) {
                TextInputLayout.END_ICON_PASSWORD_TOGGLE
            } else {
                TextInputLayout.END_ICON_NONE
            }
        }
    }

    inner class PermissionViewHolder(private val binding: ItemCreateResPermissionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CreateRestaurantItem.PermissionSection) {
            binding.permissionGrid.removeAllViews()

            item.permissions.forEach { permissionName ->
                val checkBox = MaterialCheckBox(binding.root.context).apply {
                    text = permissionName
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.black)) // Ensure you have this colorr
                    buttonTintList = ContextCompat.getColorStateList(context, R.color.mid_app_color)

                    // The "Perfect Position" Logic for 3 columns:
                    val params = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    }
                    layoutParams = params
                }
                binding.permissionGrid.addView(checkBox)
            }
        }
    }

    inner class LocationViewHolder(private val binding: ItemCreateResLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // Optional: Handle map click or lat/lng listeners here
        }
    }

    inner class UploadViewHolder(private val binding: ItemCreateResDocumentsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // Setup click listeners for btnUploadLogo and btnUploadId
        }
    }

    inner class ButtonViewHolder(private val binding: ItemCreateResButtonBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CreateRestaurantItem.ButtonSection) {
            binding.btnSave.text = item.saveText
            binding.btnCancel.text = item.cancelText
        }
    }
}
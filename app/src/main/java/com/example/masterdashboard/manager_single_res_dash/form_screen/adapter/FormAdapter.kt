package com.example.masterdashboard.manager_single_res_dash.form_screen.adapter

import android.app.DatePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.databinding.*
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.FormItem
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar

class FormAdapter(
    private var items: List<FormItem>,
    private val onInputChanged: (String, Any) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun updateData(newItems: List<FormItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    companion object {
        private const val TYPE_PROGRESS = 0
        private const val TYPE_INFO_CARD = 1
        private const val TYPE_HEADER = 2
        private const val TYPE_INPUT = 3
        private const val TYPE_SWITCH = 4
        private const val TYPE_DATE = 5
        private const val TYPE_DROPDOWN = 6
        private const val TYPE_UPLOAD = 7
        private const val TYPE_REVIEW_HEADER = 8
        private const val TYPE_REVIEW_CARD = 9
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is FormItem.StepProgress -> TYPE_PROGRESS
            is FormItem.InfoCard -> TYPE_INFO_CARD
            is FormItem.SectionHeader -> TYPE_HEADER
            is FormItem.SwitchField -> TYPE_SWITCH
            is FormItem.DatePickerField -> TYPE_DATE
            is FormItem.DropdownField -> TYPE_DROPDOWN
            is FormItem.UploadField -> TYPE_UPLOAD
            is FormItem.ReviewHeader -> TYPE_REVIEW_HEADER
            is FormItem.ReviewCard -> TYPE_REVIEW_CARD
            is FormItem.InputField -> TYPE_INPUT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PROGRESS -> ProgressViewHolder(ItemFormProgressBinding.inflate(inflater, parent, false))
            TYPE_INFO_CARD -> InfoCardViewHolder(ItemFormInfoCardBinding.inflate(inflater, parent, false))
            TYPE_HEADER -> HeaderViewHolder(ItemCreateResSectionBinding.inflate(inflater, parent, false))
            TYPE_SWITCH -> SwitchViewHolder(ItemFormSwitchBinding.inflate(inflater, parent, false))
            TYPE_DATE -> DateViewHolder(ItemFormDateBinding.inflate(inflater, parent, false))
            TYPE_DROPDOWN -> DropdownViewHolder(ItemFormDropdownBinding.inflate(inflater, parent, false))
            TYPE_UPLOAD -> UploadViewHolder(ItemFormUploadBinding.inflate(inflater, parent, false))
            TYPE_REVIEW_HEADER -> ReviewHeaderViewHolder(ItemFormReviewHeaderBinding.inflate(inflater, parent, false))
            TYPE_REVIEW_CARD -> ReviewCardViewHolder(ItemFormReviewCardBinding.inflate(inflater, parent, false))
            else -> InputViewHolder(ItemCreateResInputBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ProgressViewHolder -> holder.bind(item as FormItem.StepProgress)
            is InfoCardViewHolder -> holder.bind(item as FormItem.InfoCard)
            is HeaderViewHolder -> holder.bind(item as FormItem.SectionHeader)
            is SwitchViewHolder -> holder.bind(item as FormItem.SwitchField)
            is DateViewHolder -> holder.bind(item as FormItem.DatePickerField)
            is DropdownViewHolder -> holder.bind(item as FormItem.DropdownField)
            is UploadViewHolder -> holder.bind(item as FormItem.UploadField)
            is ReviewHeaderViewHolder -> holder.bind(item as FormItem.ReviewHeader)
            is ReviewCardViewHolder -> holder.bind(item as FormItem.ReviewCard)
            is InputViewHolder -> holder.bind(item as FormItem.InputField, position)
        }
    }

    override fun getItemCount() = items.size

    inner class ReviewHeaderViewHolder(private val binding: ItemFormReviewHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FormItem.ReviewHeader) {
            binding.tvResName.text = item.name
            binding.tvResType.text = item.type
            binding.tvStatusMessage.text = item.status
        }
    }

    inner class ReviewCardViewHolder(private val binding: ItemFormReviewCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FormItem.ReviewCard) {
            binding.tvCardTitle.text = item.title
            binding.tvEdit.setOnClickListener { item.onEditClick() }
            
            binding.llDetailsContainer.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)
            
            item.details.forEach { (label, value) ->
                val rowBinding = LayoutReviewRowBinding.inflate(inflater, binding.llDetailsContainer, false)
                rowBinding.tvLabel.text = label
                rowBinding.tvValue.text = if (value.isNullOrEmpty()) "—" else value
                binding.llDetailsContainer.addView(rowBinding.root)
            }
        }
    }

    inner class ProgressViewHolder(private val binding: ItemFormProgressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FormItem.StepProgress) {
            binding.tvStep.text = item.step
            binding.tvTitle.text = item.title
            binding.tvSubTitle.text = item.subTitle
        }
    }

    inner class InfoCardViewHolder(private val binding: ItemFormInfoCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FormItem.InfoCard) {
            binding.tvInfoMessage.text = item.message
        }
    }

    inner class HeaderViewHolder(private val binding: ItemCreateResSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FormItem.SectionHeader) {
            binding.tvSectionNumber.text = item.sectionNumber
            binding.tvSectionTitle.text = item.title
            binding.tvOptionalTag.visibility = if (item.isOptional) View.VISIBLE else View.GONE
        }
    }

    inner class SwitchViewHolder(private val binding: ItemFormSwitchBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FormItem.SwitchField) {
            binding.tvSwitchTitle.text = item.title
            binding.tvSwitchSubTitle.text = item.subTitle
            
            binding.switchField.setOnCheckedChangeListener(null)
            binding.switchField.isChecked = item.isChecked

            itemView.setOnClickListener {
                binding.switchField.toggle()
            }

            binding.switchField.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                onInputChanged(item.key, isChecked)
            }
        }
    }

    inner class DropdownViewHolder(private val binding: ItemFormDropdownBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FormItem.DropdownField) {
            binding.tvLabel.text = item.label
            binding.autoCompleteTextView.setText(item.selectedValue, false)
            
            val adapter = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, item.options)
            binding.autoCompleteTextView.setAdapter(adapter)
            
            binding.autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                val selected = item.options[position]
                item.selectedValue = selected
                onInputChanged(item.key, selected)
            }
        }
    }

    inner class UploadViewHolder(private val binding: ItemFormUploadBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FormItem.UploadField) {
            binding.tvUploadTitle.text = item.title
            binding.tvUploadHelper.text = item.subTitle
            
            binding.btnUpload.setOnClickListener {
                onInputChanged(item.key, "PICK_IMAGE")
            }
            
            if (item.imageUri != null) {
                binding.viewPlaceholderIcon.visibility = View.GONE
            } else {
                binding.viewPlaceholderIcon.visibility = View.VISIBLE
                binding.ivLogoPreview.setImageResource(com.example.masterdashboard.R.drawable.ic_launcher_background)
            }
        }
    }

    inner class DateViewHolder(private val binding: ItemFormDateBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FormItem.DatePickerField) {
            binding.tvLabel.text = item.label
            binding.etInput.hint = item.hint
            binding.etInput.setText(item.value)
            
            binding.etInput.setOnClickListener {
                val calendar = Calendar.getInstance()
                val datePickerDialog = DatePickerDialog(
                    itemView.context,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = "$dayOfMonth-${month + 1}-$year"
                        binding.etInput.setText(selectedDate)
                        item.value = selectedDate
                        onInputChanged(item.key, selectedDate)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                datePickerDialog.show()
            }
        }
    }

    inner class InputViewHolder(private val binding: ItemCreateResInputBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        private var currentWatcher: TextWatcher? = null

        fun bind(item: FormItem.InputField, position: Int) {
            binding.tvLabel.text = item.label
            binding.etInput.hint = item.hint
            binding.etInput.inputType = item.inputType
            
            if (item.helperText != null) {
                binding.tvHelperText.text = item.helperText
                binding.tvHelperText.visibility = View.VISIBLE
            } else {
                binding.tvHelperText.visibility = View.GONE
            }

            currentWatcher?.let { binding.etInput.removeTextChangedListener(it) }
            binding.etInput.setText(item.value)

            binding.textInputLayout.error = item.error
            binding.textInputLayout.isErrorEnabled = item.error != null

            binding.textInputLayout.endIconMode = if (item.isPassword) {
                TextInputLayout.END_ICON_PASSWORD_TOGGLE
            } else {
                TextInputLayout.END_ICON_NONE
            }
            
            binding.etInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding.etInput.postDelayed({
                        val recyclerView = itemView.parent as? RecyclerView
                        recyclerView?.smoothScrollToPosition(position)
                    }, 300)
                }
            }

            currentWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    item.value = s.toString()
                    if (item.error != null) {
                        item.error = null
                        binding.textInputLayout.error = null
                        binding.textInputLayout.isErrorEnabled = false
                    }
                    onInputChanged(item.key, s.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            binding.etInput.addTextChangedListener(currentWatcher)
        }
    }
}

package com.example.masterdashboard.manager_single_res_dash.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ItemFormHeaderBinding
import com.example.masterdashboard.databinding.ItemFormPersonalBinding
import com.example.masterdashboard.databinding.ItemFormWorkBinding
import com.example.masterdashboard.manager_single_res_dash.models.StaffDataModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddStaffFormAdapter(
    private val initialData: StaffDataModel? = null,
    private val onNextClicked: (
        name: String, mobile: String, email: String, gender: String,
        role: String, department: String, joiningDate: String, shift: String, salary: String
    ) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PERSONAL = 1
        private const val TYPE_WORK = 2
    }

    private var personalHolder: PersonalViewHolder? = null
    private var workHolder: WorkViewHolder? = null
    private var selectedGender: String = initialData?.gender?.takeIf { it.isNotEmpty() } ?: "Male"

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_HEADER ->
                HeaderViewHolder(ItemFormHeaderBinding.inflate(inflater, parent, false))

            TYPE_PERSONAL -> {
                val holder = PersonalViewHolder(ItemFormPersonalBinding.inflate(inflater, parent, false))
                personalHolder = holder
                holder
            }

            TYPE_WORK -> {
                val holder = WorkViewHolder(ItemFormWorkBinding.inflate(inflater, parent, false))
                workHolder = holder
                holder
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is WorkViewHolder -> {
                val context = holder.itemView.context

                // 1. Set up Roles Spinner
                val roles = listOf("Waiter", "Chef", "Cashier", "Manager")
                val rolesAdapter = ArrayAdapter(context, R.layout.item_dropdown_menu_popup, roles)
                rolesAdapter.setDropDownViewResource(R.layout.item_dropdown_menu_popup)
                holder.binding.spinnerRole.adapter = rolesAdapter
                
                // Pre-fill role if exists
                initialData?.role?.let { role ->
                    val index = roles.indexOf(role)
                    if (index >= 0) {
                        holder.binding.spinnerRole.post {
                            holder.binding.spinnerRole.setSelection(index, false)
                        }
                    }
                }

                // 2. Set up Shifts Spinner
                val shifts = listOf("Morning", "Evening", "Night")
                val shiftsAdapter = ArrayAdapter(context, R.layout.item_dropdown_menu_popup, shifts)
                shiftsAdapter.setDropDownViewResource(R.layout.item_dropdown_menu_popup)
                holder.binding.spinnerRoleShift.adapter = shiftsAdapter

                // Pre-fill shift if exists
                initialData?.shift?.let { shift ->
                    val index = shifts.indexOf(shift)
                    if (index >= 0) {
                        holder.binding.spinnerRoleShift.post {
                            holder.binding.spinnerRoleShift.setSelection(index, false)
                        }
                    }
                }

                // Pre-fill joining date and salary
                holder.binding.etJoiningDate.setText(initialData?.joiningDate ?: "")
                holder.binding.etSalary.setText(initialData?.salary ?: "")

                holder.binding.etJoiningDate.setOnClickListener {
                    // initialize material Date Picker configuration builder
                    val datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select Joining Date")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())     // by default highlighting today date
                        .build()

                    // extract fragment manager from context token safely to display dialog overlay window
                    val activity = context as? AppCompatActivity
                    activity?.supportFragmentManager?.let{ manager ->
                        datePicker.show(manager, "JOINING_DATE_PICKER")
                    }

                    // Format and set text when manager picks a date successfully
                    datePicker.addOnPositiveButtonClickListener { selectionTimestamp ->
                        val timeZoneUTC = TimeZone.getTimeZone("UTC")
                        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply{
                            timeZone = timeZoneUTC
                        }

                        val formattedDate = outputFormat.format(Date(selectionTimestamp))

                        // Assign text value dynamically back to edit text view field
                        holder.binding.etJoiningDate.setText(formattedDate)
                    }
                }
                // 3. Handle the action click
                holder.binding.btnNext.setOnClickListener {
                    val pHolder = personalHolder ?: return@setOnClickListener
                    val wHolder = workHolder ?: return@setOnClickListener

                    onNextClicked(
                        pHolder.binding.etFullName.text.toString().trim(),
                        pHolder.binding.etMobileNumber.text.toString().trim(),
                        pHolder.binding.etEmailAddress.text.toString().trim(),
                        selectedGender,
                        wHolder.binding.spinnerRole.selectedItem.toString(),
                        "Service",
                        wHolder.binding.etJoiningDate.text.toString().trim(),
                        wHolder.binding.spinnerRoleShift.selectedItem.toString(),
                        wHolder.binding.etSalary.text.toString().trim()
                    )
                }
            }
            is PersonalViewHolder -> {
                val b = holder.binding

                // Pre-fill text fields
                b.etFullName.setText(initialData?.staffName ?: "")
                b.etMobileNumber.setText(initialData?.mobile ?: "")
                b.etEmailAddress.setText(initialData?.email ?: "")

                // Helper function to update UI without rebinding the whole item
                fun updateGenderUI() {
                    b.tvGenderMale.isSelected = (selectedGender == "Male")
                    b.tvGenderFemale.isSelected = (selectedGender == "Female")
                    b.tvGenderOther.isSelected = (selectedGender == "Other")
                }

                // Set initial state
                updateGenderUI()

                // Click listener for Male
                b.tvGenderMale.setOnClickListener {
                    selectedGender = "Male"
                    updateGenderUI()
                }

                // Click listener for Female
                b.tvGenderFemale.setOnClickListener {
                    selectedGender = "Female"
                    updateGenderUI()
                }

                // Click listener for Other
                b.tvGenderOther.setOnClickListener {
                    selectedGender = "Other"
                    updateGenderUI()
                }
            }
        }
    }

    override fun getItemCount(): Int = 3

    class HeaderViewHolder(binding: ItemFormHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class PersonalViewHolder(val binding: ItemFormPersonalBinding) : RecyclerView.ViewHolder(binding.root)
    class WorkViewHolder(val binding: ItemFormWorkBinding) : RecyclerView.ViewHolder(binding.root)
}



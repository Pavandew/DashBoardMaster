package com.example.masterdashboard.manager_single_res_dash.adapter

import android.text.Editable
import android.text.TextWatcher
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

    private var inputName: String = initialData?.staffName ?: ""
    private var inputMobile: String = initialData?.mobile ?: ""
    private var inputEmail: String = initialData?.email ?: ""
    private var selectedGender: String = initialData?.gender?.takeIf { it.isNotEmpty() } ?: "Male"
    private var inputJoiningDate: String = initialData?.joiningDate ?: ""
    private var inputSalary: String = initialData?.salary ?: ""

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
                val roles = listOf("Waiter", "Head Waiter", "Chef", "Master Chef", "Cashier", "Manager")
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
                holder.binding.etJoiningDate.setText(inputJoiningDate)
                holder.binding.etSalary.setText(inputSalary)

                holder.binding.etJoiningDate.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        inputJoiningDate = s?.toString()?.trim() ?: ""
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                holder.binding.etSalary.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        inputSalary = s?.toString()?.trim() ?: ""
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                holder.binding.etJoiningDate.setOnClickListener {
                    val datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select Joining Date")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build()

                    val activity = context as? AppCompatActivity
                    activity?.supportFragmentManager?.let { manager ->
                        datePicker.show(manager, "JOINING_DATE_PICKER")
                    }

                    datePicker.addOnPositiveButtonClickListener { selectionTimestamp ->
                        val timeZoneUTC = TimeZone.getTimeZone("UTC")
                        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply {
                            timeZone = timeZoneUTC
                        }

                        val formattedDate = outputFormat.format(Date(selectionTimestamp))
                        holder.binding.etJoiningDate.setText(formattedDate)
                        inputJoiningDate = formattedDate
                    }
                }

                // 3. Handle the action click
                holder.binding.btnNext.setOnClickListener {
                    val pHolder = personalHolder
                    val wHolder = workHolder

                    val fullName = pHolder?.binding?.etFullName?.text?.toString()?.trim() ?: inputName
                    val mobileNum = pHolder?.binding?.etMobileNumber?.text?.toString()?.trim() ?: inputMobile
                    val emailAddr = pHolder?.binding?.etEmailAddress?.text?.toString()?.trim() ?: inputEmail
                    val roleStr = wHolder?.binding?.spinnerRole?.selectedItem?.toString() ?: initialData?.role ?: "Waiter"
                    val deptStr = getDepartmentForRole(roleStr)
                    val joiningDateStr = wHolder?.binding?.etJoiningDate?.text?.toString()?.trim() ?: inputJoiningDate
                    val shiftStr = wHolder?.binding?.spinnerRoleShift?.selectedItem?.toString() ?: initialData?.shift ?: "Morning"
                    val salaryStr = wHolder?.binding?.etSalary?.text?.toString()?.trim() ?: inputSalary

                    onNextClicked(
                        fullName,
                        mobileNum,
                        emailAddr,
                        selectedGender,
                        roleStr,
                        deptStr,
                        joiningDateStr,
                        shiftStr,
                        salaryStr
                    )
                }
            }
            is PersonalViewHolder -> {
                val b = holder.binding

                // Pre-fill text fields
                b.etFullName.setText(inputName)
                b.etMobileNumber.setText(inputMobile)
                b.etEmailAddress.setText(inputEmail)

                b.etFullName.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        inputName = s?.toString()?.trim() ?: ""
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                b.etMobileNumber.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        inputMobile = s?.toString()?.trim() ?: ""
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                b.etEmailAddress.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        inputEmail = s?.toString()?.trim() ?: ""
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                fun updateGenderUI() {
                    b.tvGenderMale.isSelected = (selectedGender == "Male")
                    b.tvGenderFemale.isSelected = (selectedGender == "Female")
                    b.tvGenderOther.isSelected = (selectedGender == "Other")
                }

                updateGenderUI()

                b.tvGenderMale.setOnClickListener {
                    selectedGender = "Male"
                    updateGenderUI()
                }

                b.tvGenderFemale.setOnClickListener {
                    selectedGender = "Female"
                    updateGenderUI()
                }

                b.tvGenderOther.setOnClickListener {
                    selectedGender = "Other"
                    updateGenderUI()
                }
            }
        }
    }

    private fun getDepartmentForRole(role: String): String {
        return when (role.lowercase()) {
            "chef", "kitchen", "cook" -> "Kitchen"
            "cashier", "billing" -> "Billing"
            "manager" -> "Management"
            else -> "Service"
        }
    }

    override fun getItemCount(): Int = 3

    class HeaderViewHolder(binding: ItemFormHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class PersonalViewHolder(val binding: ItemFormPersonalBinding) : RecyclerView.ViewHolder(binding.root)
    class WorkViewHolder(val binding: ItemFormWorkBinding) : RecyclerView.ViewHolder(binding.root)
}

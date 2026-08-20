package com.example.masterdashboard.manager_single_res_dash.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentAddTableBinding
import com.example.masterdashboard.manager_single_res_dash.viewModel.TableItemListViewModel
import com.example.masterdashboard.utils.SessionManager

class AddTableFragment : Fragment() {

    companion object {
        private const val TAG = "AddTableFragment ----> "
    }

    private var _binding: FragmentAddTableBinding? = null
    private val binding get() = _binding!!

    // Attaching same shared view model tier targeting your safe isolated business methods [cite: 325]
    private val viewModel: TableItemListViewModel by activityViewModels()
    private val sessionManager by lazy { SessionManager(requireContext()) }

    private var preSelectedFloorId: String = ""
    private var preSelectedFloorName: String = ""

    // Track selection states for Table Type button group explicitly
    private var selectedTableType: String = "Regular"
    private var currentSeatCapacity: Int = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            preSelectedFloorId = it.getString("PRE_SELECTED_FLOOR_ID", "")
            preSelectedFloorName = it.getString("PRE_SELECTED_FLOOR_NAME", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Form Opened. Context target floor preset value: $preSelectedFloorName")

        setupToolbar()
        setupDropdownMenus()
        setupCapacityCounter()
        setupTableTypeSelection()
        setupSaveActionTrigger()
    }

    private fun setupToolbar() {
        val toolbar = binding.addTableToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = "Add New Table"
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupDropdownMenus() {
        // Floor Selection Dropdown setup [cite: 325]
        val floorNames = listOf(preSelectedFloorName)
        val floorAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_menu_popup, floorNames)
        binding.actvFloorDropdown.setAdapter(floorAdapter)
        binding.actvFloorDropdown.setText(preSelectedFloorName, false)

        // Status Selection Dropdown setup matching your screenshot validation constraints [cite: 325]
        val statuses = listOf("Available", "Occupied", "Reserved", "Dirty")
        val statusAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_menu_popup, statuses)
        binding.actvStatusDropdown.setAdapter(statusAdapter)
        binding.actvStatusDropdown.setText("Available", false)
    }

    private fun setupCapacityCounter() {
        binding.tvCapacityValue.text = currentSeatCapacity.toString()

        binding.btnDecrementCapacity.setOnClickListener {
            if (currentSeatCapacity > 1) {
                currentSeatCapacity--
                binding.tvCapacityValue.text = currentSeatCapacity.toString()
            }
        }

        binding.btnIncrementCapacity.setOnClickListener {
            if (currentSeatCapacity < 20) {
                currentSeatCapacity++
                binding.tvCapacityValue.text = currentSeatCapacity.toString()
            }
        }
    }

    /**
     * Programmatic background switching logic that mirrors your exact gender selection setup[cite: 457, 475].
     * Avoids XML file state list selectors entirely[cite: 468, 471].
     */
    private fun setupTableTypeSelection() {
        val b = binding

        // Helper tracking method matching your exact "updateGenderUI()" functional style [cite: 457, 475]
        fun updateTableTypeUI() {
            // Regular Selection Cell Highlight Verification
            b.btnTypeRegular.background = ContextCompat.getDrawable(
                requireContext(),
                if (selectedTableType == "Regular") R.drawable.bg_btn_color_gradient else R.color.bg_card
            )

            // Family Selection Cell Highlight Verification
            b.btnTypeFamily.background = ContextCompat.getDrawable(
                requireContext(),
                if (selectedTableType == "Family") R.drawable.bg_btn_color_gradient else R.color.bg_card
            )

            // VIP Selection Cell Highlight Verification
            b.btnTypeVip.background = ContextCompat.getDrawable(
                requireContext(),
                if (selectedTableType == "VIP") R.drawable.bg_btn_color_gradient else R.color.bg_card
            )

            // Outdoor Selection Cell Highlight Verification
            b.btnTypeOutdoor.background = ContextCompat.getDrawable(
                requireContext(),
                if (selectedTableType == "Outdoor") R.drawable.bg_btn_color_gradient else R.color.bg_card
            )
        }

        // Apply fallback initialization background focus configurations on load [cite: 475]
        updateTableTypeUI()

        // Core click listener mappings [cite: 457]
        b.btnTypeRegular.setOnClickListener {
            selectedTableType = "Regular"
            updateTableTypeUI()
            Log.d(TAG, "Table type choice explicitly swapped over to: $selectedTableType")
        }

        b.btnTypeFamily.setOnClickListener {
            selectedTableType = "Family"
            updateTableTypeUI()
            Log.d(TAG, "Table type choice explicitly swapped over to: $selectedTableType")
        }

        b.btnTypeVip.setOnClickListener {
            selectedTableType = "VIP"
            updateTableTypeUI()
            Log.d(TAG, "Table type choice explicitly swapped over to: $selectedTableType")
        }

        b.btnTypeOutdoor.setOnClickListener {
            selectedTableType = "Outdoor"
            updateTableTypeUI()
            Log.d(TAG, "Table type choice explicitly swapped over to: $selectedTableType")
        }
    }

    private fun setupSaveActionTrigger() {
        binding.btnSaveTableAction.setOnClickListener {
            val rawInput = binding.etTableNameField.text?.toString()?.trim() ?: ""
            val selectedStatusInput = binding.actvStatusDropdown.text.toString().trim()
            val ownerUid = sessionManager.getUid()

            if (rawInput.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a table number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Automatically format the table name to 't-01', 't-10', etc.
            val tableNum = rawInput.toIntOrNull()
            val tableNameInput = if (tableNum != null) {
                "T-${String.format(java.util.Locale.US, "%02d", tableNum)}"
            } else {
                rawInput // Fallback
            }

            if (ownerUid.isEmpty() || preSelectedFloorId.isEmpty()) {
                Log.e(TAG, "Execution Blocked: Session reference paths evaluate to null entries.")
                return@setOnClickListener
            }

            Log.i(TAG, "Form Submission Verified. Shipping table definition records: $tableNameInput")

            // Dispatch dynamic addition parameters securely with live selected status choices [cite: 325]
            viewModel.addNewTable(
                ownerUid = ownerUid,
                floorId = preSelectedFloorId,
                tableName = tableNameInput,
                capacity = currentSeatCapacity,
                status = selectedStatusInput
            )

            Toast.makeText(requireContext(), "Table $tableNameInput added successfully", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack() // Seamless backward routing return mapping task
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
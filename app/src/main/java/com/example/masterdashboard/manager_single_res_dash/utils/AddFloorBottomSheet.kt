package com.example.masterdashboard.manager_single_res_dash.utils


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.masterdashboard.databinding.AddFloorBottomupSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Custom Bottom Sheet Dialog for adding new restaurant floors.
 * Reuses the exact operational design pattern of AddCategoryBottomSheet.
 */
class AddFloorBottomSheet(
    private val onSaveClicked: (floorName: String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: AddFloorBottomupSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddFloorBottomupSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBottomSheetSaveFloor.setOnClickListener {
            val floorName = binding.etBottomSheetFloorName.text.toString().trim()

            if (floorName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a floor name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pass the data back to the parent Fragment/ViewModel
            onSaveClicked(floorName)
            dismiss() // Close the bottom sheet safely
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
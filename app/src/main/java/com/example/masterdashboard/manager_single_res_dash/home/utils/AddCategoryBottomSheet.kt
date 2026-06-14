package com.example.masterdashboard.manager_single_res_dash.home.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.masterdashboard.databinding.BottomUpSheetAddCategoryBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddCategoryBottomSheet (
    private val onSaveClicked: (categoryName: String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomUpSheetAddCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomUpSheetAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBottomSheetSaveCategory.setOnClickListener {
            val categoryName = binding.etBottomSheetCategoryName.text.toString().trim()

            if (categoryName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pass the data back to the parent Fragment/ViewModel
            onSaveClicked(categoryName)
            dismiss() // Close the bottom sheet safely
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
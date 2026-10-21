package com.restroone.app.manager_single_res_dash.utils

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.restroone.app.R
import com.restroone.app.databinding.BottomUpSheetAddCategoryBinding
import com.restroone.app.utils.DocumentUploadManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddCategoryBottomSheet (
    private val onSaveClicked: (categoryName: String, imageUri: Uri?) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomUpSheetAddCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var uploadManager: DocumentUploadManager
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomUpSheetAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uploadManager = DocumentUploadManager(this)

        binding.cardCategoryImgPicker.setOnClickListener {
            uploadManager.selectDocument { uri ->
                selectedImageUri = uri
                binding.ivCategoryPreview.setImageURI(uri)
                binding.ivCategoryPreview.scaleType = ImageView.ScaleType.CENTER_CROP
                binding.ivCategoryPreview.imageTintList = null
                binding.ivClearCategoryImg.visibility = View.VISIBLE
            }
        }

        binding.ivClearCategoryImg.setOnClickListener {
            selectedImageUri = null
            binding.ivCategoryPreview.setImageResource(R.drawable.ic_restaurant_24dp)
            binding.ivCategoryPreview.scaleType = ImageView.ScaleType.CENTER
            binding.ivClearCategoryImg.visibility = View.GONE
        }

        binding.btnBottomSheetSaveCategory.setOnClickListener {
            val categoryName = binding.etBottomSheetCategoryName.text.toString().trim()

            if (categoryName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onSaveClicked(categoryName, selectedImageUri)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.masterdashboard.manager_single_res_dash.views

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentAddMenuItemBinding
import com.example.masterdashboard.databinding.ItemMenuVariantBinding
import com.example.masterdashboard.manager_single_res_dash.viewModel.MenuItemViewModel
import com.example.masterdashboard.manager_single_res_dash.models.ItemVariant
import com.example.masterdashboard.manager_single_res_dash.models.MenuFoodItemsData
import com.example.masterdashboard.manager_single_res_dash.form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.utils.DocumentUploadManager
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.launch

class AddMenuItemFragment : Fragment() {
    companion object {
        private const val TAG = "AddFoodItemFragment"
    }

    private var _binding: FragmentAddMenuItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MenuItemViewModel by viewModels()
    private val uploadManager = DocumentUploadManager(this)
    private var selectedImageUri: Uri? = null

    private var categoryId: String = ""
    private var categoryName: String = ""
    private var isVegSelected: Boolean = true
    private var editingItem: MenuFoodItemsData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString("CATEGORY_ID", "")
            categoryName = it.getString("CATEGORY_NAME", "New Item")
            editingItem = it.getSerializable("EDIT_ITEM") as? MenuFoodItemsData
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMenuItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: AddMenuItemFragment Opened")

        setupToolbar()
        setupFormViewDefaults()
        setupImagePicker()
        setupVariantsLogic()
        observeViewModel()

        editingItem?.let { populateEditData(it) }

        binding.btnSaveItem.setOnClickListener {
            executeSaveSequence()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveState.collect { state ->
                when (state) {
                    is RegistrationUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnSaveItem.isEnabled = false
                    }
                    is RegistrationUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), if (editingItem != null) "Item updated!" else "Item saved!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                    is RegistrationUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSaveItem.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.addFoodItemToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = if (editingItem != null) "Edit Menu Item" else "Add Menu Item"
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupFormViewDefaults() {
        binding.etMenuFoodCategory.setText(categoryName)

        val statusOptions = arrayOf("Active", "Out Of Stock")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusOptions)
        binding.spinnerMenuFoodStatus.adapter = adapter

        // Set baseline default configuration state layout (Veg selected initially)
        binding.llOptionVeg.isSelected = true
        binding.llOptionNonVeg.isSelected = false

        binding.llOptionVeg.setOnClickListener {
            if (!isVegSelected) {
                isVegSelected = true
                binding.llOptionVeg.isSelected = true
                binding.llOptionNonVeg.isSelected = false
                animateSelectionFeedback(binding.llOptionVeg)
            }
        }

        binding.llOptionNonVeg.setOnClickListener {
            if (isVegSelected) {
                isVegSelected = false
                binding.llOptionVeg.isSelected = false
                binding.llOptionNonVeg.isSelected = true
                animateSelectionFeedback(binding.llOptionNonVeg)
            }
        }
    }

    private fun setupImagePicker() {
        binding.cardFoodImagePicker.setOnClickListener {
            uploadManager.selectDocument { uri ->
                selectedImageUri = uri
                binding.ivFoodImagePreview.setImageURI(uri)
                binding.ivFoodImagePreview.scaleType = ImageView.ScaleType.CENTER_CROP
                binding.ivFoodImagePreview.setPadding(0, 0, 0, 0)
                binding.ivClearSelectedImage.visibility = View.VISIBLE
            }
        }

        binding.ivClearSelectedImage.setOnClickListener {
            selectedImageUri = null
            binding.ivFoodImagePreview.setImageResource(R.drawable.camera)
            binding.ivFoodImagePreview.setPadding(0, 0, 0, 0)
            binding.ivClearSelectedImage.visibility = View.GONE
        }
    }

    private fun setupVariantsLogic() {
        binding.switchHasVariants.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.llPriceContainer.visibility = View.GONE
                binding.llVariantsContainer.visibility = View.VISIBLE
                if (binding.variantsList.childCount == 0 && editingItem == null) {
                    addVariantRow() // Add one empty row by default for new items
                }
            } else {
                binding.llPriceContainer.visibility = View.VISIBLE
                binding.llVariantsContainer.visibility = View.GONE
            }
        }

        binding.btnAddVariant.setOnClickListener {
            addVariantRow()
        }
    }

    private fun addVariantRow(name: String = "", price: Double = 0.0) {
        val inflater = LayoutInflater.from(requireContext())
        val variantBinding = ItemMenuVariantBinding.inflate(inflater, binding.variantsList, true)
        
        variantBinding.etVariantName.setText(name)
        variantBinding.etVariantPrice.setText(if (price > 0) price.toString() else "")

        variantBinding.btnRemoveVariant.setOnClickListener {
            binding.variantsList.removeView(variantBinding.root)
        }
    }

    private fun populateEditData(item: MenuFoodItemsData) {
        binding.etMenuFoodName.setText(item.itemName)
        binding.etMenuFoodDescription.setText(item.description)
        
        // Image
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(item.imageUrl)
                .placeholder(R.drawable.person)
                .into(binding.ivFoodImagePreview)
            binding.ivFoodImagePreview.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.ivFoodImagePreview.setPadding(0, 0, 0, 0)
            binding.ivClearSelectedImage.visibility = View.VISIBLE
        }

        // Status Spinner
        val statusIndex = if (item.status.equals("Active", true)) 0 else 1
        binding.spinnerMenuFoodStatus.setSelection(statusIndex)

        // Food Classification
        isVegSelected = item.isVeg
        binding.llOptionVeg.isSelected = item.isVeg
        binding.llOptionNonVeg.isSelected = !item.isVeg

        // Variants logic
        if (item.hasVariants) {
            binding.switchHasVariants.isChecked = true
            binding.llPriceContainer.visibility = View.GONE
            binding.llVariantsContainer.visibility = View.VISIBLE
            
            item.variants.forEach { variant ->
                addVariantRow(variant.variantName, variant.price)
            }
        } else {
            binding.switchHasVariants.isChecked = false
            binding.llPriceContainer.visibility = View.VISIBLE
            binding.llVariantsContainer.visibility = View.GONE
            binding.etMenuFoodPrice.setText(item.price)
        }
        
        binding.btnSaveItem.text = "Update Item"
    }

    private fun executeSaveSequence() {
        val name = binding.etMenuFoodName.text.toString().trim()
        val hasVariants = binding.switchHasVariants.isChecked
        val description = binding.etMenuFoodDescription.text.toString().trim()
        val status = binding.spinnerMenuFoodStatus.selectedItem.toString()
        val ownerUid = SessionManager(requireContext()).getUid()

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter item name", Toast.LENGTH_SHORT).show()
            return
        }

        var basePrice = ""
        val variants = mutableListOf<ItemVariant>()

        if (hasVariants) {
            for (i in 0 until binding.variantsList.childCount) {
                val row = binding.variantsList.getChildAt(i)
                val vName = row.findViewById<EditText>(R.id.etVariantName).text.toString().trim()
                val vPriceStr = row.findViewById<EditText>(R.id.etVariantPrice).text.toString().trim()
                val vPrice = vPriceStr.toDoubleOrNull() ?: 0.0
                
                if (vName.isNotEmpty() && vPrice > 0) {
                    variants.add(ItemVariant(vName, vPrice))
                }
            }
            if (variants.isEmpty()) {
                Toast.makeText(requireContext(), "Please add at least one valid variant", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            basePrice = binding.etMenuFoodPrice.text.toString().trim()
            if (basePrice.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter price", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (ownerUid.isEmpty() || categoryId.isEmpty()) {
            Toast.makeText(requireContext(), "Session error.", Toast.LENGTH_SHORT).show()
            return
        }

        val itemId = editingItem?.id ?: ""
        val existingImageUrl = editingItem?.imageUrl ?: ""

        viewModel.uploadAndSaveMenuItem(
            ownerUid = ownerUid,
            categoryId = categoryId,
            itemName = name,
            price = basePrice,
            description = description,
            status = status,
            isVeg = isVegSelected,
            imageUri = selectedImageUri,
            hasVariants = hasVariants,
            variants = variants,
            itemId = itemId,
            existingImageUrl = existingImageUrl
        )
    }

    private fun animateSelectionFeedback(view: View) {
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(150)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

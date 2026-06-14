package com.example.masterdashboard.manager_single_res_dash.home.views

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentAddMenuItemBinding
import com.example.masterdashboard.manager_single_res_dash.home.viewModel.MenuItemViewModel
import com.example.masterdashboard.utils.DocumentUploadManager
import com.example.masterdashboard.utils.SessionManager

class AddMenuItemFragment : Fragment() {
    companion object {
        private const val TAG = "AddFoodItemFragment"
    }

    private var _binding: FragmentAddMenuItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MenuItemViewModel by viewModels()
    private lateinit var uploadManager: DocumentUploadManager
    private var selectedImageUri: Uri? = null

    private var categoryId: String = ""
    private var categoryName: String = ""
    //  Declare the classification state tracker at the top of your class
    private var isVegSelected: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString("CATEGORY_ID", "")
            categoryName = it.getString("CATEGORY_NAME", "New Item")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAddMenuItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Navigation: AddMenuItemFragment Opened")
        Log.d(TAG, "onViewCreated loaded for adding items into category ID: $categoryId")

        uploadManager = DocumentUploadManager(this)

        setupToolbar()
        setupFormViewDefaults()
        setupImagePicker()

        binding.btnSaveItem.setOnClickListener {
            executeSaveSequence()
        }
    }
    private fun setupToolbar() {
        val toolbar = binding.addFoodItemToolbar
        val context = requireContext()
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        toolbar.customToolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_main))
        toolbar.tvToolbarTitle.text = "Add Menu Item"
        toolbar.tvToolbarTitle.setTextColor(whiteColor)

        toolbar.toolbarImgMenu.setColorFilter(whiteColor)
        toolbar.llSubtitleContainer.visibility = View.GONE
        toolbar.toolbarImgNotification.visibility = View.GONE
        toolbar.toolbarImgMenu.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupFormViewDefaults() {
        // Set up the static text displaying what category they are adding into
        binding.etMenuFoodCategory.setText(categoryName)

        // populate Status Spinner dropdown element Array
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
            binding.ivFoodImagePreview.setImageResource(R.drawable.person)  // camera
            binding.ivFoodImagePreview.setPadding(0, 0, 0, 0)
            binding.ivClearSelectedImage.visibility = View.VISIBLE
        }
    }

    private fun executeSaveSequence() {
        val name = binding.etMenuFoodName.text.toString().trim()
        val price = binding.etMenuFoodPrice.text.toString().trim()
        val description = binding.etMenuFoodDescription.text.toString().trim()
        val status = binding.spinnerMenuFoodStatus.selectedItem.toString()
        val finalIsVegFlag = isVegSelected                          // Pass this directly to your save function!
        val ownerUid = SessionManager(requireContext()).getUid()

        if (name.isEmpty() || price.isEmpty()) {
            Toast.makeText(requireContext(), "Please complete all fields marked with (*)", Toast.LENGTH_SHORT).show()
            return
        }

        if (ownerUid.isEmpty() || categoryId.isEmpty()) {
            Toast.makeText(requireContext(), "Error linking session context profiles path elements.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Submit input variables out to Firestore viewmodel engine pipelines
        viewModel.saveMenuFoodItem(ownerUid, categoryId, name, price, description, status, isVegSelected)

        // 2. Alert user and pop back automatically to FoodItemListFragment
        Toast.makeText(requireContext(), "$name saved successfully!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    // Gives premium bouncy physics animation feedback when tapped
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
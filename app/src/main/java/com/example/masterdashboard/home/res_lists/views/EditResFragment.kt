package com.example.masterdashboard.home.res_lists.views

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentEditResBinding
import com.google.android.material.textfield.TextInputLayout

class EditResFragment : Fragment() {

    private var _binding: FragmentEditResBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEditResBinding.inflate(
            inflater,
            container,
            false
        )

        setupToolbar()
        setupFields()
        setupClickListeners()
        setupBackPress()

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        requireActivity()
            .findViewById<View>(R.id.host_bottom_nav)
            .visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity()
            .findViewById<View>(R.id.host_bottom_nav)
            .visibility = View.VISIBLE

        _binding = null
    }

    // Toolbar
    private fun setupToolbar() {

        binding.editResToolbar.toolbarTvTitle.text =
            getString(R.string.edit_restaurant)

        binding.editResToolbar.toolbarImgMenu.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_arrow_back_24dp
            )
        )

        binding.editResToolbar.toolbarImgMenu.setOnClickListener {
            goBack()
        }

        binding.editResToolbar.toolbarImgNotification.visibility = View.GONE
    }

    // Form Fields
    private fun setupFields() {

        // Restaurant Name
        binding.fieldResName.tvInputLabel.text = "Restaurant Name *"

        binding.fieldResName.etInput.hint = "Enter restaurant name"

        binding.fieldResName.etInput.setText("Pizza Hut")

        // Username
        binding.fieldUsername.tvInputLabel.text = "Username *"

        binding.fieldUsername.etInput.hint = "Enter username"

        binding.fieldUsername.etInput.setText("pizzahut_admin")

        // Owner Name
        binding.fieldOwnerName.tvInputLabel.text = "Owner Name *"

        binding.fieldOwnerName.etInput.hint = "Enter owner name"

        binding.fieldOwnerName.etInput.setText("John Doe")

        // Phone
        binding.fieldPhone.tvInputLabel.text = "Phone Number *"

        binding.fieldPhone.etInput.hint = "Enter phone number"

        binding.fieldPhone.etInput.inputType = InputType.TYPE_CLASS_PHONE

        binding.fieldPhone.etInput.setText("9876543210")

        // Email
        binding.fieldEmail.tvInputLabel.text = "Email Address *"

        binding.fieldEmail.etInput.hint = "Enter email"

        binding.fieldEmail.etInput.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        binding.fieldEmail.etInput.setText("pizza@gmail.com"
        )

        // New Password
        binding.fieldNewPass.tvInputLabel.text = "New Password"

        binding.fieldNewPass.etInput.hint = "Enter new password"

        binding.fieldNewPass.etInput.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD

        binding.fieldNewPass.textInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
    }

    // Click Events
    private fun setupClickListeners() {

        binding.btnCancel.setOnClickListener {
            goBack()
        }

        binding.btnUpdate.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Restaurant Updated",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnResetPassword.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Password Reset Successfully",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnDelete.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Moved To Trash",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Back Press
    private fun setupBackPress() {

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) {

                goBack()
            }
    }

    private fun goBack() {
        parentFragmentManager.popBackStack()
    }
}
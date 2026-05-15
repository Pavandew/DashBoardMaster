package com.example.masterdashboard.home.create_res

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.FragmentCreateNewResBinding
import com.google.android.material.textfield.TextInputLayout

class CreateNewResFragment : Fragment() {

    // ViewBinding
    private var _binding: FragmentCreateNewResBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCreateNewResBinding.inflate(
            inflater,
            container,
            false
        )

        setupToolbar()
        setupInputs()

        return binding.root
    }

    // Toolbar
    private fun setupToolbar() {

        binding.newResToolbar.toolbarTvTitle.setText(
            R.string.create_new_res
        )

        binding.newResToolbar.toolbarImgMenu.visibility =
            View.GONE
    }

    // Input Fields
    private fun setupInputs() {

        // Restaurant Name
        binding.fieldResName.tvInputLabel.text = "Restaurant Name *"

        binding.fieldResName.etInput.hint = "Enter restaurant name"

        // Email
        binding.fieldEmail.tvInputLabel.text = "Email Address *"

        binding.fieldEmail.etInput.hint = "Enter email address"

        binding.fieldEmail.etInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        // Owner Name
        binding.fieldOwner.tvInputLabel.text = "Owner Name *"

        binding.fieldOwner.etInput.hint = "Enter owner name"

        // Password
        binding.fieldPassword.tvInputLabel.text = "Password *"

        binding.fieldPassword.etInput.hint = "Enter password"

        binding.fieldPassword.etInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        binding.fieldPassword.textInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE

        // Phone
        binding.fieldPhone.tvInputLabel.text = "Phone Number *"

        binding.fieldPhone.etInput.hint = "Enter phone number"

        binding.fieldPhone.etInput.inputType = InputType.TYPE_CLASS_PHONE

        // Confirm Password
        binding.fieldConfirmPass.tvInputLabel.text = "Confirm Password *"

        binding.fieldConfirmPass.etInput.hint = "Re-enter password"

        binding.fieldConfirmPass.etInput.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD

        binding.fieldConfirmPass.textInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE

        // Username
        binding.fieldUsername.tvInputLabel.text = "Username *"

        binding.fieldUsername.etInput.hint = "Enter username"
    }


    private fun clickListeners() {

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
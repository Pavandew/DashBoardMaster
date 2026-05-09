package com.example.masterdashboard.home.settings.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.example.masterdashboard.R
import com.example.masterdashboard.databinding.ActivityHomeBinding
import com.example.masterdashboard.databinding.FragmentProfileBinding
import com.example.masterdashboard.home.HomeActivity

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView
    private lateinit var drawerMenuIcon: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupFields()
    }

    private fun setupToolbar() {

        binding.settingsToolbar.toolbarTvTitle.text = getString(R.string.settings)

        binding.settingsToolbar.toolbarImgMenu.visibility = View.VISIBLE

        binding.settingsToolbar.toolbarImgMenu.setOnClickListener {
            (activity as HomeActivity).openDrawer()
        }

    }

    private fun setupFields() {
        binding.profileFullName.tvInputLabel.text = "Full Name"
        binding.profileFullName.etInput.hint = "Enter full name"

        binding.profileUsername.tvInputLabel.text = "Username"
        binding.profileUsername.etInput.hint = "Enter username"

        binding.profileEmail.tvInputLabel.text = "Email Address"
        binding.profileEmail.etInput.hint = "Enter email"

        binding.profilePhone.tvInputLabel.text = "Phone Number"
        binding.profilePhone.etInput.hint = "Enter phone number"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
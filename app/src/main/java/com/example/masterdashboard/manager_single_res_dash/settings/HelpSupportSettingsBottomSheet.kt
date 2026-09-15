package com.example.masterdashboard.manager_single_res_dash.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.masterdashboard.databinding.BottomSheetHelpSupportBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class HelpSupportSettingsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "HelpSupportSettingsBottomSheet"
    }

    private var _binding: BottomSheetHelpSupportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHelpSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCallSupport.setOnClickListener {
            val phone = binding.tvSupportPhone.text.toString().trim()
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to launch dialer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

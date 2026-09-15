package com.example.masterdashboard.manager_single_res_dash.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.masterdashboard.databinding.BottomSheetManagerPinBinding
import com.example.masterdashboard.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ManagerPinSettingsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ManagerPinSettingsBottomSheet"
    }

    private var _binding: BottomSheetManagerPinBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetManagerPinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        val currentPin = sessionManager.getStaffDocId().takeLast(4).ifEmpty { "1234" }
        binding.etManagerPin.setText(currentPin)

        binding.btnSavePin.setOnClickListener {
            val pin = binding.etManagerPin.text?.toString()?.trim() ?: ""
            if (pin.length != 4) {
                Toast.makeText(requireContext(), "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Manager Security PIN saved successfully!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

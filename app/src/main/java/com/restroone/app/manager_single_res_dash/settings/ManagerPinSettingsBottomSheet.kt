package com.restroone.app.manager_single_res_dash.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.restroone.app.databinding.BottomSheetManagerPinBinding
import com.restroone.app.utils.SessionManager
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

        val currentPin = sessionManager.getManagerPin()
        binding.etManagerPin.setText(currentPin)

        binding.btnSavePin.setOnClickListener {
            val pin = binding.etManagerPin.text?.toString()?.trim() ?: ""
            if (pin.length != 4 || !pin.all { it.isDigit() }) {
                Toast.makeText(requireContext(), "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save to SessionManager
            sessionManager.saveManagerPin(pin)

            // Save to Firestore for manager/owner account
            val ownerUid = sessionManager.getUid()
            if (ownerUid.isNotEmpty()) {
                val docRef = com.restroone.app.utils.RestaurantPathHelper.getRestaurantDocRef(ownerUid)
                docRef.set(mapOf("managerPin" to pin), com.google.firebase.firestore.SetOptions.merge())
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

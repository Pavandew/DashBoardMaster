package com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.RegistrationDataModel

/**
 * REGISTRATION DATA VIEWMODEL (THE "BANK")
 * Purpose: This ViewModel is scoped to the Activity. It acts as a central repository (Bank) 
 * for the restaurant registration data. 
 * Why: By being Activity-scoped, this data survives fragment transitions and back presses, 
 * ensuring that data entered in Step 1 is still available when the user returns from Step 2.
 */
class RegistrationDataViewModel : ViewModel() {

    companion object {
        private const val TAG = "RegistrationDataVM"
    }

    // The single source of truth for the entire 7-step registration process
    private var _registrationData = RegistrationDataModel()
    val registrationData: RegistrationDataModel get() = _registrationData

    private var _isEditMode = false
    val isEditMode: Boolean get() = _isEditMode

    fun setEditMode(isEdit: Boolean) {
        _isEditMode = isEdit
    }

    /**
     * Replaces current data with a restored draft.
     */
    fun restoreFromDraft(draft: RegistrationDataModel) {
        Log.i(TAG, "Restore: Data restored from local draft.")
        _registrationData = draft
    }

    /**
     * Logging helper to track the current state of the registration data.
     * Call this when moving between steps to verify data integrity.
     */
    fun logCurrentData() {
        Log.d(TAG, "Current Registration State: ${_registrationData.toMap()}")
    }
}

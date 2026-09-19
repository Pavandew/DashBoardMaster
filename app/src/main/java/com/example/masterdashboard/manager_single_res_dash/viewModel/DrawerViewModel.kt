package com.example.masterdashboard.manager_single_res_dash.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.models.DrawerMenuFactory
import com.example.masterdashboard.manager_single_res_dash.uistate.DrawerUiState
import com.example.masterdashboard.subscription.repo.SubscriptionRepository
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DrawerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "DrawerViewModel"
    }

    private val sessionManager = SessionManager(application.applicationContext)
    private val subRepo = SubscriptionRepository(application.applicationContext)
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(DrawerUiState())
    val uiState: StateFlow<DrawerUiState> = _uiState.asStateFlow()

    private var unreadListener: ListenerRegistration? = null

    init {
        Log.d(TAG, "DrawerViewModel initialized")
        loadInitialData()
    }

    fun loadInitialData(restaurantName: String? = null) {
        viewModelScope.launch {
            try {
                val fullName = sessionManager.getUserName() ?: "User"
                val rawRole = sessionManager.getRole()
                val displayRole = when (rawRole) {
                    AppConstants.ROLE_OWNER_SINGLE -> "Owner"
                    AppConstants.ROLE_MANAGER -> "Manager"
                    else -> rawRole.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                }

                val userSub = subRepo.getUserSubscriptionInfo()

                _uiState.value = _uiState.value.copy(
                    userName = fullName,
                    displayRole = displayRole,
                    restaurantName = restaurantName ?: _uiState.value.restaurantName,
                    trialDaysRemaining = userSub.trialDaysRemaining,
                    subscriptionStatus = userSub.status,
                    menuItems = DrawerMenuFactory.getMenuItems(_uiState.value.unreadNotificationsCount)
                )

                Log.i(
                    TAG,
                    "loadInitialData: Name='$fullName', Role='$displayRole', Status=${userSub.status}, DaysRemaining=${userSub.trialDaysRemaining}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading drawer data in DrawerViewModel", e)
            }
        }
        startUnreadCountListener()
    }

    fun updateRestaurantName(name: String) {
        _uiState.value = _uiState.value.copy(restaurantName = name)
    }

    private fun startUnreadCountListener() {
        val managerId = sessionManager.getUid()
        val userRole = sessionManager.getRole().lowercase().trim()
        val staffId = sessionManager.getStaffDocId()

        if (managerId.isEmpty()) {
            Log.w(TAG, "Skipping unread listener because managerId is empty")
            return
        }

        val isManager = userRole == "manager" || userRole == "owner_single" || userRole == "owner_multi"
        val roleTargets = mutableListOf("all", userRole)
        when (userRole) {
            "waiter", "waiter_staff" -> roleTargets.addAll(listOf("waiter", "waiter_staff"))
            "chef", "kitchen" -> roleTargets.addAll(listOf("chef", "kitchen"))
            "cashier", "billing" -> roleTargets.addAll(listOf("cashier", "billing"))
        }
        val targets = if (isManager) listOf("all", "manager") else roleTargets.distinct()

        unreadListener?.remove()

        val query = db.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_NOTIFICATIONS)
            .whereEqualTo(AppConstants.FIELD_IS_READ, false)
            .whereIn(AppConstants.FIELD_TARGET_ROLE, targets)

        unreadListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error in unread badge listener", error)
                return@addSnapshotListener
            }

            val unreadCount = snapshot?.mapNotNull { doc ->
                val tStaffId = doc.getString(AppConstants.FIELD_TARGET_STAFF_ID) ?: ""
                val isRelevant = when {
                    isManager -> true
                    tStaffId.isNotEmpty() -> tStaffId == staffId
                    else -> true
                }
                if (isRelevant) 1 else null
            }?.size ?: 0

            Log.d(TAG, "Unread count update: $unreadCount")
            _uiState.value = _uiState.value.copy(
                unreadNotificationsCount = unreadCount,
                menuItems = DrawerMenuFactory.getMenuItems(unreadCount)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "DrawerViewModel cleared, removing notification listener")
        unreadListener?.remove()
        unreadListener = null
    }
}
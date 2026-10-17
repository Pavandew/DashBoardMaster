package com.restroone.app.manager_single_res_dash.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.restroone.app.manager_single_res_dash.models.DrawerMenuFactory
import com.restroone.app.manager_single_res_dash.uistate.DrawerUiState
import com.restroone.app.subscription.repo.SubscriptionRepository
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.SessionManager
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

    private val unreadListeners = mutableListOf<ListenerRegistration>()

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
        val managerId = sessionManager.getRestaurantId()
        val userRole = sessionManager.getRole().lowercase().trim()
        val staffId = sessionManager.getStaffDocId()

        if (managerId.isEmpty()) {
            Log.w(TAG, "Skipping unread listener because managerId is empty")
            return
        }

        val isManager = userRole == "manager" || userRole == "owner_single" || userRole == "owner_multi"
        val roleTargets = when (userRole) {
            "waiter", "head waiter", "waiter_staff" ->
                listOf("all", "waiter", "head waiter", "waiter_staff")
            "chef", "master chef", "kitchen", "chef_staff" ->
                listOf("all", "chef", "master chef", "kitchen", "chef_staff")
            "cashier", "billing" -> listOf("all", "cashier", "billing")
            else -> listOf("all", userRole)
        }
        val targets = if (isManager) listOf("all", "manager") else roleTargets.distinct()

        unreadListeners.forEach { it.remove() }
        unreadListeners.clear()

        val query = db.collection(AppConstants.COLLECTION_RESTAURANTS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_NOTIFICATIONS)
            .whereEqualTo(AppConstants.FIELD_IS_READ, false)
            .whereIn(AppConstants.FIELD_TARGET_ROLE, targets)

        var broadcastUnreadCount = 0
        var targetedUnreadCount = 0
        fun listenForUnread(queryFilter: com.google.firebase.firestore.Query, targetIndex: Int) {
            unreadListeners += queryFilter.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error in unread badge listener", error)
                    return@addSnapshotListener
                }

                if (isManager) {
                    broadcastUnreadCount = snapshot?.documents?.size ?: 0
                } else if (targetIndex == 0) {
                    broadcastUnreadCount = snapshot?.documents?.size ?: 0
                } else {
                    targetedUnreadCount = snapshot?.documents?.size ?: 0
                }
                val unreadCount = if (isManager) {
                    broadcastUnreadCount
                } else {
                    broadcastUnreadCount + targetedUnreadCount
                }

                Log.d(TAG, "Unread count update: $unreadCount")
                _uiState.value = _uiState.value.copy(
                    unreadNotificationsCount = unreadCount,
                    menuItems = DrawerMenuFactory.getMenuItems(unreadCount)
                )
            }
        }
        if (isManager) {
            listenForUnread(query, 0)
        } else {
            listenForUnread(query.whereEqualTo(AppConstants.FIELD_TARGET_STAFF_ID, ""), 0)
            listenForUnread(
                query.whereIn(
                    AppConstants.FIELD_TARGET_ROLE,
                    targets.filter { it != "all" }
                ).whereEqualTo(AppConstants.FIELD_TARGET_STAFF_ID, staffId),
                1
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "DrawerViewModel cleared, removing notification listener")
        unreadListeners.forEach { it.remove() }
    }
}
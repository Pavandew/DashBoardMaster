package com.example.masterdashboard.staff_dash.kitchen_screens.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.masterdashboard.R
import com.example.masterdashboard.login.SplashActivity
import com.example.masterdashboard.login.utils.SessionManager
import com.example.masterdashboard.notifications.NotificationHelper
import com.example.masterdashboard.staff_dash.kitchen_screens.repo.KitchenOrderRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class KitchenNotificationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sessionManager: SessionManager
    private lateinit var notificationHelper: NotificationHelper
    private val repository = KitchenOrderRepository()
    
    private var lastOrderIds = mutableSetOf<String>()
    private var isFirstLoad = true
    private var listenerJob: Job? = null

    companion object {
        private const val TAG = "KitchenService"
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "kitchen_background_service"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Background service created.")
        sessionManager = SessionManager(this)
        notificationHelper = NotificationHelper(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val managerId = sessionManager.getUid()
        Log.i(TAG, "onStartCommand: Service triggered for Manager: $managerId")
        
        // 1. Show Foreground Notification (Required for Android 8+)
        val notification = createForegroundNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ requires explicit service type
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 2. Start the Firestore Listener (only if not already running)
        if (managerId.isNotEmpty()) {
            if (listenerJob == null || !listenerJob!!.isActive) {
                startListening(managerId)
            }
        } else {
            Log.w(TAG, "managerId is empty. Stopping service.")
            stopSelf()
        }

        return START_STICKY
    }

    private fun startListening(managerId: String) {
        Log.d(TAG, "startListening: Starting Firestore collectionGroup listener...")
        listenerJob = serviceScope.launch {
            repository.getRealtimeKitchenOrderDetailDatas(managerId).collectLatest { orders ->
                Log.d(TAG, "Received update with ${orders.size} orders.")
                
                if (isFirstLoad) {
                    Log.d(TAG, "First load: caching ${orders.size} existing order IDs.")
                    lastOrderIds.addAll(orders.map { it.orderId })
                    isFirstLoad = false
                    return@collectLatest
                }

                val currentOrderIds = orders.map { it.orderId }.toSet()
                val newOrderIds = currentOrderIds.filter { it !in lastOrderIds }

                if (newOrderIds.isNotEmpty()) {
                    // Check if any of the new orders are actually "New" or "Pending"
                    val actualNewOrders = orders.filter { it.orderId in newOrderIds && 
                            (it.status.equals("New", true) || it.status.equals("Pending", true)) }
                    
                    if (actualNewOrders.isNotEmpty()) {
                        Log.i(TAG, "🔔 NEW ORDER DETECTED in background! Showing alert.")
                        playNotificationSound()
                        
                        val order = actualNewOrders.first()
                        notificationHelper.showNotification(
                            "New Order: ${order.tableName}",
                            "Order #${order.orderId} received. ${order.items.size} items."
                        )
                    }
                }

                // Update the tracking set
                lastOrderIds.clear()
                lastOrderIds.addAll(currentOrderIds)
            }
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, SplashActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Using IMPORTANCE_DEFAULT to keep the service stable
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kitchen Hub Active")
            .setContentText("Monitoring live orders...")
            .setSmallIcon(R.drawable.ic_notifications_24dp)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kitchen Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Keeps the kitchen order listener running in background"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun playNotificationSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play notification sound", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.w(TAG, "Service onDestroy called. Cancelling scope.")
        super.onDestroy()
        serviceScope.cancel()
    }
}

package com.dev.point_of_sale_sistem_with_kotlin_pos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.NetworkMonitor
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.NetworkObserver
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.SupabaseClientProvider
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.createSupabaseClient
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "pos_notifications"
        private const val TAG = "MyApplication"
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var sessionPreferences: SessionPreferences
        private set

    override fun onCreate() {
        super.onCreate()

        // Supabase
        try {
            supabase = createSupabaseClient(applicationContext)
            SupabaseClientProvider.client = supabase
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Session Preferences
        try {
            sessionPreferences = SessionPreferences(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // FCM Token
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener

            val token = task.result

            applicationScope.launch {
                sessionPreferences.saveFcmToken(token)
            }
        }

        // Unified Notification Channel
        createNotificationChannel()

        // Network Observer for automatic sync
        setupNetworkObserver()
    }

    private fun setupNetworkObserver() {
        val networkMonitor = NetworkMonitor(this)

        NetworkObserver(
            context = this,
            owner = ProcessLifecycleOwner.get(),
            networkMonitor = networkMonitor
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Notificaciones POS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de ventas, inventario y alertas"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "Canal de notificaciones creado: $NOTIFICATION_CHANNEL_ID")
        }
    }
}
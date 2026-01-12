package com.dev.point_of_sale_sistem_with_kotlin_pos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
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

    // ✅ Instancia global de SessionPreferences
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "notification_fcm"
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var sessionPreferences: SessionPreferences
        private set

    override fun onCreate() {
        super.onCreate()

        // 🔹 Supabase
        try {
            supabase = createSupabaseClient(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 🔹 Session Preferences
        try {
            sessionPreferences = SessionPreferences(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener

            val token = task.result

            applicationScope.launch {
                sessionPreferences.saveFcmToken(token)
            }
        }


        createNotificationChannel()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Notificaciones de Incidencias",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificaciones de nuevas incidencias"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    }

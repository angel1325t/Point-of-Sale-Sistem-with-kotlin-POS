package com.dev.point_of_sale_sistem_with_kotlin_pos.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_SERVICE"
        private const val CHANNEL_ID = "stock_alerts"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📬 Mensaje recibido")
        Log.d(TAG, "  From: ${message.from}")
        Log.d(TAG, "  Data: ${message.data}")

        val title = message.data["title"] ?: "Inventario"
        val body = message.data["body"] ?: "Actualización disponible"

        Log.d(TAG, "  Title: $title")
        Log.d(TAG, "  Body: $body")

        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 Nuevo token FCM generado")
        Log.d(TAG, "  Token: ${token.take(20)}...")

        saveTokenToSupabase(token)
    }

    private fun saveTokenToSupabase(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id

                if (userId != null) {
                    Log.d(TAG, "💾 Guardando token en Supabase...")
                    Log.d(TAG, "  User ID: $userId")

                    supabase.from("fcm_tokens").upsert(
                        FCMTokenData(
                            userId = userId,
                            token = token,
                            platform = "android"
                        )
                    )

                    Log.d(TAG, "✅ Token guardado exitosamente")
                } else {
                    Log.w(TAG, "⚠️ No hay usuario autenticado")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error guardando token: ${e.message}", e)
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        Log.d(TAG, "🔔 Mostrando notificación")
        Log.d(TAG, "  Title: $title")
        Log.d(TAG, "  Body: $body")

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener este icono
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
            )
            .build()

        try {
            NotificationManagerCompat.from(this)
                .notify(System.currentTimeMillis().toInt(), notification)

            Log.d(TAG, "✅ Notificación mostrada exitosamente")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Error mostrando notificación (Permiso denegado)", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error mostrando notificación", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de Inventario",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de cambios en el inventario"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)

            Log.d(TAG, "📢 Canal de notificaciones creado")
        }
    }
}

@Serializable
data class FCMTokenData(
    @SerialName("user_id") val userId: String,
    val token: String,
    val platform: String,
    @SerialName("created_at") val createdAt: String? = null
)
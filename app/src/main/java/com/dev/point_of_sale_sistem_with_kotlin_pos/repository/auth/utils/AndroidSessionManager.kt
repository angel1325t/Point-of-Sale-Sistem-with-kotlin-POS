package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json
import androidx.core.content.edit // <-- asegúrate de tener core-ktx en dependencias

class AndroidSessionManager(context: Context) : SessionManager {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "supabase_session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val SESSION_KEY = "supabase_user_session"
    }

    override suspend fun loadSession(): UserSession? {
        val sessionJson = sharedPreferences.getString(SESSION_KEY, null)
        return sessionJson?.let {
            try {
                Json.decodeFromString<UserSession>(it)
            } catch (_: Exception) { // <-- usamos "_" porque no necesitamos la excepción
                null
            }
        }
    }

    // ✅ Firma corregida sin SessionSource
    override suspend fun saveSession(session: UserSession) {
        val sessionJson = Json.encodeToString(UserSession.serializer(), session)
        // Usando la extensión KTX 'edit'
        sharedPreferences.edit {
            putString(SESSION_KEY, sessionJson)
        }
    }

    override suspend fun deleteSession() {
        sharedPreferences.edit {
            remove(SESSION_KEY)
        }
    }
}

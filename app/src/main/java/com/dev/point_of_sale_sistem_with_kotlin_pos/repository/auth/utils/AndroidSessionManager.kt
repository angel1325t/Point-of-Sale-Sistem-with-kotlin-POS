package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json
import javax.crypto.AEADBadTagException

class AndroidSessionManager(context: Context) : SessionManager {

    companion object {
        private const val TAG = "AndroidSessionManager"
        private const val PREF_NAME = "supabase_session_prefs"
        private const val FALLBACK_PREF_NAME = "supabase_session_prefs_plain"
        private const val SESSION_KEY = "supabase_user_session"
    }

    // sharedPrefs apunta a encrypted prefs o fallback plano
    private val sharedPreferences: SharedPreferences =
        createEncryptedPrefsSafe(context) ?: createFallbackPrefs(context)

    /**
     * Crea EncryptedSharedPreferences de forma segura
     */
    private fun createEncryptedPrefsSafe(context: Context): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {

            Log.e(TAG, "Error creando EncryptedSharedPreferences", t)

            // Caso: prefs corruptas
            if (t is AEADBadTagException || (t.cause is AEADBadTagException)) {
                try {
                    Log.w(TAG, "Prefs corruptas detectadas. Eliminando...")

                    context.deleteSharedPreferences(PREF_NAME)

                    // Reintentar una vez
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    return EncryptedSharedPreferences.create(
                        context,
                        PREF_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )

                } catch (e: Throwable) {
                    Log.e(TAG, "Falló la recreación. Usando fallback.", e)
                }
            }

            null // forza fallback
        }
    }

    /**
     * Crea SharedPreferences normales si no se puede usar cifrado
     */
    private fun createFallbackPrefs(context: Context): SharedPreferences {
        Log.w(TAG, "Usando SharedPreferences sin cifrar (fallback temporal).")
        return context.getSharedPreferences(FALLBACK_PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Cargar sesión desde prefs
     */
    override suspend fun loadSession(): UserSession? {
        val json = sharedPreferences.getString(SESSION_KEY, null) ?: return null

        return try {
            Json.decodeFromString(UserSession.serializer(), json)
        } catch (e: Exception) {
            Log.e(TAG, "Error al decodificar sesión", e)
            null
        }
    }

    /**
     * Guardar sesión de Supabase
     */
    override suspend fun saveSession(session: UserSession) {
        val json = Json.encodeToString(UserSession.serializer(), session)
        sharedPreferences.edit {
            putString(SESSION_KEY, json)
        }
    }

    /**
     * Eliminar sesión
     */
    override suspend fun deleteSession() {
        sharedPreferences.edit {
            remove(SESSION_KEY)
        }
    }
}

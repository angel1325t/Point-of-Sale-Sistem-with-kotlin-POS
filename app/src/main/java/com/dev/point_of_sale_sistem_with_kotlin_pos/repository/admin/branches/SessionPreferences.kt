package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extensión DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_preferences")

class SessionPreferences(private val context: Context) {

    companion object {
        private const val TAG = "SessionPreferences"

        private val BRANCH_ID_KEY = stringPreferencesKey("branch_id")
        private val COMPANY_ID_KEY = stringPreferencesKey("company_id")

        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        private val USER_DISABLED_KEY = stringPreferencesKey("user_disabled")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    //FMC Token

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { it[FCM_TOKEN_KEY] = token }
    }

    suspend fun getFcmToken(): String? =
        context.dataStore.data.map { it[FCM_TOKEN_KEY] }.first()

    suspend fun clearFcmToken() {
        context.dataStore.edit { it.remove(FCM_TOKEN_KEY) }
    }

    // BranchId
    suspend fun saveBranchId(branchId: String) {
        context.dataStore.edit { preferences -> preferences[BRANCH_ID_KEY] = branchId }
    }

    suspend fun getBranchId(): String? =
        context.dataStore.data.map { it[BRANCH_ID_KEY] }.first()

    suspend fun clearBranchId() {
        context.dataStore.edit { it.remove(BRANCH_ID_KEY) }
    }

    suspend fun saveCompanyId(companyId: String) {
        context.dataStore.edit { preferences -> preferences[COMPANY_ID_KEY] = companyId }
    }

    suspend fun getCompanyId(): String? =
        context.dataStore.data.map { it[COMPANY_ID_KEY] }.first()

    suspend fun clearCompanyId() {
        context.dataStore.edit { it.remove(COMPANY_ID_KEY) }
    }

    suspend fun clearBiometric() {
        context.dataStore.edit { it.remove(BIOMETRIC_ENABLED_KEY) }
    }


    // Usuario deshabilitado
    suspend fun setUserDisabled(disabled: Boolean) {
        context.dataStore.edit { it[USER_DISABLED_KEY] = disabled.toString() }
    }

    // Control de autenticación biométrica
    suspend fun setBiometricEnabled(enabled: Boolean) {
        Log.d(TAG, "Guardando estado de biometría: enabled=$enabled")

        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }

        Log.d(TAG, "Estado biométrico guardado correctamente")
    }

    suspend fun isBiometricEnabled(): Boolean {
        val enabled = context.dataStore.data.map { it[BIOMETRIC_ENABLED_KEY] ?: false }.first()

        Log.d(TAG, "Obteniendo estado de biometría: enabled=$enabled")

        return enabled
    }
}

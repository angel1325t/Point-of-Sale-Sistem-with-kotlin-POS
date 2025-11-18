package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extensión DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_preferences")

class SessionPreferences(private val context: Context) {

    companion object {
        private val BRANCH_ID_KEY = stringPreferencesKey("branch_id")
    }

    /**
     * Guarda el ID de sucursal
     */
    suspend fun saveBranchId(branchId: String) {
        context.dataStore.edit { preferences ->
            preferences[BRANCH_ID_KEY] = branchId
        }
    }

    /**
     * Obtiene el ID guardado
     */
    suspend fun getBranchId(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[BRANCH_ID_KEY]
        }.first()
    }

    /**
     * Limpia solo el branch_id
     */
    suspend fun clearBranchId() {
        context.dataStore.edit { preferences ->
            preferences.remove(BRANCH_ID_KEY)
        }
    }

    /**
     * Limpia todo DataStore
     */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

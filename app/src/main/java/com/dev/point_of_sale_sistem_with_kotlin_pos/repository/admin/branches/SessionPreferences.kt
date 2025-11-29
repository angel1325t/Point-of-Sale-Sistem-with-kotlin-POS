package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extensión DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_preferences")

class SessionPreferences(private val context: Context) {

    companion object {
        private val BRANCH_ID_KEY = stringPreferencesKey("branch_id")
        private val USER_DISABLED_KEY = stringPreferencesKey("user_disabled")

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

    // Usuario deshabilitado
    suspend fun setUserDisabled(disabled: Boolean) {
        context.dataStore.edit { it[USER_DISABLED_KEY] = disabled.toString() }
    }
}
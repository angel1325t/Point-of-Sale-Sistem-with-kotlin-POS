package com.dev.point_of_sale_sistem_with_kotlin_pos.core.permissions

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Gestor centralizado de permisos de usuario.
 * Maneja la carga, almacenamiento y verificación de permisos.
 */
object PermissionManager {
    private const val TAG = "PERMISSION_MGR"

    private val _userPermissions = MutableStateFlow<Set<String>>(emptySet())
    val userPermissions: StateFlow<Set<String>> = _userPermissions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var roleRepository: RoleRepository? = null

    fun initialize() {
        roleRepository = RoleRepository(supabase)
        Log.d(TAG, "PermissionManager initialized")
    }

    /**
     * Carga los permisos de un usuario desde el repositorio
     */
    fun loadUserPermissions(
        userId: String,
        onComplete: (() -> Unit)? = null
    ) {
        if (userId.isBlank()) {
            Log.w(TAG, "Cannot load permissions: userId is blank")
            _userPermissions.value = emptySet()
            onComplete?.invoke()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Loading permissions for user: $userId")

                if (roleRepository == null) {
                    Log.e(TAG, "RoleRepository is null! Initializing now...")
                    initialize()
                }

                val permissions = roleRepository?.getUserPermissionKeys(userId) ?: emptySet()
                _userPermissions.value = permissions

                Log.d(TAG, "Successfully loaded ${permissions.size} permissions")
                if (permissions.isNotEmpty()) {
                    Log.d(TAG, "Permissions: $permissions")
                } else {
                    Log.w(TAG, "No permissions loaded - this might be an issue!")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading permissions", e)
                Log.e(TAG, "Exception message: ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                _userPermissions.value = emptySet()
            } finally {
                _isLoading.value = false
                onComplete?.invoke()
            }
        }
    }

    /**
     * Verifica si el usuario tiene un permiso específico
     */
    fun hasPermission(permissionKey: String): Boolean {
        return _userPermissions.value.contains(permissionKey)
    }

    /**
     * Verifica si el usuario tiene al menos uno de los permisos especificados
     */
    fun hasAnyPermission(vararg permissionKeys: String): Boolean {
        return permissionKeys.any { _userPermissions.value.contains(it) }
    }

    /**
     * Verifica si el usuario tiene todos los permisos especificados
     */
    fun hasAllPermissions(vararg permissionKeys: String): Boolean {
        return permissionKeys.all { _userPermissions.value.contains(it) }
    }

    /**
     * Limpia todos los permisos (útil para logout)
     */
    fun clearPermissions() {
        _userPermissions.value = emptySet()
        Log.d(TAG, "Permissions cleared")
    }

    /**
     * Recarga los permisos del usuario
     */
    fun refreshPermissions(
        userId: String,
        onComplete: (() -> Unit)? = null
    ) {
        loadUserPermissions(userId, onComplete)
    }
}

@Serializable
data class PermissionCheckResult(
    val hasPermission: Boolean,
    val permissionKey: String,
    val message: String? = null
)
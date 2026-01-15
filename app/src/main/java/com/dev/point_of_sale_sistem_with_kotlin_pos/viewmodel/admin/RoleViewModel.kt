package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.roles.RoleIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.roles.RoleError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.roles.RoleState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class RoleViewModel(
    private val repository: RoleRepository,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    var state by mutableStateOf(RoleState())
        private set

    fun handleIntent(intent: RoleIntent) {
        when (intent) {
            is RoleIntent.LoadRoles -> loadRoles()
            is RoleIntent.LoadRole -> loadRole(intent.id)
            is RoleIntent.CreateRole -> createRole(intent.role)
            is RoleIntent.UpdateRole -> updateRole(intent.role)
            is RoleIntent.DeleteRole -> deleteRole(intent.id)
            is RoleIntent.AssignPermissions -> assignPermissions(intent.roleId, intent.permissions)
            is RoleIntent.LoadPermissions -> loadPermissions()
        }
    }

    private fun loadRoles() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val roles = repository.getAllRoles()
                state = state.copy(isLoading = false, roles = roles)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.ConnectionError)
            }
        }
    }

    private fun loadRole(id: Int) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val role = repository.getRoleById(id)
                if (role != null) {
                    val assignedIds = repository.getAssignedPermissionIds(id).toSet()
                    state = state.copy(
                        isLoading = false,
                        selectedRole = role,
                        assignedPermissionIds = assignedIds
                    )
                } else {
                    state = state.copy(isLoading = false, error = RoleError.RoleNotFound)
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other(e.message))
            }
        }
    }

    private fun createRole(role: RoleRepository.RoleModel) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                repository.createRole(role)
                state = state.copy(isLoading = false, successMessage = "Rol creado correctamente")
                loadRoles()
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other(e.message))
            }
        }
    }

    private fun updateRole(role: RoleRepository.RoleModel) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                repository.updateRole(role)
                state = state.copy(isLoading = false, successMessage = "Rol actualizado correctamente")
                loadRoles()
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other(e.message))
            }
        }
    }

    private fun deleteRole(id: Int) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                repository.deleteRole(id)
                state = state.copy(isLoading = false, successMessage = "Rol eliminado correctamente")
                loadRoles()
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other(e.message))
            }
        }
    }

    private suspend fun removeFcmTokenIfExists() {
        val session = supabase.auth.currentSessionOrNull() ?: return
        val userId = session.user?.id ?: return

        supabase.from("user_tokens")
            .delete {
                filter {
                    eq("user_id", userId)
                }
            }
    }



    private fun loadPermissions() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val permissions = repository.getAllPermissions()
                state = state.copy(isLoading = false, permissions = permissions)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other("Error al cargar permisos: ${e.message}"))
            }
        }
    }

    private fun assignPermissions(roleId: Int, permissions: List<Int>) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            try {
                // 🔹 permisos ANTES
                val previousPermissions =
                    repository.getAssignedPermissionIds(roleId)

                repository.assignPermissionsToRole(roleId, permissions)

                // 🔹 permisos DESPUÉS
                val hadStockPermissions =
                    previousPermissions.contains(48) && previousPermissions.contains(49)

                val hasStockPermissions =
                    permissions.contains(48) && permissions.contains(49)

                when {
                    // 🟢 NO tenía → AHORA sí
                    !hadStockPermissions && hasStockPermissions -> {
                        sendFcmTokenIfExists()
                    }

                    // 🔴 TENÍA → AHORA no
                    hadStockPermissions && !hasStockPermissions -> {
                        removeFcmTokenIfExists()
                    }
                }

                state = state.copy(
                    isLoading = false,
                    successMessage = "Permisos asignados correctamente",
                    assignedPermissionIds = permissions.toSet()
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other(e.message))
            }
        }
    }

    private suspend fun sendFcmTokenIfExists() {
        val token = sessionPreferences.getFcmToken() ?: return
        val branchId = sessionPreferences.getBranchId() ?: return

        val session = supabase.auth.currentSessionOrNull() ?: return
        val userId = session.user?.id ?: return

        supabase.from("user_tokens")
            .upsert(
                mapOf(
                    "user_id" to userId,
                    "fcm_token" to token,
                    "branch_id" to branchId
                )
            )
    }






    fun clearMessages() {
        state = state.copy(error = null, successMessage = null)
    }
}
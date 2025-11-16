package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.roles.RoleIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.roles.RoleError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.roles.RoleState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository
import kotlinx.coroutines.launch

class RoleViewModel(
    private val repository: RoleRepository
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
                repository.assignPermissionsToRole(roleId, permissions)
                state = state.copy(
                    isLoading = false,
                    successMessage = "Permisos asignados correctamente",
                    assignedPermissionIds = permissions.toSet()
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other("Error al asignar permisos: ${e.message}"))
            }
        }
    }

    fun clearMessages() {
        state = state.copy(error = null, successMessage = null)
    }
}
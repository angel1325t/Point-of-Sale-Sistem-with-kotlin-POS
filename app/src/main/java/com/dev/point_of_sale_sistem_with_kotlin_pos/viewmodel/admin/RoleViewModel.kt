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
            is RoleIntent.CreateRole ->
            createRole(intent.name, intent.description)

            is RoleIntent.UpdateRole ->
                updateRole(intent.roleId, intent.name, intent.description)
            is RoleIntent.DeleteRole -> deleteRole(intent.id)
            is RoleIntent.AssignPermissions -> assignPermissions(intent.roleId, intent.permissions)
            is RoleIntent.LoadPermissions -> loadPermissions()
        }
    }

    /* =========================
       LOAD ROLES (FILTRADO)
    ========================== */

    private fun loadRoles() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val companyId = sessionPreferences.getCompanyId()
                    ?: throw IllegalStateException("Company ID not found")

                val roles = repository.getRolesByCompany(companyId)

                state = state.copy(
                    isLoading = false,
                    roles = roles
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = RoleError.Other(e.message)
                )
            }
        }
    }

    private fun loadRole(id: Int) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val role = repository.getRoleById(id)
                val assignedIds = repository.getAssignedPermissionIds(id).toSet()

                state = state.copy(
                    isLoading = false,
                    selectedRole = role,
                    assignedPermissionIds = assignedIds
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other(e.message))
            }
        }
    }

    private fun createRole(name: String, description: String?) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val companyId = sessionPreferences.getCompanyId()
                    ?: throw IllegalStateException("Company ID not found")

                val role = RoleRepository.RoleModel(
                    company_id = companyId,
                    name = name,
                    description = description
                )

                repository.createRole(role)
                loadRoles()

                state = state.copy(
                    isLoading = false,
                    successMessage = "Rol creado correctamente"
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = RoleError.Other(e.message)
                )
            }
        }
    }


    private fun updateRole(roleId: Int, name: String, description: String?) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val companyId = sessionPreferences.getCompanyId()
                    ?: throw IllegalStateException("Company ID not found")

                val role = RoleRepository.RoleModel(
                    role_id = roleId,
                    company_id = companyId,
                    name = name,
                    description = description
                )

                repository.updateRole(role)
                loadRoles()

                state = state.copy(
                    isLoading = false,
                    successMessage = "Rol actualizado correctamente"
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = RoleError.Other(e.message)
                )
            }
        }
    }


    private fun deleteRole(id: Int) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                repository.deleteRole(id)
                loadRoles()
                state = state.copy(isLoading = false, successMessage = "Rol eliminado correctamente")
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other(e.message))
            }
        }
    }

    /* =========================
       PERMISSIONS
    ========================== */

    private fun loadPermissions() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val permissions = repository.getAllPermissions()
                state = state.copy(isLoading = false, permissions = permissions)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other(e.message))
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
                    assignedPermissionIds = permissions.toSet(),
                    successMessage = "Permisos asignados correctamente"
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = RoleError.Other(e.message))
            }
        }
    }

    fun clearMessages() {
        state = state.copy(error = null, successMessage = null)
    }
}

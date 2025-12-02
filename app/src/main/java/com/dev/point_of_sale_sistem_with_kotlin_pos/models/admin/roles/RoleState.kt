package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.roles

import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository

data class RoleState(
    val roles: List<RoleRepository.RoleModel> = emptyList(),
    val selectedRole: RoleRepository.RoleModel? = null,
    val permissions: List<RoleRepository.PermissionModel> = emptyList(),
    val assignedPermissionIds: Set<Int> = emptySet(), // AÑADIDO
    val isLoading: Boolean = false,
    val error: RoleError? = null,
    val successMessage: String? = null
)
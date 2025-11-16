package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.roles

import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository

sealed class RoleIntent {
    data object LoadRoles : RoleIntent()
    data class LoadRole(val id: Int) : RoleIntent()
    data class CreateRole(val role: RoleRepository.RoleModel) : RoleIntent()
    data class UpdateRole(val role: RoleRepository.RoleModel) : RoleIntent()
    data class DeleteRole(val id: Int) : RoleIntent()
    data class AssignPermissions(val roleId: Int, val permissions: List<Int>) : RoleIntent()
    data object LoadPermissions : RoleIntent()
}
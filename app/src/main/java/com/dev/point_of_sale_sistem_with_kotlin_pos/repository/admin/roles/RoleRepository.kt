package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

class RoleRepository(private val supabase: SupabaseClient) {

    suspend fun getAllRoles(): List<RoleModel> =
        supabase.from("roles")
            .select(columns = Columns.ALL)
            .decodeList<RoleModel>()

    suspend fun getRoleById(roleId: Int): RoleModel? =
        supabase.from("roles")
            .select(columns = Columns.ALL) {
                filter {
                    RoleModel::role_id eq roleId
                }
            }
            .decodeSingleOrNull<RoleModel>()

    suspend fun createRole(role: RoleModel): RoleModel =
        supabase.from("roles")
            .insert(role) {
                select(Columns.ALL)
            }
            .decodeSingle<RoleModel>()

    suspend fun updateRole(role: RoleModel): RoleModel {
        require(role.role_id != null) { "role_id required for update" }
        return supabase.from("roles")
            .update(role) {
                filter {
                    RoleModel::role_id eq role.role_id!!
                }
                select(Columns.ALL)
            }
            .decodeSingle<RoleModel>()
    }

    suspend fun deleteRole(roleId: Int) {
        supabase.from("roles")
            .delete {
                filter {
                    RoleModel::role_id eq roleId
                }
            }
    }

    suspend fun getAllPermissions(): List<PermissionModel> =
        supabase.from("permissions")
            .select(columns = Columns.list("permission_id", "name"))
            .decodeList<PermissionModel>()

    // CORREGIDO: Obtiene los permission_id asignados al rol
    suspend fun getAssignedPermissionIds(roleId: Int): List<Int> =
        supabase.from("role_permission")
            .select(columns = Columns.list("permission_id")) {
                filter {
                    RolePermissionModel::role_id eq roleId
                }
            }
            .decodeList<RolePermissionIdOnly>()
            .map { it.permission_id }

    suspend fun assignPermissionsToRole(roleId: Int, permissionIds: List<Int>) {
        // Eliminar todos los permisos actuales
        supabase.from("role_permission")
            .delete {
                filter {
                    RolePermissionModel::role_id eq roleId
                }
            }

        // Insertar los nuevos
        if (permissionIds.isNotEmpty()) {
            val records = permissionIds.map { id ->
                RolePermissionModel(role_id = roleId, permission_id = id)
            }
            supabase.from("role_permission")
                .insert(records)
        }
    }

    // === MODELOS SERIALIZABLES ===

    @Serializable
    data class RoleModel(
        val role_id: Int? = null,
        val name: String,
        val description: String? = null,
        val created_at: String? = null,
        val updated_at: String? = null
    )

    @Serializable
    data class PermissionModel(
        val permission_id: Int,
        val name: String
    )

    @Serializable
    data class RolePermissionModel(
        val role_id: Int,
        val permission_id: Int
    )

    @Serializable
    private data class RolePermissionIdOnly(
        val permission_id: Int
    )
}
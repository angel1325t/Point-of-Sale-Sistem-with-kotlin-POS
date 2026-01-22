package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository.UserModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

class RoleRepository(private val supabase: SupabaseClient) {

    private val TAG = "RoleRepository"

    /* =========================
       ROLES
    ========================== */

    suspend fun getRolesByCompany(companyId: String): List<RoleModel> =
        supabase.from("roles")
            .select {
                filter {
                    eq("company_id", companyId)
                }
            }
            .decodeList()

    suspend fun getRoleById(roleId: Int): RoleModel? =
        supabase.from("roles")
            .select {
                filter {
                    RoleModel::role_id eq roleId
                }
            }
            .decodeSingleOrNull()

    suspend fun createRole(role: RoleModel): RoleModel =
        supabase.from("roles")
            .insert(role) {
                select(Columns.ALL)
            }
            .decodeSingle()

    suspend fun updateRole(role: RoleModel): RoleModel {
        require(role.role_id != null) { "role_id required for update" }

        return supabase.from("roles")
            .update(
                mapOf(
                    "name" to role.name,
                    "description" to role.description,
                    "updated_at" to "now()"
                )
            ) {
                filter {
                    RoleModel::role_id eq role.role_id
                }
                select(Columns.ALL)
            }
            .decodeSingle()
    }

    suspend fun deleteRole(roleId: Int) {
        supabase.from("roles")
            .delete {
                filter {
                    RoleModel::role_id eq roleId
                }
            }
    }

    /* =========================
       PERMISSIONS
    ========================== */

    suspend fun getAllPermissions(): List<PermissionModel> =
        supabase.from("permissions")
            .select(columns = Columns.list("permission_id", "name", "key"))
            .decodeList()

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
        supabase.from("role_permission")
            .delete {
                filter {
                    RolePermissionModel::role_id eq roleId
                }
            }

        if (permissionIds.isNotEmpty()) {
            val records = permissionIds.map {
                RolePermissionModel(role_id = roleId, permission_id = it)
            }
            supabase.from("role_permission").insert(records)
        }
    }

    /* =========================
       USER ROLES & PERMISSIONS
    ========================== */

    suspend fun getUserRoleId(userId: String): Int? =
        try {
            supabase.from("users")
                .select(columns = Columns.list("role_id")) {
                    filter {
                        UserModel::user_id eq userId
                    }
                }
                .decodeSingleOrNull<UserRoleIdOnly>()
                ?.role_id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user role_id", e)
            null
        }

    suspend fun getUserPermissionKeys(userId: String): Set<String> {
        val roleId = getUserRoleId(userId) ?: return emptySet()

        val permissionIds = supabase.from("role_permission")
            .select(columns = Columns.list("permission_id")) {
                filter {
                    RolePermissionModel::role_id eq roleId
                }
            }
            .decodeList<RolePermissionIdOnly>()
            .map { it.permission_id }

        if (permissionIds.isEmpty()) return emptySet()

        return supabase.from("permissions")
            .select(columns = Columns.list("key")) {
                filter {
                    PermissionModel::permission_id isIn permissionIds
                }
            }
            .decodeList<PermissionKeyWrapper>()
            .map { it.key }
            .toSet()
    }

    suspend fun getUserRoleName(userId: String): String? {
        val roleId = getUserRoleId(userId) ?: return null
        return getRoleById(roleId)?.name
    }

    /* =========================
       MODELS
    ========================== */

    @Serializable
    data class RoleModel(
        val role_id: Int? = null,
        val company_id: String,
        val name: String,
        val description: String? = null,
        val created_at: String? = null,
        val updated_at: String? = null
    )

    @Serializable
    data class PermissionModel(
        val permission_id: Int,
        val name: String,
        val key: String? = null
    )

    @Serializable
    data class RolePermissionModel(
        val role_id: Int,
        val permission_id: Int
    )

    @Serializable
    private data class RolePermissionIdOnly(val permission_id: Int)

    @Serializable
    private data class UserRoleIdOnly(val role_id: Int?)

    @Serializable
    private data class PermissionKeyWrapper(val key: String)
}

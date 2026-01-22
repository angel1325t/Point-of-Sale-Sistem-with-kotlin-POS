package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.SecureRandom
import java.util.UUID

class UserRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TAG = "UserRepository"
    }

    // 🔹 Usuario actual
    suspend fun getCurrentUser(): UserModel? {
        val authUser = supabase.auth.currentUserOrNull() ?: return null
        return supabase.postgrest.from("users")
            .select { filter { eq("auth_id", authUser.id) } }
            .decodeList<UserModel>()
            .firstOrNull()
    }

    // 🔹 SOLO usuarios activos del branch
    suspend fun getActiveUsersByBranch(
        branchId: UUID,
        authId: UUID
    ): List<UserModel> {

        return supabase.postgrest.from("users")
            .select {
                filter {
                    eq("active", true)
                    eq("branch_id", branchId.toString())
                    neq("auth_id", authId)
                }
            }
            .decodeList()
    }


    suspend fun getUserById(authId: String): UserModel? =
        supabase.postgrest.from("users")
            .select { filter { eq("auth_id", authId) } }
            .decodeList<UserModel>()
            .firstOrNull()

    suspend fun createUser(
        email: String,
        roleId: Int,
        companyId: UUID,
        branchId: UUID
    ): UserModel {

        val username = email.substringBefore("@")

        val exists = supabase.postgrest.from("users")
            .select { filter { eq("username", username) } }
            .decodeList<UserCheck>()
            .firstOrNull()

        if (exists != null) {
            throw IllegalArgumentException("username already exists")
        }

//        val password = generateRandomPassword()
        val password = "12345678"

        val authResponse = supabase.postgrest.rpc(
            "create_auth_user",
            buildJsonObject {
                put("email", email)
                put("password", password)
            }
        )

        val authId = UUID.fromString(authResponse.decodeAs<String>())

        supabase.postgrest.from("users").insert(
            UserInsert(
                auth_id = authId,
                role_id = roleId,
                active = true,
                company_id = companyId,
                branch_id = branchId,
                username = username
            )
        )

        return getUserById(authId.toString())
            ?: throw IllegalStateException("User created but not found")
    }

    suspend fun updateUser(
        authId: String,
        branchId: UUID?,
        roleId: Int?
    ): UserModel {

        val updateData = buildJsonObject {
            branchId?.let { put("branch_id", it.toString()) }
            roleId?.let { put("role_id", it) }
        }

        supabase.postgrest.from("users")
            .update(updateData) {
                filter { eq("auth_id", authId) }
            }

        return getUserById(authId)
            ?: throw IllegalStateException("Updated user not found")
    }

    suspend fun deleteUser(authId: String) {
        supabase.postgrest.from("users")
            .update(buildJsonObject { put("active", false) }) {
                filter { eq("auth_id", authId) }
            }
    }

    suspend fun getAllRoles(): List<RoleModel> =
        supabase.postgrest.from("roles")
            .select()
            .decodeList()

    suspend fun getBranchesByCompany(companyId: UUID): List<BranchModel> =
        supabase.postgrest.from("branches")
            .select { filter { eq("company_id", companyId.toString()) } }
            .decodeList()

    private fun generateRandomPassword(length: Int = 12): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    // 🔹 MODELOS

    @Serializable
    data class UserModel(
        @Contextual val user_id: UUID,
        @Contextual val auth_id: UUID,
        val username: String,
        val role_id: Int? = null,
        val active: Boolean = true,
        val created_at: String? = null,
        val updated_at: String? = null,
        val email: String? = null,
        @Contextual val company_id: UUID? = null,
        @Contextual val branch_id: UUID? = null
    )

    @Serializable
    data class UserInsert(
        @Contextual val auth_id: UUID,
        val role_id: Int,
        val active: Boolean,
        @Contextual val company_id: UUID,
        @Contextual val branch_id: UUID,
        val username: String
    )

    @Serializable
    data class RoleModel(
        val role_id: Int,
        val name: String
    )

    @Serializable
    data class BranchModel(
        @Contextual val branch_id: UUID,
        val name: String
    )

    @Serializable
    data class UserCheck(
        val username: String
    )
}

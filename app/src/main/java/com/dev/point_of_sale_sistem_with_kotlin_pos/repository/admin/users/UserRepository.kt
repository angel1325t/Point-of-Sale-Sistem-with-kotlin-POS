package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
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

    // Obtener el usuario logueado para sacar company_id y branch_id
    suspend fun getCurrentUser(): UserModel? {
        val authUser = supabase.auth.currentUserOrNull() ?: return null
        return supabase.postgrest.from("users")
            .select {
                filter {
                    eq("auth_id", authUser.id)
                }
            }
            .decodeList<UserModel>()
            .firstOrNull()
    }

    // Obtener solo usuarios activos
    suspend fun getAllActiveUsers(): List<UserModel> {
        Log.d(TAG, "Fetching all active users...")

        val users = supabase.postgrest.from("users")
            .select {
                filter {
                    eq("active", true)
                }
            }
            .decodeList<UserModel>()

        Log.d(TAG, "Found ${users.size} users")
        users.forEach { user ->
            Log.d(TAG, "User: ${user.username}, Active: ${user.active}")
        }

        return users
    }

    suspend fun getUserById(authId: String): UserModel? =
        supabase.postgrest.from("users")
            .select {
                filter {
                    eq("auth_id", authId)
                }
            }
            .decodeList<UserModel>()
            .firstOrNull()


    suspend fun createUser(
        email: String,
        roleId: Int,
        companyId: UUID,
        branchId: UUID
    ): UserModel {
        Log.d(TAG, "Starting user creation for $email")

        if (email.isBlank()) throw IllegalArgumentException("Email cannot be empty")

        // === username basado en email ===
        val username = email.substringBefore("@")

        // === Verificar si username ya existe ===
        val existingUser = supabase.postgrest.from("users")
            .select { filter { eq("username", username) } }
            .decodeList<UserCheck>()
            .firstOrNull()

        if (existingUser != null) {
            throw IllegalArgumentException("username '$username' is already in use")
        }

        // === Generar contraseña ===
        val randomPassword = generateRandomPassword()
        val passwordTest = "12345678"
        Log.d(TAG, "Generated random password for auth")

        // === Crear usuario en auth.users ===
        val authResponse = supabase.postgrest.rpc(
            "create_auth_user",
            buildJsonObject {
                put("email", email)
                put("password", passwordTest)
            }
        )

        val authId = try {
            val idString = authResponse.decodeAs<String>()
            UUID.fromString(idString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse auth user ID", e)
            throw Exception("Failed to create auth user: ${e.message}")
        }

        Log.d(TAG, "Auth user created: authId=$authId")


        // === Insert en public.users ===
        val userData = UserInsert(
            auth_id = authId,
            role_id = roleId,
            active = true,
            company_id = companyId,
            branch_id = branchId,
            username = username  // ← AQUI LO AGREGAS
        )

        Log.d(TAG, "Inserting user in public.users: $userData")
        val userResponse = supabase.postgrest.from("users").insert(userData)
        Log.d(TAG, "User insert response: $userResponse")

        return getUserById(authId.toString())
            ?: throw Exception("User created but could not retrieve")
    }


    suspend fun updateUser(
        authId: String,
        branchId: UUID?,
        roleId: Int?
    ): UserModel {
        Log.d(TAG, "Updating user: userId=$authId")

        val currentUser = getUserById(authId)
            ?: throw IllegalStateException("User not found")

        // 🔹 Crear el objeto con los datos a actualizar
        val updateData = buildJsonObject {
            if (branchId != null) put("branch_id", branchId.toString())
            if (roleId != null) put("role_id", roleId)
            put("updated_at", "now()")
        }

        // 🔹 Actualizar en public.users
        supabase.postgrest.from("users")
            .update(updateData) {
                filter {
                    eq("auth_id", authId)
                }
            }

        Log.d(TAG, "User updated successfully")

        // 🔹 Retornar el usuario actualizado
        return getUserById(authId)
            ?: throw IllegalStateException("Error reloading updated user")
    }


    // Soft delete: solo cambiar active a false
    suspend fun deleteUser(authId: String) {
        Log.d(TAG, "Soft deleting user: userId=$authId")

        val updateData = buildJsonObject {
            put("active", false)
            put("updated_at", "now()")
        }

        supabase.postgrest.from("users")
            .update(updateData) {
                filter {
                    eq("auth_id", authId)
                }
            }

        Log.d(TAG, "User soft deleted successfully")
    }

    suspend fun getAllRoles(): List<RoleModel> =
        supabase.postgrest.from("roles")
            .select {
                filter { /* no filter needed */ }
            }
            .decodeList<RoleModel>()

    suspend fun getAllBranches(): List<BranchModel> =
        supabase.postgrest.from("branches")
            .select { }
            .decodeList<BranchModel>()

    suspend fun getBranchesByCompany(companyId: UUID): List<BranchModel> =
        supabase.postgrest.from("branches")
            .select {
                filter {
                    eq("company_id", companyId)
                }
            }
            .decodeList<BranchModel>()


    // Generar contraseña aleatoria
    private fun generateRandomPassword(length: Int = 12): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    // === MODELOS SERIALIZABLES ===

    @Serializable
    data class UserModel(
        @Contextual val auth_id: UUID,
        val phone: String? = null,
        val username: String,
        val role_id: Int? = null,
        val active: Boolean? = false,
        val created_at: String? = null,
        val updated_at: String? = null,
        val email: String? = null,
        val profile_image_url: String? = null,
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

    @Serializable
    data class AuthUserResult(
        val id: String
    )
}
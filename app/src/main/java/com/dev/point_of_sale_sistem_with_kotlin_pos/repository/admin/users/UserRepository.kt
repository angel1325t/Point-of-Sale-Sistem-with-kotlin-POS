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
    suspend fun getAllActiveUsers(): List<UserModel> =
        supabase.postgrest.from("user_full_info")
            .select {
                filter {
                    eq("active", true)
                }
            }
            .decodeList<UserModel>()


    suspend fun getUserById(userId: String): UserModel? =
        supabase.postgrest.from("user_full_info")
            .select {
                filter {
                    eq("user_id", userId)
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

        // === 1. Validaciones ===
        if (email.isBlank()) throw IllegalArgumentException("Email cannot be empty")

        // Verificar si el username ya existe
        val existingUser = supabase.postgrest.from("users")
            .select { filter { eq("email", email) } }
            .decodeList<UserCheck>()
            .firstOrNull()
        if (existingUser != null) {
            throw IllegalArgumentException("email '$email' is already in use")
        }

        // === 2. Generar contraseña aleatoria ===
        val randomPassword = generateRandomPassword()
        Log.d(TAG, "Generated random password for user")

        // === 3. Crear usuario en auth.users usando RPC o REST API ===
        // Nota: Supabase Kotlin no expone signUpWith para admin, necesitamos usar el endpoint REST
        val authResponse = supabase.postgrest.rpc(
            "create_auth_user",
            buildJsonObject {
                put("email", email)
                put("password", randomPassword)
            }
        )

        val authId = try {
            val result = authResponse.decodeAs<AuthUserResult>()
            UUID.fromString(result.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse auth user ID", e)
            throw Exception("Failed to create auth user: ${e.message}")
        }

        Log.d(TAG, "Auth user created: authId=$authId")

        // === 4. Generar UUID para el usuario ===
        val userId = UUID.randomUUID()

        // === 5. Insertar usuario en public.users ===
        val userData = UserInsert(
            user_id = userId,
            auth_id = authId,
            role_id = roleId,
            active = true, // Por motivos de prueba
            company_id = companyId,
            branch_id = branchId
        )

        Log.d(TAG, "Inserting user: $userData")
        val userResponse: PostgrestResult = supabase.postgrest.from("users").insert(userData)
        Log.d(TAG, "User insert response: $userResponse")

        return getUserById(userId.toString())
            ?: throw Exception("User created but could not retrieve")
    }

    suspend fun updateUser(
        userId: String,
        email: String?,
        branchId: UUID?,
        roleId: Int?
    ): UserModel {
        Log.d(TAG, "Updating user: userId=$userId")

        val currentUser = getUserById(userId)
            ?: throw IllegalStateException("User not found")

        // 🔹 Si se actualiza el email, actualizar también en auth.users
        if (email != null && currentUser.auth_id != null) {
            try {
                supabase.postgrest.rpc(
                    "update_auth_user_email",
                    buildJsonObject {
                        put("user_auth_id", currentUser.auth_id.toString())
                        put("new_email", email)
                    }
                )
                Log.d(TAG, "Auth email updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update auth email", e)
                // Continuar aunque falle la actualización del email en auth
            }
        }

        // 🔹 Crear el objeto con los datos a actualizar
        val updateData = buildJsonObject {
            if (email != null) put("email", email)
            if (branchId != null) put("branch_id", branchId.toString())
            if (roleId != null) put("role_id", roleId)
            put("updated_at", "now()")
        }

        // 🔹 Actualizar en public.users
        supabase.postgrest.from("users")
            .update(updateData) {
                filter {
                    eq("user_id", userId)
                }
            }

        Log.d(TAG, "User updated successfully")

        // 🔹 Retornar el usuario actualizado
        return getUserById(userId)
            ?: throw IllegalStateException("Error reloading updated user")
    }


    // Soft delete: solo cambiar active a false
    suspend fun deleteUser(userId: String) {
        Log.d(TAG, "Soft deleting user: userId=$userId")

        val updateData = buildJsonObject {
            put("active", false)
            put("updated_at", "now()")
        }

        supabase.postgrest.from("users")
            .update(updateData) {
                filter {
                    eq("user_id", userId)
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
            .select {
                filter { /* no filter needed */ }
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
        @Contextual val user_id: UUID? = null,
        val phone: String? = null,
        val username: String,
        val role_id: Int? = null,
        val active: Boolean? = false,
        val created_at: String? = null,
        val updated_at: String? = null,
        val email: String? = null,
        @Contextual val auth_id: UUID? = null,
        val profile_image_url: String? = null,
        @Contextual val company_id: UUID? = null,
        @Contextual val branch_id: UUID? = null
    )

    @Serializable
    data class UserInsert(
        @Contextual val user_id: UUID,
        @Contextual val auth_id: UUID,
        val role_id: Int,
        val active: Boolean,
        @Contextual val company_id: UUID,
        @Contextual val branch_id: UUID
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
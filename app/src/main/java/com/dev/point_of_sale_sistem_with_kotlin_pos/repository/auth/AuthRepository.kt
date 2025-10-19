package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository(private val supabase: SupabaseClient) {

    // 🔹 Registro de usuario
    suspend fun register(email: String, password: String, username: String): Result<String> {
        return try {
            val result = supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("username", username)
                }
            }
            // Insertar en la tabla users
            supabase.from("users").insert(
                mapOf(
                    "auth_id" to result!!.id,
                    "email" to email,
                    "username" to username,
                    "created_at" to System.currentTimeMillis()
                )
            )
            Result.success(result.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🔹 Login
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val user = supabase.auth.currentUserOrNull()
            Result.success(user!!.id) // ✅ !! porque login exitoso = usuario existe
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🔹 Logout
    suspend fun logout(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🔹 Obtener usuario actual
    suspend fun getCurrentUser(): Result<String?> {
        return try {
            val user = supabase.auth.currentUserOrNull()
            Result.success(user?.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🔹 BONUS: Obtener sesión actual (para tu SessionManager)
    suspend fun getCurrentSession(): Result<Pair<String, String>?> {
        return try {
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                Result.success(session.accessToken to session.refreshToken)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
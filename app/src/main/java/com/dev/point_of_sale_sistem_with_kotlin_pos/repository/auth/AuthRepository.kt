package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

class AuthRepository(private val supabase: SupabaseClient) {

    // 🔹 Login
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val user = supabase.auth.currentUserOrNull()
            Result.success(user!!.id)
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
    fun getCurrentUser(): Result<String?> {
        return try {
            val user = supabase.auth.currentUserOrNull()
            Result.success(user?.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
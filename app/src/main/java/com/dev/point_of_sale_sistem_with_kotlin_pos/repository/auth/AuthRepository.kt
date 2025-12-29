package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.UserFullInfoDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import io.github.jan.supabase.postgrest.from

class AuthRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    suspend fun login(email: String, password: String): Result<String> = try {
        Log.d(TAG, "Attempting login with email=$email")
        if (email.isBlank()) throw IllegalArgumentException("Email cannot be empty")
        if (password.isBlank()) throw IllegalArgumentException("Password cannot be empty")

        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }

        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No user found after successful login")
        Log.d(TAG, "Login successful: userId=$userId")

        Result.success(userId)
    } catch (e: Exception) {
        Log.e(TAG, "Login failed", e)
        Result.failure(Exception("Login failed: ${e.message}", e))
    }

    suspend fun isUserDisabled(userID: UUID): Boolean {

        val user = supabase.postgrest.from("users")
            .select {
                filter { eq("auth_id", userID.toString()) }
            }
            .decodeSingleOrNull<UserRepository.UserModel>()

        if (user == null) {
            Log.d("UserCheck", "User not found for ID: $userID")
            return false
        }

        Log.d("UserCheck", "User active = ${user.active}")
        return !(user.active ?: true)
    }

    suspend fun getUserByAuthId(authId: UUID): UserFullInfoDTO {
        return supabase
            .from("user_full_info")
            .select {
                filter {
                    eq("auth_id", authId.toString())
                }
            }
            .decodeSingle<UserFullInfoDTO>()
    }







    suspend fun register(
        userEmail: String,
        userPassword: String,
        userName: String,
        businessName: String,
        businessEmail: String,
        businessPhone: String?,
        businessAddress: String?
    ): Result<Unit> = try {
        Log.d(TAG, "Starting registration for $userEmail")

        // === Validaciones básicas ===
        if (userEmail.isBlank()) throw IllegalArgumentException("User email cannot be empty")
        if (userPassword.isBlank()) throw IllegalArgumentException("Password cannot be empty")
        if (userName.isBlank()) throw IllegalArgumentException("Username cannot be empty")
        if (businessName.isBlank()) throw IllegalArgumentException("Business name cannot be empty")
        if (businessEmail.isBlank()) throw IllegalArgumentException("Business email cannot be empty")

        // === 1. Verificar conflictos de unicidad ===
        // Check for existing username
        val existingUser = supabase.postgrest.from("users")
            .select { filter { eq("username", userName) } }
            .decodeList<UserCheck>().firstOrNull()
        if (existingUser != null) {
            throw IllegalArgumentException("Username '$userName' is already in use")
        }

        // Check for existing company name
        val existingCompany = supabase.postgrest.from("companies")
            .select { filter { eq("name", businessName) } }
            .decodeList<CompanyCheck>().firstOrNull()
        if (existingCompany != null) {
            throw IllegalArgumentException("Company name '$businessName' is already in use")
        }

        // Check for existing company email
        val existingCompanyEmail = supabase.postgrest.from("companies")
            .select { filter { eq("email", businessEmail) } }
            .decodeList<CompanyCheck>().firstOrNull()
        if (existingCompanyEmail != null) {
            throw IllegalArgumentException("Business email '$businessEmail' is already in use")
        }

        // Check for existing branch name
        val branchName = "$businessName - Principal"
        val existingBranch = supabase.postgrest.from("branches")
            .select { filter { eq("name", branchName) } }
            .decodeList<BranchCheck>().firstOrNull()
        if (existingBranch != null) {
            throw IllegalArgumentException("Branch name '$branchName' is already in use")
        }

        // === 2. Crear usuario en Auth ===
        val authUser = supabase.auth.signUpWith(Email) {
            email = userEmail
            password = userPassword
            data = buildJsonObject { put("username", userName) }
        }
        val authId = authUser?.id?.let { UUID.fromString(it) }
            ?: throw Exception("Auth ID not found after signup")

        Log.d(TAG, "Auth user created: authId=$authId")

        // === 3. Generar UUIDs ===
        val companyId = UUID.randomUUID()
        val branchId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        // === 4. Insertar empresa ===
        val companyData = CompanyInsert(companyId, businessName, businessEmail, businessPhone, businessAddress)
        Log.d(TAG, "Inserting company: $companyData")
        val companyResponse: PostgrestResult = supabase.postgrest.from("companies").insert(companyData)
        Log.d(TAG, "Company insert response: $companyResponse")

        // === 5. Insertar sucursal ===
        val branchData = BranchInsert(branchId, companyId, branchName, businessPhone, businessAddress)
        Log.d(TAG, "Inserting branch: $branchData")
        val branchResponse: PostgrestResult = supabase.postgrest.from("branches").insert(branchData)
        Log.d(TAG, "Branch insert response: $branchResponse")

        // === 6. Insertar usuario ===
        val userData = UserInsert(
            username = userName,
            auth_id = authId,
            phone = businessPhone,
            role_id = 1,
            active = false,
            company_id = companyId,
            branch_id = branchId
        )

        Log.d(TAG, "Inserting user: $userData")
        val userResponse: PostgrestResult = supabase.postgrest.from("users").insert(userData)
        Log.d(TAG, "User insert response: $userResponse")

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Registration failed", e)
        Result.failure(e)
    }
    suspend fun logout(): Result<Unit> = try {
        supabase.auth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Logout failed: ${e.message}", e))
    }


    // === DTOs ===
    @Serializable
    data class CompanyInsert(
        @Contextual val company_id: UUID,
        val name: String,
        val email: String?,
        val phone: String?,
        val address: String?
    )

    @Serializable
    data class BranchInsert(
        @Contextual val branch_id: UUID,
        @Contextual val company_id: UUID,
        val name: String,
        val phone: String?,
        val address: String?
    )

    @Serializable
    data class UserInsert(
        val username: String,
        @Contextual val auth_id: UUID,
        val phone: String?,
        val role_id: Int,
        val active: Boolean,
        @Contextual val company_id: UUID,
        @Contextual val branch_id: UUID
    )

    // === Check DTOs ===
    @Serializable
    data class UserCheck(
        val username: String
    )

    @Serializable
    data class CompanyCheck(
        val name: String
    )

    @Serializable
    data class BranchCheck(
        val name: String
    )
}
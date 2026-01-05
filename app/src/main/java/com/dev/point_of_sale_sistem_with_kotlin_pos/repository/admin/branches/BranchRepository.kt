package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository.UserModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches.Branch
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.BusinessInfo
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.CompanyResponse

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

private const val TAG = "BranchRepository"

@Serializable
private data class BranchResponse(
    @SerialName("branch_id") val branchId: String,
    @SerialName("company_id") val companyId: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val active: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
private data class CompanyNameResponse(
    val name: String
)

class BranchRepository(private val supabase: SupabaseClient) {

    // ---------------------------------------------------------------------
    // 🔍 OBTENER COMPANY_ID DEL USUARIO AUTENTICADO
    // ---------------------------------------------------------------------
    suspend fun getCurrentUserCompanyId(): UUID? {
        return try {
            val authId = supabase.auth.currentUserOrNull()?.id
            Log.d(TAG, "Auth ID actual: $authId")

            if (authId == null) {
                Log.e(TAG, "No hay sesión activa")
                return null
            }

            val userData = supabase.from("users")
                .select {
                    filter { eq("auth_id", authId) }
                    limit(1)
                }
                .decodeSingleOrNull<UserModel>()

            Log.d(TAG, "Usuario obtenido de tabla users: $userData")

            return userData?.company_id

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo company_id: ${e.message}")
            null
        }
    }


    // ---------------------------------------------------------------------
    // 🔍 GET BRANCHES
    // ---------------------------------------------------------------------
    suspend fun getBranches(): Result<List<Branch>> {
        return try {
            val companyId = getCurrentUserCompanyId()
                ?: return Result.failure(Exception("User does not belong to any company"))

            Log.d(TAG, "Obteniendo sucursales de la empresa: $companyId")

            val branches = supabase.from("branches")
                .select {
                    filter { eq("company_id", companyId.toString()) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<BranchResponse>()

            Result.success(
                branches.map {
                    Branch(
                        branchId = it.branchId,
                        companyId = it.companyId,
                        name = it.name,
                        address = it.address,
                        phone = it.phone,
                        city = it.city,
                        active = it.active,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting branches", e)
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------------------
    // ➕ CREATE BRANCH
    // ---------------------------------------------------------------------
    suspend fun createBranch(alias: String, address: String, phone: String, city: String): Result<Branch> {
        return try {
            val companyId = getCurrentUserCompanyId()
                ?: return Result.failure(Exception("User does not belong to any company"))

            Log.d(TAG, "CompanyId para nueva sucursal: $companyId")

            val companyName = supabase.from("companies")
                .select(columns = Columns.list("name")) {
                    filter { eq("company_id", companyId.toString()) }
                }
                .decodeSingle<CompanyNameResponse>()
                .name

            val fullName = "$companyName - $alias"
            val newBranchId = UUID.randomUUID().toString()

            val newBranchJson = buildJsonArray {
                add(
                    buildJsonObject {
                        put("branch_id", newBranchId)
                        put("company_id", companyId.toString())
                        put("name", fullName)
                        put("address", address)
                        put("phone", phone)
                        put("city", city)
                        put("active", true)
                    }
                )
            }

            Log.d(TAG, "Insertando sucursal: $newBranchJson")

            supabase.from("branches").insert(newBranchJson)

            val created = supabase.from("branches")
                .select {
                    filter { eq("branch_id", newBranchId) }
                    limit(1)
                }
                .decodeSingle<BranchResponse>()

            return Result.success(
                Branch(
                    branchId = created.branchId,
                    companyId = created.companyId,
                    name = created.name,
                    address = created.address,
                    phone = created.phone,
                    city = created.city,
                    active = created.active,
                    createdAt = created.createdAt,
                    updatedAt = created.updatedAt
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error creating branch", e)
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------------------
    // ✏ UPDATE BRANCH
    // ---------------------------------------------------------------------
    suspend fun updateBranch(branchId: String, alias: String, address: String, phone: String, city: String): Result<Branch> {
        return try {
            val companyName = supabase.from("companies")
                .select(columns = Columns.list("name"))
                .decodeSingle<CompanyNameResponse>()
                .name

            val fullName = "$companyName - $alias"

            val updateData = buildJsonObject {
                put("name", fullName)
                put("address", address)
                put("phone", phone)
                put("city", city)
            }

            supabase.from("branches")
                .update(updateData) {
                    filter { eq("branch_id", branchId) }
                }

            val updated = supabase.from("branches")
                .select {
                    filter { eq("branch_id", branchId) }
                    limit(1)
                }
                .decodeSingle<BranchResponse>()

            Result.success(
                Branch(
                    branchId = updated.branchId,
                    companyId = updated.companyId,
                    name = updated.name,
                    address = updated.address,
                    phone = updated.phone,
                    city = updated.city,
                    active = updated.active,
                    createdAt = updated.createdAt,
                    updatedAt = updated.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating branch", e)
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------------------
    // ❌ DELETE BRANCH
    // ---------------------------------------------------------------------
    suspend fun deleteBranch(branchId: String): Result<Unit> {
        return try {
            supabase.from("branches")
                .delete {
                    filter { eq("branch_id", branchId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting branch", e)
            Result.failure(e)
        }
    }

    suspend fun getBusinessInfo(): BusinessInfo? {
        return try {
            val companyId = getCurrentUserCompanyId() ?: return null

            // --- COMPANY ---
            val company = supabase.from("companies")
                .select {
                    filter { eq("company_id", companyId.toString()) }
                    limit(1)
                }
                .decodeSingleOrNull<CompanyResponse>()
                ?: return null

            // --- BRANCH (opcional) ---
            val branch = supabase.from("branches")
                .select {
                    filter { eq("company_id", companyId.toString()) }
                    filter { eq("active", true) }
                    limit(1)
                }
                .decodeSingleOrNull<BranchResponse>()

            BusinessInfo(
                name = branch?.name ?: company.name,
                address = branch?.address ?: company.address,
                phone = branch?.phone ?: company.phone,
                city = branch?.city,
                email = company.email,
                taxId = company.taxId
            )
        } catch (e: Exception) {
            Log.e("BranchRepository", "Error getting BusinessInfo", e)
            null
        }
    }


    // ---------------------------------------------------------------------
    // 🔄 TOGGLE ACTIVE STATUS
    // ---------------------------------------------------------------------
    suspend fun toggleBranchStatus(branchId: String, active: Boolean): Result<Unit> {
        return try {
            val updateData = buildJsonObject { put("active", active) }

            supabase.from("branches")
                .update(updateData) {
                    filter { eq("branch_id", branchId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling status", e)
            Result.failure(e)
        }
    }
}

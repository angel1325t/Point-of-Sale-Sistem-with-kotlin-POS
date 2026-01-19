package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches

import android.content.Context
import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.utils.NetworkUtils
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineBranchEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches.Branch
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

private const val TAG = "HybridBranchRepository"



class HybridBranchRepository(
    private val context: Context,
    private val supabase: SupabaseClient,
    private val sessionPreferences: SessionPreferences
) {
    private val offlineDb = OfflineDatabase.getInstance(context)

    suspend fun getBranches(): Result<List<Branch>> {
        return if (NetworkUtils.isOnline(context)) {
            getBranchesOnline()
        } else {
            getBranchesOffline()
        }
    }

    suspend fun getCurrentUserCompanyId(): String? {
        val cachedCompanyId = sessionPreferences.getCompanyId()
        if (cachedCompanyId != null) {
            return cachedCompanyId
        }

        if (NetworkUtils.isOnline(context)) {
            return try {
                val authId = supabase.auth.currentUserOrNull()?.id
                if (authId == null) {
                    Log.e(TAG, "No hay sesión activa")
                    return null
                }

                val userData = supabase.from("users")
                    .select {
                        filter { eq("auth_id", authId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<UserRepository.UserModel>()

                val companyId = userData?.company_id?.toString()
                if (companyId != null) {
                    sessionPreferences.saveCompanyId(companyId)
                }
                companyId
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo company_id: ${e.message}")
                null
            }
        }
        return null
    }

    suspend fun cacheBranches() {
        if (!NetworkUtils.isOnline(context)) return

        try {
            val companyId = getCurrentUserCompanyId() ?: return
            val branches = supabase.from("branches")
                .select {
                    filter { eq("company_id", companyId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<BranchResponse>()

            val offlineBranches = branches.map { it.toOfflineEntity() }
            offlineDb.branchCacheDao().deleteByCompany(companyId)
            offlineDb.branchCacheDao().insertAll(offlineBranches)

            Log.d(TAG, "Cached ${branches.size} branches for offline use")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching branches: ${e.message}")
        }
    }

    private suspend fun getBranchesOnline(): Result<List<Branch>> {
        return try {
            val companyId = getCurrentUserCompanyId()
                ?: return Result.failure(Exception("User does not belong to any company"))

            Log.d(TAG, "Obteniendo sucursales online de la empresa: $companyId")

            val branches = supabase.from("branches")
                .select {
                    filter { eq("company_id", companyId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<BranchResponse>()

            val offlineBranches = branches.map { it.toOfflineEntity() }
            offlineDb.branchCacheDao().deleteByCompany(companyId)
            offlineDb.branchCacheDao().insertAll(offlineBranches)

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
            Log.e(TAG, "Error getting branches online", e)
            getBranchesOffline()
        }
    }

    private suspend fun getBranchesOffline(): Result<List<Branch>> {
        return try {
            val companyId = sessionPreferences.getCompanyId()
                ?: return Result.failure(Exception("No company cached. Please log in online first."))

            Log.d(TAG, "Obteniendo sucursales offline para company: $companyId")

            val branches = offlineDb.branchCacheDao().getBranchesByCompany(companyId)

            if (branches.isEmpty()) {
                Log.w(TAG, "No cached branches found for company: $companyId")
                return Result.failure(Exception("No branches cached. Please connect online to load branches."))
            }

            Log.d(TAG, "Found ${branches.size} cached branches")

            Result.success(
                branches.map { it.toBranch() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting branches offline", e)
            Result.failure(e)
        }
    }

    suspend fun createBranch(alias: String, address: String, phone: String, city: String): Result<Branch> {
        if (!NetworkUtils.isOnline(context)) {
            return Result.failure(Exception("Cannot create branch offline. Please connect online."))
        }

        return try {
            val companyId = getCurrentUserCompanyId()
                ?: return Result.failure(Exception("User does not belong to any company"))

            Log.d(TAG, "CompanyId para nueva sucursal: $companyId")

            val companyName = supabase.from("companies")
                .select(Columns.list("name")) {
                    filter { eq("company_id", companyId) }
                }
                .decodeSingle<CompanyNameResponse>()
                .name

            val fullName = "$companyName - $alias"
            val newBranchId = UUID.randomUUID().toString()

            val newBranchJson = buildJsonArray {
                add(
                    buildJsonObject {
                        put("branch_id", newBranchId)
                        put("company_id", companyId)
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

            val offlineEntity = created.toOfflineEntity()
            offlineDb.branchCacheDao().insert(offlineEntity)

            Result.success(
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

    suspend fun updateBranch(branchId: String, alias: String, address: String, phone: String, city: String): Result<Branch> {
        if (!NetworkUtils.isOnline(context)) {
            return Result.failure(Exception("Cannot update branch offline. Please connect online."))
        }

        return try {
            val companyName = supabase.from("companies")
                .select(Columns.list("name"))
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

            val offlineEntity = updated.toOfflineEntity()
            offlineDb.branchCacheDao().insert(offlineEntity)

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

    suspend fun deleteBranch(branchId: String): Result<Unit> {
        if (!NetworkUtils.isOnline(context)) {
            return Result.failure(Exception("Cannot delete branch offline. Please connect online."))
        }

        return try {
            supabase.from("branches")
                .delete {
                    filter { eq("branch_id", branchId) }
                }

            offlineDb.branchCacheDao().deleteByCompany(branchId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting branch", e)
            Result.failure(e)
        }
    }

    suspend fun toggleBranchStatus(branchId: String, active: Boolean): Result<Unit> {
        if (!NetworkUtils.isOnline(context)) {
            return Result.failure(Exception("Cannot update branch status offline. Please connect online."))
        }

        return try {
            val updateData = buildJsonObject { put("active", active) }

            supabase.from("branches")
                .update(updateData) {
                    filter { eq("branch_id", branchId) }
                }

            offlineDb.branchCacheDao().getById(branchId)?.let { existing ->
                val updated = existing.copy(active = active)
                offlineDb.branchCacheDao().insert(updated)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling status", e)
            Result.failure(e)
        }
    }

    @Serializable
    private data class CompanyNameResponse(
        val name: String
    )

    private fun BranchResponse.toOfflineEntity() = OfflineBranchEntity(
        branchId = branchId,
        companyId = companyId,
        name = name,
        address = address,
        phone = phone,
        city = city,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun OfflineBranchEntity.toBranch() = Branch(
        branchId = branchId,
        companyId = companyId,
        name = name,
        address = address,
        phone = phone,
        city = city,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.suppliers

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.Supplier
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.SupplierDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.SupplierError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.SupplierInsertDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.SupplierUpdateDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupplierRepository(
    private val supabase: SupabaseClient,
    private val sessionPreferences: SessionPreferences
) {

    companion object {
        private const val TABLE_NAME = "suppliers"
    }
    private suspend fun getBranchId(): String =
        sessionPreferences.getBranchId()
            ?: throw IllegalStateException("BRANCH_ID_NOT_FOUND")

    private suspend fun getCompanyId(): String =
        sessionPreferences.getCompanyId()
            ?: throw IllegalStateException("COMPANY_ID_NOT_FOUND")



    /**
     * Obtiene todos los proveedores (FILTRADO POR SUCURSAL)
     */
    suspend fun getAllSuppliers(): Result<List<Supplier>> = withContext(Dispatchers.IO) {
        try {
            val branchId = getBranchId()
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter { eq("branch_id", branchId) }
                }
                .decodeList<SupplierDTO>()

            Result.success(response.map { it.toSupplier() })

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Obtiene un proveedor por ID (y sucursal)
     */
    suspend fun getSupplierById(supplierId: Int): Result<Supplier> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()
                val response = supabase.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("supplier_id", supplierId)
                            eq("branch_id", branchId)
                        }
                    }
                    .decodeSingleOrNull<SupplierDTO>()

                response?.let {
                    Result.success(it.toSupplier())
                } ?: Result.failure(SupplierError.RecordNotFound())

            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    /**
     * Busca proveedores por nombre (FILTRADO POR SUCURSAL)
     */
    suspend fun searchSuppliers(query: String): Result<List<Supplier>> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()
                val response = supabase.from(TABLE_NAME)
                    .select {
                        filter {
                            ilike("name", "%$query%")
                            eq("branch_id", branchId)
                        }
                    }
                    .decodeList<SupplierDTO>()

                Result.success(response.map { it.toSupplier() })

            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    /**
     * Crear proveedor
     */
    suspend fun createSupplier(
        name: String,
        contact: String?,
        phone: String?,
        email: String?,
        address: String?
    ): Result<Supplier> = withContext(Dispatchers.IO) {
        try {
            if (name.isBlank()) {
                return@withContext Result.failure(
                    SupplierError.ValidationError("El nombre no puede estar vacío", "name")
                )
            }

            if (!phone.isNullOrBlank() && phone.length < 8) {
                return@withContext Result.failure(SupplierError.InvalidPhoneError())
            }

            if (!email.isNullOrBlank() && !email.contains("@")) {
                return@withContext Result.failure(SupplierError.InvalidEmailError())
            }
            val branchId = getBranchId()
            val companyId = getCompanyId()

            // 🔒 Validar duplicado POR SUCURSAL
            val duplicate = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("name", name.trim())
                        eq("branch_id", branchId)
                    }
                }
                .decodeSingleOrNull<SupplierDTO>()

            if (duplicate != null) {
                return@withContext Result.failure(SupplierError.DuplicateNameError())
            }

            val newSupplier = SupplierInsertDTO(
                name = name.trim(),
                contact = contact?.trim(),
                phone = phone?.trim(),
                email = email?.trim(),
                address = address?.trim(),
                branchId = branchId,
                companyId = companyId
            )

            val response = supabase.from(TABLE_NAME)
                .insert(newSupplier) { select() }
                .decodeSingle<SupplierDTO>()

            Result.success(response.toSupplier())

        } catch (e: Exception) {
            Log.e("CREATE_SUPPLIER", "Error", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Actualizar proveedor (NO toca branch/company)
     */
    suspend fun updateSupplier(
        supplierId: Int,
        name: String,
        contact: String?,
        phone: String?,
        email: String?,
        address: String?
    ): Result<Supplier> = withContext(Dispatchers.IO) {
        try {
            if (name.isBlank()) {
                return@withContext Result.failure(
                    SupplierError.ValidationError("El nombre no puede estar vacío", "name")
                )
            }

            val updatedData = SupplierUpdateDTO(
                name = name.trim(),
                contact = contact?.trim(),
                phone = phone?.trim(),
                email = email?.trim(),
                address = address?.trim()
            )
            val branchId = getBranchId()

            val response = supabase.from(TABLE_NAME)
                .update(updatedData) {
                    filter {
                        eq("supplier_id", supplierId)
                        eq("branch_id", branchId)
                    }
                    select()
                }
                .decodeSingleOrNull<SupplierDTO>()

            response?.let {
                Result.success(it.toSupplier())
            } ?: Result.failure(SupplierError.RecordNotFound())

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Eliminar proveedor (por sucursal)
     */
    suspend fun deleteSupplier(supplierId: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()
                supabase.from(TABLE_NAME)
                    .delete {
                        filter {
                            eq("supplier_id", supplierId)
                            eq("branch_id", branchId)
                        }
                    }

                Result.success(true)

            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    private fun handleException(e: Exception): SupplierError =
        SupplierError.UnknownError(cause = e)

    private fun SupplierDTO.toSupplier() = Supplier(
        supplierId = supplierId,
        name = name,
        contact = contact,
        phone = phone,
        email = email,
        address = address
    )
}

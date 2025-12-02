package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.suppliers

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupplierRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TABLE_NAME = "suppliers"
    }

    /**
     * Obtiene todos los proveedores
     */
    suspend fun getAllSuppliers(): Result<List<Supplier>> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.from(TABLE_NAME)
                .select()
                .decodeList<SupplierDTO>()

            Result.success(response.map { it.toSupplier() })

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Obtiene un proveedor por ID
     */
    suspend fun getSupplierById(supplierId: Int): Result<Supplier> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.from(TABLE_NAME)
                .select { filter { eq("supplier_id", supplierId) } }
                .decodeSingleOrNull<SupplierDTO>()

            response?.let {
                Result.success(it.toSupplier())
            } ?: Result.failure(SupplierError.RecordNotFound())

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Busca proveedores por nombre
     */
    suspend fun searchSuppliers(query: String): Result<List<Supplier>> =
        withContext(Dispatchers.IO) {
            try {
                val response = supabase.from(TABLE_NAME)
                    .select { filter { ilike("name", "%$query%") } }
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
            // Validaciones básicas
            if (name.isBlank()) {
                return@withContext Result.failure(
                    SupplierError.ValidationError(
                        message = "El nombre no puede estar vacío",
                        field = "name"
                    )
                )
            }

            if (!phone.isNullOrBlank() && phone.length < 8) {
                return@withContext Result.failure(SupplierError.InvalidPhoneError())
            }

            if (!email.isNullOrBlank() && !email.contains("@")) {
                return@withContext Result.failure(SupplierError.InvalidEmailError())
            }

            // Validar duplicado
            val duplicateName = supabase.from(TABLE_NAME)
                .select { filter { eq("name", name.trim()) } }
                .decodeSingleOrNull<SupplierDTO>()

            if (duplicateName != null) {
                return@withContext Result.failure(SupplierError.DuplicateNameError())
            }

            val newSupplier = SupplierInsertDTO(
                name = name.trim(),
                contact = contact?.trim(),
                phone = phone?.trim(),
                email = email?.trim(),
                address = address?.trim()
            )

            val response = supabase.from(TABLE_NAME)
                .insert(newSupplier) {
                    select()
                }
                .decodeSingle<SupplierDTO>()

            Result.success(response.toSupplier())

        } catch (e: Exception) {
            Log.e("CREATE_SUPPLIER", "Error: $e")
            Result.failure(handleException(e))
        }
    }

    /**
     * Actualiza un proveedor
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
            // Validaciones
            if (name.isBlank()) {
                return@withContext Result.failure(
                    SupplierError.ValidationError(
                        message = "El nombre no puede estar vacío",
                        field = "name"
                    )
                )
            }

            if (!phone.isNullOrBlank() && phone.length < 8) {
                return@withContext Result.failure(SupplierError.InvalidPhoneError())
            }

            if (!email.isNullOrBlank() && !email.contains("@")) {
                return@withContext Result.failure(SupplierError.InvalidEmailError())
            }

            // Validar duplicado, excluyendo el mismo proveedor
            val existingNames = supabase.from(TABLE_NAME)
                .select { filter { eq("name", name.trim()) } }
                .decodeList<SupplierDTO>()

            if (existingNames.any { it.supplierId != supplierId }) {
                return@withContext Result.failure(SupplierError.DuplicateNameError())
            }

            val updatedData = SupplierUpdateDTO(
                name = name.trim(),
                contact = contact?.trim(),
                phone = phone?.trim(),
                email = email?.trim(),
                address = address?.trim()
            )

            val response = supabase.from(TABLE_NAME)
                .update(updatedData) {
                    filter { eq("supplier_id", supplierId) }
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
     * Elimina un proveedor
     */
    suspend fun deleteSupplier(supplierId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val supplier = supabase.from(TABLE_NAME)
                .select { filter { eq("supplier_id", supplierId) } }
                .decodeSingleOrNull<SupplierDTO>()

            supplier ?: return@withContext Result.failure(SupplierError.RecordNotFound())

            supabase.from(TABLE_NAME)
                .delete { filter { eq("supplier_id", supplierId) } }

            Result.success(true)

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Elimina múltiples proveedores
     */
    suspend fun deleteMultipleSuppliers(supplierIds: List<Int>): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                var deletedCount = 0

                supplierIds.forEach { id ->
                    val result = deleteSupplier(id)
                    if (result.isSuccess) deletedCount++
                }

                Result.success(deletedCount)

            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    /**
     * Manejo centralizado de errores
     * Convierte excepciones genéricas en SupplierError específicos
     */
    private fun handleException(e: Exception): SupplierError {
        return when {
            e.message?.contains("network", ignoreCase = true) == true ->
                SupplierError.NetworkError(cause = e)

            e.message?.contains("timeout", ignoreCase = true) == true ->
                SupplierError.TimeoutError(cause = e)

            e.message?.contains("unauthorized", ignoreCase = true) == true ->
                SupplierError.UnauthorizedError(cause = e)

            e.message?.contains("not found", ignoreCase = true) == true ->
                SupplierError.RecordNotFound(cause = e)

            else -> SupplierError.UnknownError(cause = e)
        }
    }

    /**
     * Mapper: DTO → Modelo interno
     */
    private fun SupplierDTO.toSupplier() = Supplier(
        supplierId = supplierId,
        name = name,
        contact = contact,
        phone = phone,
        email = email,
        address = address
    )
}
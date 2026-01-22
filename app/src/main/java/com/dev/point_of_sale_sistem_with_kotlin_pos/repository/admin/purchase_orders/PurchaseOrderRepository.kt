package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.purchase_orders

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.OrderStatus
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.PurchaseOrder
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.PurchaseOrderDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.PurchaseOrderError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.PurchaseOrderInsertDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.PurchaseOrderUpdateDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class PurchaseOrderRepository(
    private val supabase: SupabaseClient,
    private val sessionPreferences: SessionPreferences
) {

    companion object {
        private const val TABLE_NAME = "purchase_orders"
        private const val PRODUCTS_TABLE = "products"
        private const val SUPPLIERS_TABLE = "suppliers"
        private const val TAG = "PurchaseOrderRepository"
    }

    private suspend fun getBranchId(): String =
        sessionPreferences.getBranchId()
            ?: throw IllegalStateException("BRANCH_ID_NOT_FOUND")

    // ───────────────────────────────────────────────
    // OBTENER PEDIDOS
    // ───────────────────────────────────────────────

    suspend fun getAllOrders(): Result<List<PurchaseOrder>> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()
                val ordersDTO = supabase.from(TABLE_NAME)
                    .select { filter { eq("branch_id", branchId) } }
                    .decodeList<PurchaseOrderDTO>()

                val orders = ordersDTO.map { dto ->
                    dto.toPurchaseOrder(
                        supplierName = getSupplierName(dto.supplierId),
                        productName = getProductName(dto.productId)
                    )
                }

                Result.success(orders)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading orders", e)
                Result.failure(handleException(e))
            }
        }

    suspend fun getOrderById(orderId: Int): Result<PurchaseOrder> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()
                val dto = supabase.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("order_id", orderId)
                            eq("branch_id", branchId)
                        }
                    }
                    .decodeSingleOrNull<PurchaseOrderDTO>()

                dto?.let {
                    Result.success(
                        it.toPurchaseOrder(
                            supplierName = getSupplierName(it.supplierId),
                            productName = getProductName(it.productId)
                        )
                    )
                } ?: Result.failure(PurchaseOrderError.RecordNotFound())
            } catch (e: Exception) {
                Log.e(TAG, "Error loading order by ID", e)
                Result.failure(handleException(e))
            }
        }

    suspend fun searchOrders(query: String): Result<List<PurchaseOrder>> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()
                val ordersDTO = supabase.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("branch_id", branchId)
                            like("product_name", "%$query%")
                        }
                    }
                    .decodeList<PurchaseOrderDTO>()

                val orders = ordersDTO.map { dto ->
                    dto.toPurchaseOrder(
                        supplierName = getSupplierName(dto.supplierId),
                        productName = getProductName(dto.productId)
                    )
                }

                Result.success(orders)
            } catch (e: Exception) {
                Log.e(TAG, "Error searching orders", e)
                Result.failure(handleException(e))
            }
        }

    suspend fun filterByStatus(status: OrderStatus): Result<List<PurchaseOrder>> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()
                val ordersDTO = supabase.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("branch_id", branchId)
                            eq("status", status.name)
                        }
                    }
                    .decodeList<PurchaseOrderDTO>()

                val orders = ordersDTO.map { dto ->
                    dto.toPurchaseOrder(
                        supplierName = getSupplierName(dto.supplierId),
                        productName = getProductName(dto.productId)
                    )
                }

                Result.success(orders)
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering orders by status", e)
                Result.failure(handleException(e))
            }
        }

    // ───────────────────────────────────────────────
    // CREAR, ACTUALIZAR, ELIMINAR
    // ───────────────────────────────────────────────

    suspend fun createOrder(
        supplierId: Int,
        productId: Int,
        quantity: Int,
        orderDate: String,
        notes: String?
    ): Result<PurchaseOrder> = withContext(Dispatchers.IO) {
        try {
            val branchId = getBranchId()
            val dto = PurchaseOrderInsertDTO(
                supplierId = supplierId,
                productId = productId,
                quantity = quantity,
                orderDate = orderDate,
                status = OrderStatus.PENDING.name,
                notes = notes
            )

            supabase.from(TABLE_NAME).insert(dto)
            val createdOrder = PurchaseOrder(
                orderId = 0, // Supabase generará ID al insertar
                branchId = branchId,
                supplierId = supplierId,
                supplierName = getSupplierName(supplierId),
                productId = productId,
                productName = getProductName(productId),
                quantity = quantity,
                orderDate = orderDate,
                status = OrderStatus.PENDING,
                notes = notes
            )

            Result.success(createdOrder)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating order", e)
            Result.failure(handleException(e))
        }
    }

    suspend fun markAsReceived(
        orderId: Int,
        receivedDate: String
    ): Result<PurchaseOrder> = withContext(Dispatchers.IO) {
        try {
            val branchId = getBranchId()
            val updateDTO = PurchaseOrderUpdateDTO(
                status = OrderStatus.RECEIVED.name,
                receivedDate = receivedDate
            )

            supabase.from(TABLE_NAME)
                .update(updateDTO) {
                    filter {
                        eq("order_id", orderId)
                        eq("branch_id", branchId)
                    }
                }

            // Obtener el pedido actualizado
            getOrderById(orderId)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking order as received", e)
            Result.failure(handleException(e))
        }
    }

    suspend fun updateNotes(
        orderId: Int,
        notes: String
    ): Result<PurchaseOrder> = withContext(Dispatchers.IO) {
        try {
            val branchId = getBranchId()
            val updateDTO = PurchaseOrderUpdateDTO(notes = notes)

            supabase.from(TABLE_NAME)
                .update(updateDTO) {
                    filter {
                        eq("order_id", orderId)
                        eq("branch_id", branchId)
                    }
                }

            getOrderById(orderId)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order notes", e)
            Result.failure(handleException(e))
        }
    }

    suspend fun deleteOrder(orderId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val branchId = getBranchId()

            supabase.from(TABLE_NAME)
                .delete {
                    filter {
                        eq("order_id", orderId)
                        eq("branch_id", branchId)
                    }
                }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting order", e)
            Result.failure(handleException(e))
        }
    }


    // ───────────────────────────────────────────────
    // HELPER METHODS
    // ───────────────────────────────────────────────

    private suspend fun getSupplierName(supplierId: Int): String {
        val supplier = supabase.from(SUPPLIERS_TABLE)
            .select { filter { eq("supplier_id", supplierId) } }
            .decodeSingleOrNull<JsonObject>()
        return supplier?.get("name")?.toString()?.removeSurrounding("\"") ?: "Desconocido"
    }

    private suspend fun getProductName(productId: Int): String {
        val product = supabase.from(PRODUCTS_TABLE)
            .select { filter { eq("product_id", productId) } }
            .decodeSingleOrNull<JsonObject>()
        return product?.get("name")?.toString()?.removeSurrounding("\"") ?: "Desconocido"
    }

    private fun handleException(e: Exception): PurchaseOrderError {
        return when {
            e.message?.contains("network", true) == true ->
                PurchaseOrderError.NetworkError(cause = e)
            e.message?.contains("timeout", true) == true ->
                PurchaseOrderError.TimeoutError(cause = e)
            e.message?.contains("unauthorized", true) == true ->
                PurchaseOrderError.UnauthorizedError(cause = e)
            e.message?.contains("not found", true) == true ->
                PurchaseOrderError.RecordNotFound(cause = e)
            else -> PurchaseOrderError.UnknownError(cause = e)
        }
    }

    private fun PurchaseOrderDTO.toPurchaseOrder(
        supplierName: String,
        productName: String
    ) = PurchaseOrder(
        orderId = orderId,
        branchId = branchId,
        supplierId = supplierId,
        supplierName = supplierName,
        productId = productId,
        productName = productName,
        quantity = quantity,
        orderDate = orderDate,
        status = OrderStatus.valueOf(status),
        notes = notes,
        receivedDate = receivedDate
    )
}

package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.purchase_orders

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductUpdateDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class PurchaseOrderRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TABLE_NAME = "purchase_orders"
        private const val PRODUCTS_TABLE = "products"
        private const val TAG = "PurchaseOrderRepository"
    }

    /**
     * Obtiene todos los pedidos con información de proveedor y producto
     */
    suspend fun getAllOrders(): Result<List<PurchaseOrder>> = withContext(Dispatchers.IO) {
        try {
            // Opción 1: Si tienes una vista en Supabase con JOIN
            // val response = supabase.from("purchase_orders_view")
            //     .select()
            //     .decodeList<PurchaseOrderWithDetailsDTO>()

            // Opción 2: Obtener pedidos y hacer queries adicionales
            val ordersDTO = supabase.from(TABLE_NAME)
                .select()
                .decodeList<PurchaseOrderDTO>()

            val orders = ordersDTO.map { dto ->
                // Obtener información del proveedor
                val supplier = supabase.from("suppliers")
                    .select { filter { eq("supplier_id", dto.supplierId) } }
                    .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

                // Obtener información del producto
                val product = supabase.from(PRODUCTS_TABLE)
                    .select { filter { eq("product_id", dto.productId) } }
                    .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

                dto.toPurchaseOrder(
                    supplierName = supplier?.get("name")?.toString()?.removeSurrounding("\"") ?: "Desconocido",
                    productName = product?.get("name")?.toString()?.removeSurrounding("\"") ?: "Desconocido"
                )
            }

            Result.success(orders)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading orders", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Obtiene un pedido por ID
     */
    suspend fun getOrderById(orderId: Int): Result<PurchaseOrder> = withContext(Dispatchers.IO) {
        try {
            val dto = supabase.from(TABLE_NAME)
                .select { filter { eq("order_id", orderId) } }
                .decodeSingleOrNull<PurchaseOrderDTO>()

            dto?.let {
                // Obtener detalles adicionales
                val supplier = supabase.from("suppliers")
                    .select { filter { eq("supplier_id", it.supplierId) } }
                    .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

                val product = supabase.from(PRODUCTS_TABLE)
                    .select { filter { eq("product_id", it.productId) } }
                    .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

                Result.success(
                    it.toPurchaseOrder(
                        supplierName = supplier?.get("name")?.toString()?.removeSurrounding("\"") ?: "Desconocido",
                        productName = product?.get("name")?.toString()?.removeSurrounding("\"") ?: "Desconocido"
                    )
                )
            } ?: Result.failure(PurchaseOrderError.RecordNotFound())

        } catch (e: Exception) {
            Log.e(TAG, "Error loading order by ID", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Crea un nuevo pedido
     */
    suspend fun createOrder(
        supplierId: Int,
        productId: Int,
        quantity: Int,
        orderDate: String,
        notes: String? = null
    ): Result<PurchaseOrder> = withContext(Dispatchers.IO) {
        try {
            // Validaciones
            if (quantity <= 0) {
                return@withContext Result.failure(PurchaseOrderError.InvalidQuantityError())
            }

            // Verificar que el proveedor existe
            val supplierExists = supabase.from("suppliers")
                .select { filter { eq("supplier_id", supplierId) } }
                .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>() != null

            if (!supplierExists) {
                return@withContext Result.failure(
                    PurchaseOrderError.ValidationError("El proveedor no existe", "supplier")
                )
            }

            // Verificar que el producto existe
            val productExists = supabase.from(PRODUCTS_TABLE)
                .select { filter { eq("product_id", productId) } }
                .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>() != null

            if (!productExists) {
                return@withContext Result.failure(
                    PurchaseOrderError.ValidationError("El producto no existe", "product")
                )
            }

            val newOrder = PurchaseOrderInsertDTO(
                supplierId = supplierId,
                productId = productId,
                quantity = quantity,
                orderDate = orderDate,
                status = "PENDING",
                notes = notes
            )

            val response = supabase.from(TABLE_NAME)
                .insert(newOrder) {
                    select()
                }
                .decodeSingle<PurchaseOrderDTO>()

            // Obtener detalles para retornar
            val order = getOrderById(response.orderId)
            order

        } catch (e: Exception) {
            Log.e(TAG, "Error creating order", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Marca un pedido como enviado
     */
    suspend fun markAsSent(orderId: Int): Result<PurchaseOrder> = withContext(Dispatchers.IO) {
        try {
            val updateDTO = PurchaseOrderUpdateDTO(status = "SENT")

            val response = supabase.from(TABLE_NAME)
                .update(updateDTO) {
                    filter { eq("order_id", orderId) }
                    select()
                }
                .decodeSingleOrNull<PurchaseOrderDTO>()

            response?.let {
                getOrderById(orderId)
            } ?: Result.failure(PurchaseOrderError.RecordNotFound())

        } catch (e: Exception) {
            Log.e(TAG, "Error marking as sent", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Marca un pedido como recibido y actualiza el stock del producto
     */
    suspend fun markAsReceived(
        orderId: Int,
        receivedDate: String
    ): Result<PurchaseOrder> = withContext(Dispatchers.IO) {
        try {
            // Obtener el pedido actual
            val orderResult = getOrderById(orderId)
            if (orderResult.isFailure) {
                return@withContext orderResult
            }

            val order = orderResult.getOrNull()!!

            // Verificar que no esté ya recibido
            if (order.isReceived) {
                return@withContext Result.failure(
                    PurchaseOrderError.CannotModifyReceivedOrder()
                )
            }

            // Actualizar el estado del pedido
            val updateOrderDTO = PurchaseOrderUpdateDTO(
                status = "RECEIVED",
                receivedDate = receivedDate
            )

            supabase.from(TABLE_NAME)
                .update(updateOrderDTO) {
                    filter { eq("order_id", orderId) }
                }

            // Actualizar el stock del producto
            val updateStockResult = updateProductStock(order.productId, order.quantity)
            if (updateStockResult.isFailure) {
                return@withContext Result.failure(
                    PurchaseOrderError.StockUpdateError()
                )
            }

            // Retornar el pedido actualizado
            getOrderById(orderId)

        } catch (e: Exception) {
            Log.e(TAG, "Error marking as received", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Actualiza el stock de un producto incrementándolo
     */
    private suspend fun updateProductStock(
        productId: Int,
        quantityToAdd: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Obtener el producto actual
            val product = supabase.from(PRODUCTS_TABLE)
                .select { filter { eq("product_id", productId) } }
                .decodeSingleOrNull<ProductDTO>()

            product ?: return@withContext Result.failure(
                PurchaseOrderError.ValidationError("Producto no encontrado")
            )

            // Calcular el nuevo stock
            val newStock = product.currentStock + quantityToAdd

            // Actualizar el producto
            val updateDTO = ProductUpdateDTO(
                name = product.name,
                description = product.description,
                price = product.price,
                barcode = product.barcode,
                categoryId = product.categoryId,
                image = product.image,
                currentStock = newStock,
                minimumStock = product.minimumStock
            )

            supabase.from(PRODUCTS_TABLE)
                .update(updateDTO) {
                    filter { eq("product_id", productId) }
                }

            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating product stock", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Actualiza las notas de un pedido
     */
    suspend fun updateNotes(
        orderId: Int,
        notes: String
    ): Result<PurchaseOrder> = withContext(Dispatchers.IO) {
        try {
            val updateDTO = PurchaseOrderUpdateDTO(notes = notes)

            supabase.from(TABLE_NAME)
                .update(updateDTO) {
                    filter { eq("order_id", orderId) }
                }

            getOrderById(orderId)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating notes", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Elimina un pedido
     */
    suspend fun deleteOrder(orderId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Verificar que el pedido no esté recibido
            val orderResult = getOrderById(orderId)
            if (orderResult.isSuccess) {
                val order = orderResult.getOrNull()!!
                if (order.isReceived) {
                    return@withContext Result.failure(
                        PurchaseOrderError.CannotModifyReceivedOrder(
                            "No se puede eliminar un pedido ya recibido"
                        )
                    )
                }
            }

            supabase.from(TABLE_NAME)
                .delete { filter { eq("order_id", orderId) } }

            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting order", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Busca pedidos por nombre de proveedor o producto
     */
    suspend fun searchOrders(query: String): Result<List<PurchaseOrder>> = withContext(Dispatchers.IO) {
        try {
            val allOrdersResult = getAllOrders()
            if (allOrdersResult.isFailure) {
                return@withContext allOrdersResult
            }

            val orders = allOrdersResult.getOrNull()!!
            val filtered = orders.filter {
                it.supplierName.contains(query, ignoreCase = true) ||
                        it.productName.contains(query, ignoreCase = true)
            }

            Result.success(filtered)

        } catch (e: Exception) {
            Log.e(TAG, "Error searching orders", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Filtra pedidos por estado
     */
    suspend fun filterByStatus(status: OrderStatus): Result<List<PurchaseOrder>> = withContext(Dispatchers.IO) {
        try {
            val allOrdersResult = getAllOrders()
            if (allOrdersResult.isFailure) {
                return@withContext allOrdersResult
            }

            val orders = allOrdersResult.getOrNull()!!
            val filtered = orders.filter { it.status == status }

            Result.success(filtered)

        } catch (e: Exception) {
            Log.e(TAG, "Error filtering by status", e)
            Result.failure(handleException(e))
        }
    }

    /**
     * Manejo centralizado de errores
     */
    private fun handleException(e: Exception): PurchaseOrderError {
        return when {
            e.message?.contains("network", ignoreCase = true) == true ->
                PurchaseOrderError.NetworkError(cause = e)

            e.message?.contains("timeout", ignoreCase = true) == true ->
                PurchaseOrderError.TimeoutError(cause = e)

            e.message?.contains("unauthorized", ignoreCase = true) == true ->
                PurchaseOrderError.UnauthorizedError(cause = e)

            e.message?.contains("not found", ignoreCase = true) == true ->
                PurchaseOrderError.RecordNotFound(cause = e)

            else -> PurchaseOrderError.UnknownError(cause = e)
        }
    }

    /**
     * Mapper: DTO → Modelo interno
     */
    private fun PurchaseOrderDTO.toPurchaseOrder(
        supplierName: String,
        productName: String
    ) = PurchaseOrder(
        orderId = orderId,
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
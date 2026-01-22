package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Estados posibles de un pedido - SIMPLIFICADO
 */
enum class OrderStatus {
    PENDING,    // Pendiente
    RECEIVED;   // Recibido

    fun toSpanish(): String = when (this) {
        PENDING -> "Pendiente"
        RECEIVED -> "Recibido"
    }
}

/**
 * Modelo de dominio para Pedidos de Reposición
 */
data class PurchaseOrder(
    val orderId: Int = 0,
    val branchId: String, // UUID como String
    val supplierId: Int,
    val supplierName: String = "",
    val productId: Int,
    val productName: String = "",
    val quantity: Int,
    val orderDate: String,
    val status: OrderStatus = OrderStatus.PENDING,
    val notes: String? = null,
    val receivedDate: String? = null
) {
    val isReceived: Boolean
        get() = status == OrderStatus.RECEIVED

    val isPending: Boolean
        get() = status == OrderStatus.PENDING
}


/**
 * DTO para leer desde Supabase
 */
@Serializable
data class PurchaseOrderDTO(
    @SerialName("order_id")
    val orderId: Int = 0,

    @SerialName("branch_id")
    val branchId: String, // UUID como String

    @SerialName("supplier_id")
    val supplierId: Int,

    @SerialName("product_id")
    val productId: Int,

    @SerialName("quantity")
    val quantity: Int,

    @SerialName("order_date")
    val orderDate: String,

    @SerialName("status")
    val status: String,

    @SerialName("notes")
    val notes: String? = null,

    @SerialName("received_date")
    val receivedDate: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)


/**
 * DTO extendido con información de proveedor y producto
 */
@Serializable
data class PurchaseOrderWithDetailsDTO(
    @SerialName("order_id")
    val orderId: Int = 0,

    @SerialName("supplier_id")
    val supplierId: Int,

    @SerialName("product_id")
    val productId: Int,

    @SerialName("quantity")
    val quantity: Int,

    @SerialName("order_date")
    val orderDate: String,

    @SerialName("status")
    val status: String,

    @SerialName("notes")
    val notes: String? = null,

    @SerialName("received_date")
    val receivedDate: String? = null,

    // Datos del proveedor (join)
    @SerialName("supplier_name")
    val supplierName: String? = null,

    // Datos del producto (join)
    @SerialName("product_name")
    val productName: String? = null
)

/**
 * DTO para insertar nuevos pedidos
 */
@Serializable
data class PurchaseOrderInsertDTO(
    @SerialName("supplier_id")
    val supplierId: Int,

    @SerialName("product_id")
    val productId: Int,

    @SerialName("quantity")
    val quantity: Int,

    @SerialName("order_date")
    val orderDate: String,

    @SerialName("status")
    val status: String = "PENDING",

    @SerialName("notes")
    val notes: String? = null
)

/**
 * DTO para actualizar pedidos
 */
@Serializable
data class PurchaseOrderUpdateDTO(
    @SerialName("status")
    val status: String? = null,

    @SerialName("notes")
    val notes: String? = null,

    @SerialName("received_date")
    val receivedDate: String? = null
)

/**
 * Estado del módulo de pedidos
 */
data class PurchaseOrderState(
    val orders: List<PurchaseOrder> = emptyList(),
    val filteredOrders: List<PurchaseOrder> = emptyList(),
    val selectedOrder: PurchaseOrder? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: PurchaseOrderError? = null,

    val searchQuery: String = "",
    val filterStatus: OrderStatus? = null,
    val filterSupplierId: Int? = null,

    val operationSuccess: Boolean = false,
    val successMessage: String? = null,

    // Validaciones
    val quantityError: String? = null,
    val supplierError: String? = null,
    val productError: String? = null,

    // Paginación
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val itemsPerPage: Int = 20,
    val totalItems: Int = 0
) {
    val hasOrders: Boolean
        get() = orders.isNotEmpty()

    val isFiltered: Boolean
        get() = searchQuery.isNotEmpty() ||
                filterStatus != null ||
                filterSupplierId != null

    val displayOrders: List<PurchaseOrder>
        get() = if (isFiltered) filteredOrders else orders

    val isProcessing: Boolean
        get() = isLoading || isRefreshing

    val pendingOrders: List<PurchaseOrder>
        get() = orders.filter { it.isPending }

    val receivedOrders: List<PurchaseOrder>
        get() = orders.filter { it.isReceived }
}
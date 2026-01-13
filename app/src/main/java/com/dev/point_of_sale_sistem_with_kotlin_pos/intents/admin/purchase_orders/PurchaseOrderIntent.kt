package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.purchase_orders

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.OrderStatus

sealed class PurchaseOrderIntent {

    // ───────────────────────────────
    // Carga y Consultas
    // ───────────────────────────────

    data object LoadOrders : PurchaseOrderIntent()

    data class LoadOrderById(val orderId: Int) : PurchaseOrderIntent()

    data class SearchOrders(val query: String) : PurchaseOrderIntent()

    data class FilterByStatus(val status: OrderStatus?) : PurchaseOrderIntent()

    data class FilterBySupplier(val supplierId: Int?) : PurchaseOrderIntent()

    data object LoadPendingOrders : PurchaseOrderIntent()

    // ───────────────────────────────
    // Creación
    // ───────────────────────────────

    data class CreateOrder(
        val supplierId: Int,
        val productId: Int,
        val quantity: Int,
        val orderDate: String,
        val notes: String? = null
    ) : PurchaseOrderIntent()

    // ───────────────────────────────
    // Actualización de Estado
    // ───────────────────────────────

    data class MarkAsSent(val orderId: Int) : PurchaseOrderIntent()

    data class MarkAsReceived(
        val orderId: Int,
        val receivedDate: String
    ) : PurchaseOrderIntent()

    data class UpdateNotes(
        val orderId: Int,
        val notes: String
    ) : PurchaseOrderIntent()

    // ───────────────────────────────
    // Eliminación
    // ───────────────────────────────

    data class DeleteOrder(val orderId: Int) : PurchaseOrderIntent()

    data class CancelOrder(val orderId: Int) : PurchaseOrderIntent()

    // ───────────────────────────────
    // UI y Validaciones
    // ───────────────────────────────

    data class SelectOrder(val orderId: Int?) : PurchaseOrderIntent()

    data object ClearSelection : PurchaseOrderIntent()

    data object ClearError : PurchaseOrderIntent()

    data object ResetState : PurchaseOrderIntent()

    data class ValidateQuantity(val quantity: Int) : PurchaseOrderIntent()

    data class ValidateSupplier(val supplierId: Int?) : PurchaseOrderIntent()

    data class ValidateProduct(val productId: Int?) : PurchaseOrderIntent()
}
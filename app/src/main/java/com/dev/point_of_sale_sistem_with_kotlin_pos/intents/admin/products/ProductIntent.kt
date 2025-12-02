package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.products

sealed class ProductsIntent {

    // ───────────────────────────────
    // ACCIONES DE LISTADO Y CONSULTA
    // ───────────────────────────────
    data object LoadProducts : ProductsIntent()
    data class LoadProductById(val productId: Int) : ProductsIntent()
    data class SearchProducts(val query: String) : ProductsIntent()
    data class FilterByCategory(val categoryId: Int?) : ProductsIntent()
    data object LoadLowStockProducts : ProductsIntent()


    // ───────────────────────────────
    // ACCIONES DE CREACIÓN
    // ───────────────────────────────
    data class CreateProduct(
        val name: String,
        val description: String?,
        val price: Double,
        val barcode: String?,
        val categoryId: Int,
        val image: String?,
        val currentStock: Int,
        val minimumStock: Int
    ) : ProductsIntent()


    // ───────────────────────────────
    // ACCIONES DE ACTUALIZACIÓN
    // ───────────────────────────────
    data class UpdateProduct(
        val productId: Int,
        val name: String,
        val description: String?,
        val price: Double,
        val barcode: String?,
        val categoryId: Int,
        val image: String?,
        val currentStock: Int,
        val minimumStock: Int
    ) : ProductsIntent()


    // ───────────────────────────────
    // ACCIONES DE ELIMINACIÓN
    // ───────────────────────────────
    data class DeleteProduct(val productId: Int) : ProductsIntent()
    data class DeleteMultipleProducts(val productIds: List<Int>) : ProductsIntent()


    // ───────────────────────────────
    // ACCIONES DE UI
    // ───────────────────────────────
    data class SelectProduct(val productId: Int?) : ProductsIntent()
    data object ClearSelection : ProductsIntent()
    data object ClearError : ProductsIntent()
    data object ResetState : ProductsIntent()


    // ───────────────────────────────
    // VALIDACIONES
    // ───────────────────────────────
    data class ValidateProductName(val name: String) : ProductsIntent()
    data class ValidatePrice(val price: Double) : ProductsIntent()
    data class ValidateStock(val stock: Int) : ProductsIntent()
    data class ValidateBarcode(val barcode: String?) : ProductsIntent()
}

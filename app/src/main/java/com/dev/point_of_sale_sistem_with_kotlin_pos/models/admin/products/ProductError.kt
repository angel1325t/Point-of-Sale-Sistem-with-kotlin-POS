package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products

sealed class ProductsError {

    // ═══════════════════════════════════════════════════
    // ERRORES DE RED/CONEXIÓN
    // ═══════════════════════════════════════════════════
    data object NetworkError : ProductsError()
    data object TimeoutError : ProductsError()
    data object NoInternetConnection : ProductsError()

    // ═══════════════════════════════════════════════════
    // ERRORES DE VALIDACIÓN
    // ═══════════════════════════════════════════════════
    data class ValidationError(val field: String, val message: String) : ProductsError()
    data object InvalidProductName : ProductsError()
    data object InvalidPrice : ProductsError()
    data object InvalidStock : ProductsError()
    data object InvalidCategory : ProductsError()
    data object InvalidBarcode : ProductsError()
    data object InvalidDiscount : ProductsError()
    data object ProductNameTooShort : ProductsError()
    data object ProductNameTooLong : ProductsError()
    data object PriceZeroOrNegative : ProductsError()
    data object StockNegative : ProductsError()
    data object DiscountValueInvalid : ProductsError()
    data object DiscountPercentageExceeded : ProductsError()

    // ═══════════════════════════════════════════════════
    // ERRORES DE BASE DE DATOS/API
    // ═══════════════════════════════════════════════════
    data object ProductNotFound : ProductsError()
    data class DuplicateBarcode(val barcode: String) : ProductsError()
    data object DatabaseError : ProductsError()
    data object UnauthorizedAccess : ProductsError()

    // ═══════════════════════════════════════════════════
    // ERRORES DE OPERACIONES CRUD
    // ═══════════════════════════════════════════════════
    data object CreateProductFailed : ProductsError()
    data object UpdateProductFailed : ProductsError()
    data object DeleteProductFailed : ProductsError()
    data object LoadProductsFailed : ProductsError()
    data object SearchProductsFailed : ProductsError()

    // ═══════════════════════════════════════════════════
    // ERRORES DE ARCHIVOS/STORAGE
    // ═══════════════════════════════════════════════════
    data object ImageUploadFailed : ProductsError()
    data object BarcodeGenerationFailed : ProductsError()
    data object StorageError : ProductsError()

    // ═══════════════════════════════════════════════════
    // ERRORES DE STOCK
    // ═══════════════════════════════════════════════════
    data object InsufficientStock : ProductsError()
    data object StockUpdateFailed : ProductsError()

    // ═══════════════════════════════════════════════════
    // ERROR GENÉRICO
    // ═══════════════════════════════════════════════════
    data class UnknownError(val message: String? = null) : ProductsError()
}

// ═══════════════════════════════════════════════════
// FUNCIÓN DE EXTENSIÓN PARA CONVERTIR EXCEPCIONES
// ═══════════════════════════════════════════════════
fun Throwable.toProductsError(): ProductsError {
    val errorMessage = message?.lowercase() ?: ""

    return when {
        // Errores de red
        errorMessage.contains("network") ||
                errorMessage.contains("socket") ||
                errorMessage.contains("connection") -> ProductsError.NetworkError

        errorMessage.contains("timeout") -> ProductsError.TimeoutError

        errorMessage.contains("internet") ||
                errorMessage.contains("offline") -> ProductsError.NoInternetConnection

        // Errores de base de datos
        errorMessage.contains("barcode") &&
                errorMessage.contains("duplicate") -> {
            val barcode = message?.substringAfter("barcode: ")?.substringBefore(" ") ?: ""
            ProductsError.DuplicateBarcode(barcode)
        }

        errorMessage.contains("not found") -> ProductsError.ProductNotFound

        errorMessage.contains("unauthorized") ||
                errorMessage.contains("permission") -> ProductsError.UnauthorizedAccess

        errorMessage.contains("database") ||
                errorMessage.contains("sql") -> ProductsError.DatabaseError

        // Errores de storage
        errorMessage.contains("upload") ||
                errorMessage.contains("storage") -> ProductsError.ImageUploadFailed

        // Errores de stock
        errorMessage.contains("insufficient stock") -> ProductsError.InsufficientStock

        // Error genérico
        else -> ProductsError.UnknownError(message)
    }
}
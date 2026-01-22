package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.suppliers

sealed class SupplierIntent {
    // Load operations
    object LoadSuppliers : SupplierIntent()
    data class LoadSupplierById(val supplierId: Int) : SupplierIntent()

    // CRUD operations
    data class CreateSupplier(
        val name: String,
        val contact: String?,
        val phone: String?,
        val email: String?,
        val address: String?
    ) : SupplierIntent()

    data class UpdateSupplier(
        val supplierId: Int,
        val name: String,
        val contact: String?,
        val phone: String?,
        val email: String?,
        val address: String?
    ) : SupplierIntent()

    data class DeleteSupplier(val supplierId: Int) : SupplierIntent()

    // Search and filter
    data class SearchSupplier(val query: String) : SupplierIntent()
    data class FilterSuppliers(val onlyCompleteContact: Boolean) : SupplierIntent()

    // Validation
    data class ValidateName(val name: String) : SupplierIntent()
    data class ValidatePhone(val phone: String) : SupplierIntent()
    data class ValidateEmail(val email: String) : SupplierIntent()

    // Clear operations
    object ClearError : SupplierIntent()
    object ClearSelectedSupplier : SupplierIntent()
}
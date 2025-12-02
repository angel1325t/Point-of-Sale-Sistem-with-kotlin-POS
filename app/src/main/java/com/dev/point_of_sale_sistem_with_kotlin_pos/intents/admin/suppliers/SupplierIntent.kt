package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.suppliers

sealed class SupplierIntent {
    object LoadSuppliers : SupplierIntent()
    data class LoadSupplierById(val supplierId: Int) : SupplierIntent()

    // Crear
    data class CreateSupplier(
        val name: String,
        val contact: String?,
        val phone: String?,
        val email: String?,
        val address: String?,
    ) : SupplierIntent()

    // Actualizar
    data class UpdateSupplier(
        val supplierId: Int,
        val name: String,
        val contact: String?,
        val phone: String?,
        val email: String?,
        val address: String?,
    ) : SupplierIntent()

    // Eliminar
    data class DeleteSupplier(val supplierId: Int) : SupplierIntent()

    // Buscar
    data class SearchSupplier(val query: String) : SupplierIntent()
}

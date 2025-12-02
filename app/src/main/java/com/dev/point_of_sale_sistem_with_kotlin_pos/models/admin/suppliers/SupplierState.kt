package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers

data class SupplierState(
    val suppliers: List<Supplier> = emptyList(),
    val filteredSuppliers: List<Supplier> = emptyList(),
    val selectedSupplier: Supplier? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val operationSuccess: Boolean = false,
    val successMessage: String? = null,

    val error: SupplierError? = null,

    // ───────────────────────────────
    // Validaciones
    // ───────────────────────────────
    val nameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,

    // ───────────────────────────────
    // Búsqueda y filtro
    // ───────────────────────────────
    val searchQuery: String = "",

    // ───────────────────────────────
    // Paginación
    // ───────────────────────────────
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val itemsPerPage: Int = 20,
    val totalItems: Int = 0
) {

    val hasSuppliers: Boolean
        get() = suppliers.isNotEmpty()

    val isFiltered: Boolean
        get() = searchQuery.isNotEmpty()

    val displaySuppliers: List<Supplier>
        get() = if (isFiltered) filteredSuppliers else suppliers

    val isProcessing: Boolean
        get() = isLoading || isRefreshing
}

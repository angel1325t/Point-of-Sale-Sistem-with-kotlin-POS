package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers

data class SupplierState(
    // Data
    val suppliers: List<Supplier> = emptyList(),
    val displaySuppliers: List<Supplier> = emptyList(),
    val selectedSupplier: Supplier? = null,

    // UI State
    val isLoading: Boolean = false,
    val error: SupplierError? = null,
    val operationSuccess: Boolean = false,
    val successMessage: String? = null,

    // Search and Filter
    val searchQuery: String = "",
    val isFiltered: Boolean = false,

    // Validation
    val nameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null
)


    package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products

    data class ProductState(
        val isLoading: Boolean = false,
        val products: List<ProductDTO> = emptyList(),
        val selectedProduct: ProductDTO? = null,
        val successMessage: String? = null,
        val error: ProductsError? = null
    )

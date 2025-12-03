package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo usado internamente por la app (usa String para discount_type como Supabase)
 */
@Serializable
data class Product(
    @SerialName("product_id")
    val productId: Int = 0,
    val name: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val barcode: String? = null,
    @SerialName("category_id")
    val categoryId: Int = 0,
    val categoryName: String? = null,
    val image: String? = null,
    @SerialName("current_stock")
    val currentStock: Int = 0,
    @SerialName("minimum_stock")
    val minimumStock: Int = 0,

    // Cambiado a String para coincidir con Supabase
    @SerialName("discount_type")
    val discountType: String = "none",

    @SerialName("discount_value")
    val discountValue: Double = 0.0,

    @SerialName("final_price")
    val finalPrice: Double? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * DTO que viene directo de Supabase
 */
@Serializable
data class ProductDTO(
    @SerialName("product_id") val productId: Int,
    val name: String,
    val description: String? = null,
    val price: Double,
    val barcode: String? = null,  // Supabase puede devolver null
    @SerialName("category_id") val categoryId: Int,
    val image: String? = null,
    @SerialName("current_stock") val currentStock: Int,
    @SerialName("minimum_stock") val minimumStock: Int,
    @SerialName("discount_type") val discountType: String = "none",
    @SerialName("discount_value") val discountValue: Double = 0.0,
    @SerialName("final_price") val finalPrice: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Para crear nuevos productos
 */
@Serializable
data class ProductInsertDTO(
    val name: String,
    val description: String? = null,
    val price: Double,
    @SerialName("category_id") val categoryId: Int,
    val image: String? = null,
    @SerialName("current_stock") val currentStock: Int = 0,
    @SerialName("minimum_stock") val minimumStock: Int = 0,
    @SerialName("discount_type") val discountType: String = "none",
    @SerialName("discount_value") val discountValue: Double = 0.0,
    val barcode: String? = null
)

/**
 * Para actualizar productos (parcial)
 */
@Serializable
data class ProductUpdateDTO(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val barcode: String? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    val image: String? = null,
    @SerialName("current_stock") val currentStock: Int? = null,
    @SerialName("minimum_stock") val minimumStock: Int? = null,
    @SerialName("discount_type") val discountType: String? = null,
    @SerialName("discount_value") val discountValue: Double? = null
)

/**
 * Extensiones de conversión CORRECTA y SIN ERRORES
 */
fun ProductDTO.toProduct(categoryName: String? = null): Product {
    return Product(
        productId = productId,
        name = name,
        description = description,
        price = price,
        barcode = barcode,
        categoryId = categoryId,
        categoryName = categoryName,
        image = image,
        currentStock = currentStock,
        minimumStock = minimumStock,
        discountType = discountType,           // String → String (perfecto)
        discountValue = discountValue,
        finalPrice = finalPrice,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Product.toUpdateDTO(): ProductUpdateDTO {
    return ProductUpdateDTO(
        name = name.takeIf { it.isNotBlank() },
        description = description?.takeIf { it.isNotBlank() },
        price = price.takeIf { it > 0 },
        barcode = barcode?.takeIf { it.isNotBlank() },
        categoryId = categoryId.takeIf { it > 0 },
        image = image,
        currentStock = currentStock.takeIf { it >= 0 },
        minimumStock = minimumStock.takeIf { it >= 0 },
        discountType = discountType.takeIf { it != "none" }, // solo envía si no es none
        discountValue = discountValue.takeIf { it > 0 }
    )
}

@Serializable
enum class DiscountType {
    @SerialName("none")
    NONE,

    @SerialName("percent")
    PERCENT,

    @SerialName("fixed")
    FIXED
}
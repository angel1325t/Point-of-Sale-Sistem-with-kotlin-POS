package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- SUMMARY ----------
@Serializable
data class SaleTotalDTO(
    val total: Double
)

// ---------- CASH REGISTER ----------
@Serializable
data class CashRegisterSaleDTO(
    @SerialName("cash_register_history_id")
    val cashRegisterHistoryId: String,
    val total: Double,
    @SerialName("cash_registers_history")
    val cashRegisterHistory: CashRegisterHistoryDTO
)

@Serializable
data class CashRegisterHistoryDTO(
    @SerialName("cash_register_id")
    val cashRegisterId: String,
    @SerialName("cash_registers")
    val cashRegister: CashRegisterNameDTO
)

@Serializable
data class CashRegisterNameDTO(
    val name: String
)

// ---------- CASHIER ----------
@Serializable
data class CashierSaleDTO(
    @SerialName("user_id")
    val userId: String,
    val total: Double,
    val users: UserDTO
)

@Serializable
data class UserDTO(
    val username: String
)

// ---------- PRODUCT ----------
@Serializable
data class ProductSaleDetailDTO(
    @SerialName("product_id")
    val productId: Int,
    val quantity: Int,
    @SerialName("final_price")
    val finalPrice: Double,
    val products: ProductWithCategoryDTO,
    val sales: SaleStatusDTO
)

@Serializable
data class ProductWithCategoryDTO(
    val name: String,
    @SerialName("category_id")
    val categoryId: Int,
    val categories: CategoryNameDTO
)

// ---------- CATEGORY ----------
@Serializable
data class CategorySaleDetailDTO(
    @SerialName("product_id")
    val productId: Int,
    val quantity: Int,
    @SerialName("final_price")
    val finalPrice: Double,
    val products: ProductCategoryDTO,
    val sales: SaleStatusDTO
)

@Serializable
data class ProductCategoryDTO(
    @SerialName("category_id")
    val categoryId: Int,
    val categories: CategoryNameDTO
)

// ---------- COMMON ----------
@Serializable
data class CategoryNameDTO(
    val name: String
)

@Serializable
data class SaleStatusDTO(
    @SerialName("sale_date")
    val saleDate: String,
    val status: String
)



package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports

import java.time.LocalDate

sealed class ReportType {
    object ByCashRegister : ReportType()
    object ByCashier : ReportType()
    object ByProduct : ReportType()
    object ByCategory : ReportType()
}

enum class DateFilter {
    DAILY,
    WEEKLY,
    MONTHLY
}

data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

// Reportes
data class CashRegisterSalesReport(
    val cashRegisterId: String,
    val cashRegisterName: String,
    val totalSales: Double,
    val salesCount: Int,
    val averageTicket: Double
)

data class CashierSalesReport(
    val userId: String,
    val username: String,
    val totalSales: Double,
    val salesCount: Int,
    val averageTicket: Double
)

data class ProductSalesReport(
    val productId: Int,
    val productName: String,
    val categoryName: String,
    val quantitySold: Int,
    val totalRevenue: Double,
    val averagePrice: Double
)

data class CategorySalesReport(
    val categoryId: Int,
    val categoryName: String,
    val totalRevenue: Double,
    val productsSold: Int,
    val salesCount: Int
)

data class ReportSummary(
    val totalRevenue: Double,
    val totalSales: Int,
    val averageTicket: Double,
    val period: String
)
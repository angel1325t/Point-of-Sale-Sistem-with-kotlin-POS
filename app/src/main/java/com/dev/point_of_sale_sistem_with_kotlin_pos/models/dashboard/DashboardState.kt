package com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard

data class DashboardState(
    val summary: DashboardSummary = DashboardSummary(),
    val topProducts: List<TopProduct> = emptyList(),
    val revenueData: List<RevenueDataPoint> = emptyList(),

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: DashboardError? = null,

    val selectedStartDate: String = "",
    val selectedEndDate: String = "",

    val lastUpdated: Long = 0L
) {
    val hasData: Boolean
        get() = topProducts.isNotEmpty() || revenueData.isNotEmpty()

    val isProcessing: Boolean
        get() = isLoading || isRefreshing
}

/**
 * Resumen general de las métricas del dashboard
 */
data class DashboardSummary(
    val totalRevenue: Double = 0.0,
    val totalSales: Int = 0,
    val averageTicket: Double = 0.0,
    val growthPercentage: Double = 0.0
)

/**
 * Producto más vendido
 */
data class TopProduct(
    val productId: Int,
    val productName: String,
    val quantitySold: Int,
    val revenue: Double,
    val percentage: Double
)

/**
 * Punto de datos para el gráfico de ingresos
 */
data class RevenueDataPoint(
    val date: String,
    val revenue: Double,
    val salesCount: Int
)
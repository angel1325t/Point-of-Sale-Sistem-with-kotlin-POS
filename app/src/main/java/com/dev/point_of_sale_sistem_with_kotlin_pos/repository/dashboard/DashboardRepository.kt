package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.dashboard

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DashboardRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TAG = "DASHBOARD_REPO"
        private const val SALES_TABLE = "sale"
        private const val SALE_ITEMS_TABLE = "sale_items"
    }

    /**
     * Obtiene el resumen completo del dashboard
     */
    suspend fun getDashboardData(
        startDate: String,
        endDate: String,
        branchId: Int? = null
    ): Result<DashboardData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 Obteniendo datos del dashboard - Rango: $startDate a $endDate")

            // Obtener ventas del período
            val sales = getSales(startDate, endDate, branchId)

            // Obtener productos más vendidos
            val topProducts = getTopProducts(startDate, endDate, branchId)

            // Obtener datos de ingresos por fecha
            val revenueData = getRevenueByDate(startDate, endDate, branchId)

            // Calcular márgenes (esto requiere información de costos)
            val profitMargins = calculateProfitMargins(sales)

            val dashboardData = DashboardData(
                sales = sales,
                topProducts = topProducts,
                revenueData = revenueData,
                profitMargins = profitMargins
            )

            Log.d(TAG, "Datos del dashboard obtenidos exitosamente")
            Result.success(dashboardData)

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo datos del dashboard: ${e.message}", e)
            Result.failure(DashboardError.UnknownError(exception = e))
        }
    }

    /**
     * Obtiene las ventas del período
     */
    private suspend fun getSales(
        startDate: String,
        endDate: String,
        branchId: Int?
    ): List<SaleDTO> {
        return try {
            val allSales = supabase
                .from(SALES_TABLE)
                .select()
                .decodeList<SaleDTO>()

            allSales.filter { sale ->
                val inDateRange =
                    sale.saleDate >= startDate &&
                            sale.saleDate <= endDate

                val sameBranch =
                    branchId == null || sale.branchId == branchId

                inDateRange && sameBranch
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo ventas: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene los productos más vendidos
     */
    private suspend fun getTopProducts(
        startDate: String,
        endDate: String,
        branchId: Int?,
        limit: Int = 5
    ): List<TopProductDTO> {
        return try {
            // Nota: Esto es una aproximación. En producción, deberías usar una vista
            // o una función de PostgreSQL que haga el JOIN y agregación correctamente

            val items = supabase.from(SALE_ITEMS_TABLE)
                .select(
                    columns = Columns.list(
                        "product_id",
                        "product_name",
                        "quantity",
                        "subtotal",
                        "sale_id"
                    )
                )
                .decodeList<SaleItemDTO>()

            // Agrupar y agregar en el cliente
            items.groupBy { it.productId }
                .map { (productId, items) ->
                    TopProductDTO(
                        productId = productId,
                        productName = items.first().productName,
                        totalQuantity = items.sumOf { it.quantity },
                        totalRevenue = items.sumOf { it.subtotal }
                    )
                }
                .sortedByDescending { it.totalRevenue }
                .take(limit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo top productos: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene los ingresos agrupados por fecha
     */
    private suspend fun getRevenueByDate(
        startDate: String,
        endDate: String,
        branchId: Int?
    ): List<RevenueDateDTO> {
        return try {
            val sales = getSales(startDate, endDate, branchId)

            // Agrupar por fecha
            sales.groupBy { it.saleDate.take(10) } // YYYY-MM-DD
                .map { (date, salesOfDay) ->
                    RevenueDateDTO(
                        saleDate = date,
                        totalRevenue = salesOfDay.sumOf { it.totalAmount },
                        salesCount = salesOfDay.size
                    )
                }
                .sortedBy { it.saleDate }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo ingresos por fecha: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Calcula los márgenes de ganancia
     * Nota: Esto requiere tener información de costos de productos
     * Por ahora retorna valores simulados
     */
    private fun calculateProfitMargins(sales: List<SaleDTO>): ProfitMargins {
        val totalRevenue = sales.sumOf { it.totalAmount }

        // TODO: Implementar cálculo real de costos cuando esté disponible
        // Por ahora asumimos un margen de 30%
        val estimatedCost = totalRevenue * 0.70
        val grossProfit = totalRevenue - estimatedCost
        val profitMargin = if (totalRevenue > 0) (grossProfit / totalRevenue) * 100 else 0.0

        return ProfitMargins(
            totalCost = estimatedCost,
            totalRevenue = totalRevenue,
            grossProfit = grossProfit,
            profitMargin = profitMargin
        )
    }
}

/**
 * Clase contenedora para todos los datos del dashboard
 */
data class DashboardData(
    val sales: List<SaleDTO>,
    val topProducts: List<TopProductDTO>,
    val revenueData: List<RevenueDateDTO>,
    val profitMargins: ProfitMargins
)
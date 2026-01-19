package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.dashboard

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardRepository(
    private val supabase: SupabaseClient
) {

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
        branchId: String? = null
    ): Result<DashboardData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 Obteniendo datos del dashboard - Rango: $startDate a $endDate, BranchId: $branchId")

            val sales = getSales(startDate, endDate, branchId)
            val topProducts = getTopProducts(startDate, endDate, branchId)
            val revenueData = getRevenueByDate(startDate, endDate, branchId)
            val profitMargins = calculateProfitMargins(sales)

            Result.success(
                DashboardData(
                    sales = sales,
                    topProducts = topProducts,
                    revenueData = revenueData,
                    profitMargins = profitMargins
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo datos del dashboard: ${e.message}", e)
            Result.failure(DashboardError.UnknownError(exception = e))
        }
    }

    /**
     * Obtiene las ventas del período
     */
    private suspend fun getSales(
        startDate: String,
        endDate: String,
        branchId: String?
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
                    branchId == null || sale.branchId.toString() == branchId

                inDateRange && sameBranch
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo ventas: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene los productos más vendidos
     */
    private suspend fun getTopProducts(
        startDate: String,
        endDate: String,
        branchId: String?,
        limit: Int = 5
    ): List<TopProductDTO> {
        return try {
            val items = supabase
                .from(SALE_ITEMS_TABLE)
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

            items
                .groupBy { it.productId }
                .map { (_, productItems) ->
                    TopProductDTO(
                        productId = productItems.first().productId,
                        productName = productItems.first().productName,
                        totalQuantity = productItems.sumOf { it.quantity },
                        totalRevenue = productItems.sumOf { it.subtotal }
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
        branchId: String?
    ): List<RevenueDateDTO> {
        return try {
            val sales = getSales(startDate, endDate, branchId)

            sales
                .groupBy { it.saleDate.take(10) } // YYYY-MM-DD
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
     * Nota: requiere costos reales de productos
     */
    private fun calculateProfitMargins(
        sales: List<SaleDTO>
    ): ProfitMargins {
        val totalRevenue = sales.sumOf { it.totalAmount }

        // Margen estimado del 30%
        val estimatedCost = totalRevenue * 0.70
        val grossProfit = totalRevenue - estimatedCost
        val profitMargin =
            if (totalRevenue > 0)
                (grossProfit / totalRevenue) * 100
            else 0.0

        return ProfitMargins(
            totalCost = estimatedCost,
            totalRevenue = totalRevenue,
            grossProfit = grossProfit,
            profitMargin = profitMargin
        )
    }
}

/**
 * Contenedor de datos del dashboard
 */
data class DashboardData(
    val sales: List<SaleDTO>,
    val topProducts: List<TopProductDTO>,
    val revenueData: List<RevenueDateDTO>,
    val profitMargins: ProfitMargins
)
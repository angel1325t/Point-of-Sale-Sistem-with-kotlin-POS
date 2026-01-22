package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.dashboard

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.DashboardError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.RevenueDateDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.SaleDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.SaleItemDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.TopProductDTO
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
        private const val SALES_TABLE = "sales"
        private const val SALE_ITEMS_TABLE = "sale_details"
    }

    suspend fun getDashboardData(
        startDate: String,
        endDate: String,
        branchId: String? = null
    ): Result<DashboardData> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 Obteniendo datos del dashboard - Rango: $startDate a $endDate, BranchId: $branchId")

            val sales = getSales(startDate, endDate, branchId)
            val topProducts = getTopProducts(startDate, endDate, sales, branchId)
            val revenueData = getRevenueByDate(sales)

            Result.success(
                DashboardData(
                    sales = sales,
                    topProducts = topProducts,
                    revenueData = revenueData
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo datos del dashboard: ${e.message}", e)
            Result.failure(DashboardError.UnknownError(exception = e))
        }
    }

    private suspend fun getSales(
        startDate: String,
        endDate: String,
        branchId: String?
    ): List<SaleDTO> {
        return try {
            val endDateTime = "$endDate 23:59:59"

            val allSales = supabase
                .from(SALES_TABLE)
                .select(
                    columns = Columns.list(
                        "sale_id",
                        "total",
                        "sale_date",
                        "payment_method",
                        "user_id",
                        "status",
                        "subtotal",
                        "itbis",
                        "invoice_number",
                        "branch_id"
                    )
                ) {
                    filter {
                        gte("sale_date", startDate)
                        lte("sale_date", endDateTime)
                        eq("status", "completed")
                        if (branchId != null) {
                            eq("branch_id", branchId)
                        }
                    }
                }
                .decodeList<SaleDTO>()

            Log.d(TAG, "✅ Ventas cargadas: ${allSales.size} registros")
            allSales

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo ventas: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun getTopProducts(
        startDate: String,
        endDate: String,
        sales: List<SaleDTO>,
        branchId: String? = null
    ): List<TopProductDTO> {
        return try {
            if (sales.isEmpty()) return emptyList()

            val endDateTime = "$endDate 23:59:59"

            val details = supabase
                .from(SALE_ITEMS_TABLE)
                .select(
                    columns = Columns.raw(
                        """
                        product_id,
                        quantity,
                        final_price,
                        products(name),
                        sales(branch_id, sale_date, status)
                        """.trimIndent()
                    )
                ) {
                    filter {
                        gte("sales.sale_date", startDate)
                        lte("sales.sale_date", endDateTime)
                        eq("sales.status", "completed")
                        if (branchId != null) {
                            eq("sales.branch_id", branchId)
                        }
                    }
                }
                .decodeList<SaleItemDTO>()

            details
                .groupBy { it.productId }
                .map { (_, productItems) ->
                    TopProductDTO(
                        productId = productItems.first().productId,
                        productName = productItems.first().productName,
                        totalQuantity = productItems.sumOf { it.quantity },
                        totalRevenue = productItems.sumOf { it.finalPrice }
                    )
                }
                .sortedByDescending { it.totalRevenue }
                .take(5)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo top productos: ${e.message}", e)
            emptyList()
        }
    }

    private fun getRevenueByDate(
        sales: List<SaleDTO>
    ): List<RevenueDateDTO> {
        return try {
            sales
                .groupBy { it.saleDate.take(10) }
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
}

data class DashboardData(
    val sales: List<SaleDTO>,
    val topProducts: List<TopProductDTO>,
    val revenueData: List<RevenueDateDTO>
)

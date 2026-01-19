package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.reports

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.CashRegisterSaleDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.CashRegisterSalesReport
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.CashierSaleDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.CashierSalesReport
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.CategorySaleDetailDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.CategorySalesReport
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.DateFilter
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.DateRange
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.ProductSaleDetailDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.ProductSalesReport
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.ReportSummary
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.SaleTotalDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SalesReportRepository(
    private val supabase: SupabaseClient
) {

    companion object {
        private const val TAG = "SalesReportRepository"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // -------------------- CASH REGISTER REPORT --------------------
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCashRegisterSalesReport(
        dateRange: DateRange
    ): Result<List<CashRegisterSalesReport>> = withContext(Dispatchers.IO) {
        try {
            val startDate = dateRange.startDate.format(dateFormatter)
            val endDate = dateRange.endDate.format(dateFormatter)

            val sales = supabase.postgrest
                .from("sales")
                .select(
                    columns = Columns.raw("""
                        cash_register_history_id,
                        total,
                        cash_registers_history!inner(
                            cash_register_id,
                            cash_registers(name)
                        )
                    """.trimIndent())
                ) {
                    filter {
                        gte("sale_date", startDate)
                        lte("sale_date", "$endDate 23:59:59")
                        eq("status", "completed")
                    }
                }
                .decodeList<CashRegisterSaleDTO>()

            val report = sales
                .groupBy {
                    it.cashRegisterHistory.cashRegisterId to it.cashRegisterHistory.cashRegister.name
                }
                .map { (idAndName, items) ->
                    val totalSales = items.sumOf { it.total }
                    val salesCount = items.size
                    val averageTicket = if (salesCount > 0) totalSales / salesCount else 0.0

                    CashRegisterSalesReport(
                        cashRegisterId = idAndName.first,
                        cashRegisterName = idAndName.second,
                        totalSales = totalSales,
                        salesCount = salesCount,
                        averageTicket = averageTicket
                    )
                }

            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCashRegisterSalesReport", e)
            Result.failure(e)
        }
    }

    // -------------------- CASHIER REPORT --------------------
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCashierSalesReport(
        dateRange: DateRange
    ): Result<List<CashierSalesReport>> = withContext(Dispatchers.IO) {
        try {
            val startDate = dateRange.startDate.format(dateFormatter)
            val endDate = dateRange.endDate.format(dateFormatter)

            val sales = supabase.postgrest
                .from("sales")
                .select(
                    columns = Columns.raw("""
                        user_id,
                        total,
                        users!inner(username)
                    """.trimIndent())
                ) {
                    filter {
                        gte("sale_date", startDate)
                        lte("sale_date", "$endDate 23:59:59")
                        eq("status", "completed")
                    }
                }
                .decodeList<CashierSaleDTO>()

            val report = sales
                .groupBy { it.userId to it.users.username }
                .map { (idAndUsername, items) ->
                    val totalSales = items.sumOf { it.total }
                    val salesCount = items.size
                    val averageTicket = if (salesCount > 0) totalSales / salesCount else 0.0

                    CashierSalesReport(
                        userId = idAndUsername.first,
                        username = idAndUsername.second,
                        totalSales = totalSales,
                        salesCount = salesCount,
                        averageTicket = averageTicket
                    )
                }

            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCashierSalesReport", e)
            Result.failure(e)
        }
    }

    // -------------------- PRODUCT REPORT --------------------
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getProductSalesReport(
        dateRange: DateRange
    ): Result<List<ProductSalesReport>> = withContext(Dispatchers.IO) {
        try {
            val startDate = dateRange.startDate.format(dateFormatter)
            val endDate = dateRange.endDate.format(dateFormatter)

            val details = supabase.postgrest
                .from("sale_details")
                .select(
                    columns = Columns.raw("""
                        product_id,
                        quantity,
                        final_price,
                        products!inner(name, category_id, categories(name)),
                        sales!inner(sale_date, status)
                    """.trimIndent())
                ) {
                    filter {
                        gte("sales.sale_date", startDate)
                        lte("sales.sale_date", "$endDate 23:59:59")
                        eq("sales.status", "completed")
                    }
                }
                .decodeList<ProductSaleDetailDTO>()

            val report = details
                .groupBy { it.productId }
                .map { (productId, items) ->
                    val firstItem = items.first()
                    val quantitySold = items.sumOf { it.quantity }
                    val totalRevenue = items.sumOf { it.finalPrice }
                    val averagePrice = if (quantitySold > 0) totalRevenue / quantitySold else 0.0

                    ProductSalesReport(
                        productId = productId,
                        productName = firstItem.products.name,
                        categoryName = firstItem.products.categories.name,
                        quantitySold = quantitySold,
                        totalRevenue = totalRevenue,
                        averagePrice = averagePrice
                    )
                }

            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProductSalesReport", e)
            Result.failure(e)
        }
    }

    // -------------------- CATEGORY REPORT --------------------
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCategorySalesReport(
        dateRange: DateRange
    ): Result<List<CategorySalesReport>> = withContext(Dispatchers.IO) {
        try {
            val startDate = dateRange.startDate.format(dateFormatter)
            val endDate = dateRange.endDate.format(dateFormatter)

            val details = supabase.postgrest
                .from("sale_details")
                .select(
                    columns = Columns.raw("""
                        product_id,
                        quantity,
                        final_price,
                        products!inner(category_id, categories(name)),
                        sales!inner(sale_date, status)
                    """.trimIndent())
                ) {
                    filter {
                        gte("sales.sale_date", startDate)
                        lte("sales.sale_date", "$endDate 23:59:59")
                        eq("sales.status", "completed")
                    }
                }
                .decodeList<CategorySaleDetailDTO>()

            val report = details
                .groupBy { it.products.categoryId }
                .map { (categoryId, items) ->
                    val categoryName = items.first().products.categories.name
                    val totalRevenue = items.sumOf { it.finalPrice }
                    val productsSold = items.distinctBy { it.productId }.size
                    val salesCount = items.size

                    CategorySalesReport(
                        categoryId = categoryId,
                        categoryName = categoryName,
                        totalRevenue = totalRevenue,
                        productsSold = productsSold,
                        salesCount = salesCount
                    )
                }

            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getCategorySalesReport", e)
            Result.failure(e)
        }
    }

    // -------------------- SUMMARY --------------------
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getReportSummary(
        dateRange: DateRange
    ): Result<ReportSummary> = withContext(Dispatchers.IO) {
        try {
            val startDate = dateRange.startDate.format(dateFormatter)
            val endDate = dateRange.endDate.format(dateFormatter)

            val sales = supabase.postgrest
                .from("sales")
                .select(columns = Columns.list("total")) {
                    filter {
                        gte("sale_date", startDate)
                        lte("sale_date", "$endDate 23:59:59")
                        eq("status", "completed")
                    }
                }
                .decodeList<SaleTotalDTO>()

            val totals = sales.map { it.total }
            val totalRevenue = totals.sum()
            val totalSales = totals.size
            val averageTicket = if (totalSales > 0) totalRevenue / totalSales else 0.0

            val period =
                "${dateRange.startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} - " +
                        "${dateRange.endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"

            Result.success(
                ReportSummary(
                    totalRevenue = totalRevenue,
                    totalSales = totalSales,
                    averageTicket = averageTicket,
                    period = period
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in getReportSummary", e)
            Result.failure(e)
        }
    }

    // -------------------- DATE RANGE --------------------
    @RequiresApi(Build.VERSION_CODES.O)
    fun getDateRangeForFilter(filter: DateFilter, customDate: LocalDate): DateRange {
        return when (filter) {
            DateFilter.DAILY -> DateRange(customDate, customDate)
            DateFilter.WEEKLY -> {
                val start = customDate.minusDays(customDate.dayOfWeek.value.toLong() - 1)
                DateRange(start, start.plusDays(6))
            }
            DateFilter.MONTHLY -> {
                DateRange(
                    customDate.withDayOfMonth(1),
                    customDate.withDayOfMonth(customDate.lengthOfMonth())
                )
            }
        }
    }
}
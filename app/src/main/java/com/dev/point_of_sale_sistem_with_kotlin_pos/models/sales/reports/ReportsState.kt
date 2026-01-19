package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports

import android.os.Build
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.BusinessInfo
import java.time.LocalDate

data class ReportsState @RequiresApi(Build.VERSION_CODES.O) constructor(
    val selectedReportType: ReportType = ReportType.ByCashRegister,
    val selectedDateFilter: DateFilter = DateFilter.DAILY,
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val error: String? = null,

    // Business Info
    val businessInfo: BusinessInfo? = null,

    // Reports data
    val cashRegisterReports: List<CashRegisterSalesReport> = emptyList(),
    val cashierReports: List<CashierSalesReport> = emptyList(),
    val productReports: List<ProductSalesReport> = emptyList(),
    val categoryReports: List<CategorySalesReport> = emptyList(),

    val summary: ReportSummary? = null
)
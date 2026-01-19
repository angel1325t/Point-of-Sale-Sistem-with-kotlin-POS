package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.reports

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.DateFilter
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.ReportType
import java.time.LocalDate

sealed class ReportsIntent {
    data class SelectReportType(val reportType: ReportType) : ReportsIntent()
    data class SelectDateFilter(val dateFilter: DateFilter) : ReportsIntent()
    data class SelectCustomDate(val date: LocalDate) : ReportsIntent()
    object LoadReport : ReportsIntent()
    object RefreshReport : ReportsIntent()
}
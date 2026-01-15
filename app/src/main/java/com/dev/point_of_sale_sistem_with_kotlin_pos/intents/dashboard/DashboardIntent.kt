package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.dashboard

sealed class DashboardIntent {
    object LoadDashboard : DashboardIntent()
    data class SelectDateRange(val startDate: String, val endDate: String) : DashboardIntent()
    object RefreshData : DashboardIntent()
    object ClearError : DashboardIntent()
}
package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.dashboard.DashboardIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.dashboard.DashboardRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.dashboard.DashboardData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(
    private val repository: DashboardRepository,
    private val branchId: String? = null
) : ViewModel() {

    companion object {
        private const val TAG = "DASHBOARD_VM"
        private const val DATE_FORMAT = "yyyy-MM-dd"
    }

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        Log.d(TAG, "🚀 DashboardViewModel inicializado con branchId: $branchId")

        val calendar = Calendar.getInstance()
        val endDate = formatDate(calendar.time)

        calendar.add(Calendar.MONTH, -1)
        val startDate = formatDate(calendar.time)

        _state.update {
            it.copy(
                selectedStartDate = startDate,
                selectedEndDate = endDate
            )
        }

        loadDashboard()
    }

    /**
     * Punto de entrada para todos los intents
     */
    fun handleIntent(intent: DashboardIntent) {
        Log.d(TAG, "📨 Intent recibido: ${intent::class.simpleName}")
        when (intent) {
            is DashboardIntent.LoadDashboard -> loadDashboard()
            is DashboardIntent.RefreshData -> refreshData()
            is DashboardIntent.SelectDateRange ->
                selectDateRange(intent.startDate, intent.endDate)
            is DashboardIntent.ClearError -> clearError()
        }
    }

    /**
     * Carga los datos del dashboard
     */
    private fun loadDashboard() {
        viewModelScope.launch {
            Log.d(TAG, "📊 Cargando dashboard con branchId: $branchId")
            _state.update { it.copy(isLoading = true, error = null) }

            val startDate = _state.value.selectedStartDate
            val endDate = _state.value.selectedEndDate

            repository.getDashboardData(startDate, endDate, branchId)
                .onSuccess { data ->
                    Log.d(TAG, "✅ Dashboard cargado exitosamente")

                    _state.update {
                        it.copy(
                            summary = calculateSummary(data),
                            topProducts = mapTopProducts(data.topProducts),
                            revenueData = mapRevenueData(data.revenueData),
                            isLoading = false,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error cargando dashboard: ${error.message}")
                    _state.update {
                        it.copy(
                            error = when (error) {
                                is DashboardError -> error
                                else -> DashboardError.UnknownError(exception = error)
                            },
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Refresca los datos
     */
    private fun refreshData() {
        viewModelScope.launch {
            Log.d(TAG, "🔄 Refrescando datos...")
            _state.update { it.copy(isRefreshing = true, error = null) }

            val startDate = _state.value.selectedStartDate
            val endDate = _state.value.selectedEndDate

            repository.getDashboardData(startDate, endDate, branchId)
                .onSuccess { data ->
                    Log.d(TAG, "✅ Datos refrescados")

                    _state.update {
                        it.copy(
                            summary = calculateSummary(data),
                            topProducts = mapTopProducts(data.topProducts),
                            revenueData = mapRevenueData(data.revenueData),
                            isRefreshing = false,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error refrescando: ${error.message}")
                    _state.update {
                        it.copy(
                            error = when (error) {
                                is DashboardError -> error
                                else -> DashboardError.UnknownError(exception = error)
                            },
                            isRefreshing = false
                        )
                    }
                }
        }
    }

    /**
     * Selecciona un nuevo rango de fechas
     */
    private fun selectDateRange(startDate: String, endDate: String) {
        Log.d(TAG, "📅 Nuevo rango seleccionado: $startDate a $endDate")

        _state.update {
            it.copy(
                selectedStartDate = startDate,
                selectedEndDate = endDate
            )
        }

        loadDashboard()
    }

    /**
     * Limpia el error
     */
    private fun clearError() {
        Log.d(TAG, "🧹 Limpiando error")
        _state.update { it.copy(error = null) }
    }

    // ═══════════════════════════════════════════════════════════
    // CÁLCULOS Y MAPEOS
    // ═══════════════════════════════════════════════════════════

    private fun calculateSummary(data: DashboardData): DashboardSummary {
        val totalRevenue = data.sales.sumOf { it.totalAmount }
        val totalSales = data.sales.size
        val averageTicket =
            if (totalSales > 0) totalRevenue / totalSales else 0.0

        return DashboardSummary(
            totalRevenue = totalRevenue,
            totalSales = totalSales,
            averageTicket = averageTicket,
            growthPercentage = 0.0
        )
    }

    private fun mapTopProducts(dtos: List<TopProductDTO>): List<TopProduct> {
        val totalRevenue = dtos.sumOf { it.totalRevenue }

        return dtos.map { dto ->
            TopProduct(
                productId = dto.productId,
                productName = dto.productName,
                quantitySold = dto.totalQuantity,
                revenue = dto.totalRevenue,
                percentage =
                    if (totalRevenue > 0)
                        (dto.totalRevenue / totalRevenue) * 100
                    else 0.0
            )
        }
    }

    private fun mapRevenueData(
        dtos: List<RevenueDateDTO>
    ): List<RevenueDataPoint> =
        dtos.map { dto ->
            RevenueDataPoint(
                date = dto.saleDate,
                revenue = dto.totalRevenue,
                salesCount = dto.salesCount
            )
        }

    // ═══════════════════════════════════════════════════════════
    // FECHAS
    // ═══════════════════════════════════════════════════════════

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return formatter.format(date)
    }
}
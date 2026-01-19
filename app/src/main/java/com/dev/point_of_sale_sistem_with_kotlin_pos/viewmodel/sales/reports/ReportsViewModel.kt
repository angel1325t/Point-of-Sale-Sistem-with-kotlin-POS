package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.reports

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.reports.ReportsIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.BusinessInfo
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.ReportType
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.ReportsState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.reports.SalesReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ReportsViewModel(
    private val repository: SalesReportRepository,
    businessInfo: BusinessInfo? = null
) : ViewModel() {

    companion object {
        private const val TAG = "ReportsViewModel"
    }

    private val _state = MutableStateFlow(ReportsState(businessInfo = businessInfo))
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    init {
        Log.d(TAG, "ViewModel INIT with businessInfo: ${businessInfo?.name}")
        loadReport()
    }

    // Método para actualizar BusinessInfo si llega después
    fun setBusinessInfo(businessInfo: BusinessInfo) {
        Log.d(TAG, "setBusinessInfo -> ${businessInfo.name}")
        _state.update { it.copy(businessInfo = businessInfo) }
    }

    fun handleIntent(intent: ReportsIntent) {
        Log.d(TAG, "handleIntent -> $intent")

        when (intent) {
            is ReportsIntent.SelectReportType -> {
                Log.d(TAG, "SelectReportType -> ${intent.reportType}")
                _state.update { it.copy(selectedReportType = intent.reportType, error = null) }
                loadReport()
            }

            is ReportsIntent.SelectDateFilter -> {
                Log.d(TAG, "SelectDateFilter -> ${intent.dateFilter}")
                _state.update { it.copy(selectedDateFilter = intent.dateFilter, error = null) }
                loadReport()
            }

            is ReportsIntent.SelectCustomDate -> {
                Log.d(TAG, "SelectCustomDate -> ${intent.date}")
                _state.update { it.copy(selectedDate = intent.date, error = null) }
                loadReport()
            }

            is ReportsIntent.LoadReport -> {
                Log.d(TAG, "LoadReport")
                loadReport()
            }

            is ReportsIntent.RefreshReport -> {
                Log.d(TAG, "RefreshReport")
                _state.update { it.copy(error = null) }
                loadReport()
            }
        }
    }

    private fun loadReport() {
        viewModelScope.launch {
            Log.d(TAG, "loadReport() START")

            _state.update { it.copy(isLoading = true, error = null) }

            val dateRange = repository.getDateRangeForFilter(
                _state.value.selectedDateFilter,
                _state.value.selectedDate
            )

            Log.d(
                TAG,
                "DateRange -> ${dateRange.startDate} / ${dateRange.endDate}"
            )

            // SUMMARY
            repository.getReportSummary(dateRange).fold(
                onSuccess = {
                    Log.d(TAG, "Summary loaded -> $it")
                    _state.update { state -> state.copy(summary = it) }
                },
                onFailure = {
                    Log.e(TAG, "Error loading summary", it)
                    _state.update { state ->
                        state.copy(isLoading = false, error = it.message)
                    }
                    return@launch
                }
            )

            when (_state.value.selectedReportType) {
                is ReportType.ByCashRegister -> {
                    Log.d(TAG, "Loading ByCashRegister report")
                    loadCashRegisterReport(dateRange)
                }

                is ReportType.ByCashier -> {
                    Log.d(TAG, "Loading ByCashier report")
                    loadCashierReport(dateRange)
                }

                is ReportType.ByProduct -> {
                    Log.d(TAG, "Loading ByProduct report")
                    loadProductReport(dateRange)
                }

                is ReportType.ByCategory -> {
                    Log.d(TAG, "Loading ByCategory report")
                    loadCategoryReport(dateRange)
                }
            }
        }
    }

    private suspend fun loadCashRegisterReport(dateRange: com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.DateRange) {
        repository.getCashRegisterSalesReport(dateRange).fold(
            onSuccess = {
                Log.d(TAG, "CashRegister reports size -> ${it.size}")
                _state.update { state ->
                    state.copy(
                        cashRegisterReports = it,
                        isLoading = false,
                        error = null
                    )
                }
            },
            onFailure = {
                Log.e(TAG, "Error loading CashRegister report", it)
                _state.update { state ->
                    state.copy(isLoading = false, error = it.message)
                }
            }
        )
    }

    private suspend fun loadCashierReport(dateRange: com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.DateRange) {
        repository.getCashierSalesReport(dateRange).fold(
            onSuccess = {
                Log.d(TAG, "Cashier reports size -> ${it.size}")
                _state.update { state ->
                    state.copy(
                        cashierReports = it,
                        isLoading = false,
                        error = null
                    )
                }
            },
            onFailure = {
                Log.e(TAG, "Error loading Cashier report", it)
                _state.update { state ->
                    state.copy(isLoading = false, error = it.message)
                }
            }
        )
    }

    private suspend fun loadProductReport(dateRange: com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.DateRange) {
        repository.getProductSalesReport(dateRange).fold(
            onSuccess = {
                Log.d(TAG, "Product reports size -> ${it.size}")
                _state.update { state ->
                    state.copy(
                        productReports = it,
                        isLoading = false,
                        error = null
                    )
                }
            },
            onFailure = {
                Log.e(TAG, "Error loading Product report", it)
                _state.update { state ->
                    state.copy(isLoading = false, error = it.message)
                }
            }
        )
    }

    private suspend fun loadCategoryReport(dateRange: com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.DateRange) {
        repository.getCategorySalesReport(dateRange).fold(
            onSuccess = {
                Log.d(TAG, "Category reports size -> ${it.size}")
                _state.update { state ->
                    state.copy(
                        categoryReports = it,
                        isLoading = false,
                        error = null
                    )
                }
            },
            onFailure = {
                Log.e(TAG, "Error loading Category report", it)
                _state.update { state ->
                    state.copy(isLoading = false, error = it.message)
                }
            }
        )
    }
}
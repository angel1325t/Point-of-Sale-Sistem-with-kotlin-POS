package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.cash_register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.cash_register.CashRegisterIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CashRegisterViewModel(private val repository: CashRegisterRepository) : ViewModel() {

    companion object {
        private const val TAG = "CashRegisterVM"
        private const val PAGE_SIZE = 5
    }

    private var currentOffset = 0
    private var isLoadingPage = false

    private val _state = MutableStateFlow(CashRegisterState(isLoading = true))
    val state: StateFlow<CashRegisterState> = _state

    fun processIntent(intent: CashRegisterIntent) {
        when (intent) {
            is CashRegisterIntent.LoadCurrentCashRegister -> loadCurrentCashRegister()
            is CashRegisterIntent.OpenCashRegister -> openCashRegister(intent.cashRegisterId, intent.initialBalance)
            is CashRegisterIntent.CloseCashRegister -> closeCashRegister(intent.finalBalance)
            is CashRegisterIntent.CreateCashRegister -> createCashRegister(intent.name)
            is CashRegisterIntent.UpdateCashRegister -> updateCashRegister(intent.cashRegisterId, intent.newName)
            is CashRegisterIntent.DeleteCashRegister -> deleteCashRegister(intent.cashRegisterId)
            is CashRegisterIntent.LoadAllCashRegisters -> loadAllCashRegisters()
            is CashRegisterIntent.LoadCashRegisterHistory -> loadCashRegisterHistory()
            is CashRegisterIntent.LoadMoreHistory -> loadMoreCashRegisterHistory(intent.nextOffset)
            is CashRegisterIntent.ResetHistory -> resetHistory()
            is CashRegisterIntent.ClearSuccess -> _state.update { it.copy(success = false) }
            is CashRegisterIntent.ClearError -> _state.update { it.copy(error = null) }
            is CashRegisterIntent.RefreshAllData -> refreshAllData()
        }
    }

    private fun resetHistory() {
        Log.d(TAG, "resetHistory() - limpiando estado del historial")
        _state.update {
            it.copy(
                historyLoaded = false,
                cashRegisterHistory = emptyList(),
                isLoadingMore = false,
                allPagesLoaded = false,
                error = null
            )
        }
        currentOffset = 0
        isLoadingPage = false
    }

    private fun refreshAllData() {
        _state.update {
            it.copy(
                isLoading = true,
                historyLoaded = false,
                allPagesLoaded = false
            )
        }
        currentOffset = 0
        isLoadingPage = false

        loadAllCashRegisters()
        loadCurrentCashRegister()
    }

    private fun loadCurrentCashRegister() = viewModelScope.launch {
        try {
            val open = repository.getOpenCashRegister()
            _state.update { it.copy(isLoading = false, currentCashRegister = open) }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = mapException(e)) }
        }
    }

    private fun openCashRegister(cashRegisterId: String, initialBalance: Double) =
        viewModelScope.launch {
            if (initialBalance < 0) {
                _state.update {
                    it.copy(error = CashRegisterError.ValidationError("El saldo inicial no puede ser negativo"))
                }
                return@launch
            }

            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val history = repository.openCashRegister(cashRegisterId, initialBalance)
                _state.update {
                    it.copy(isLoading = false, currentCashRegister = history, success = true)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = mapException(e)) }
            }
        }

    private fun closeCashRegister(realFinalBalance: Double) = viewModelScope.launch {
        val current = _state.value.currentCashRegister ?: run {
            _state.update { it.copy(error = CashRegisterError.CashRegisterNotFound) }
            return@launch
        }

        if (realFinalBalance < 0) {
            _state.update {
                it.copy(error = CashRegisterError.ValidationError("El saldo final no puede ser negativo"))
            }
            return@launch
        }

        _state.update { it.copy(isLoading = true) }

        try {
            // ✅ Ahora pasamos el saldo real, el repository calcula expected y difference
            val closedRegister = repository.closeCashRegister(realFinalBalance)

            _state.update {
                it.copy(
                    isLoading = false,
                    currentCashRegister = null,
                    success = true,
                    lastClosedRegister = closedRegister // Para mostrar resumen
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = mapException(e)) }
        }
    }

    private fun createCashRegister(name: String) = viewModelScope.launch {
        if (name.isBlank()) {
            _state.update {
                it.copy(error = CashRegisterError.ValidationError("El nombre no puede estar vacío"))
            }
            return@launch
        }

        _state.update { it.copy(isLoading = true) }

        try {
            repository.createCashRegister(name)
            _state.update { it.copy(isLoading = false, success = true) }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = mapException(e)) }
        }
    }

    private fun updateCashRegister(cashRegisterId: String, newName: String) = viewModelScope.launch {
        if (newName.isBlank()) {
            _state.update {
                it.copy(error = CashRegisterError.ValidationError("El nombre no puede estar vacío"))
            }
            return@launch
        }

        _state.update { it.copy(isLoading = true) }

        try {
            repository.updateCashRegister(cashRegisterId, newName)
            _state.update { it.copy(isLoading = false, success = true) }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = mapException(e)) }
        }
    }

    private fun deleteCashRegister(cashRegisterId: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        try {
            val deleted = repository.deleteCashRegister(cashRegisterId)
            _state.update { it.copy(isLoading = false, success = deleted) }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = mapException(e)) }
        }
    }

    private fun loadAllCashRegisters() = viewModelScope.launch {
        try {
            val list = repository.getAllCashRegisters()
            _state.update { it.copy(isLoading = false, allCashRegisters = list) }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = mapException(e)) }
        }
    }

    private fun loadCashRegisterHistory() = viewModelScope.launch {
        Log.d(TAG, "loadCashRegisterHistory(): currentOffset=$currentOffset")

        if (_state.value.historyLoaded) {
            Log.d(TAG, "historyLoaded = true, no recargando")
            return@launch
        }

        currentOffset = 0
        isLoadingPage = false

        _state.update {
            it.copy(
                isLoading = true,
                historyLoaded = false,
                isLoadingMore = false,
                allPagesLoaded = false,
                cashRegisterHistory = emptyList()
            )
        }

        try {
            val list = repository.getCashRegisterHistory(PAGE_SIZE, currentOffset)
            Log.d(TAG, "Primera carga recibió ${list.size} items")

            val hasMore = list.size == PAGE_SIZE

            _state.update {
                it.copy(
                    isLoading = false,
                    cashRegisterHistory = list,
                    allPagesLoaded = !hasMore,
                    historyLoaded = true
                )
            }

            if (hasMore) {
                currentOffset += PAGE_SIZE
            }

            Log.d(TAG, "Carga inicial completa: allPagesLoaded=${!hasMore}, nextOffset=$currentOffset")
        } catch (e: Exception) {
            Log.e(TAG, "Error en loadCashRegisterHistory", e)
            _state.update {
                it.copy(isLoading = false, error = mapException(e))
            }
        }
    }

    fun loadMoreCashRegisterHistory(nextOffset: Int) = viewModelScope.launch {
        if (isLoadingPage || _state.value.allPagesLoaded) return@launch

        isLoadingPage = true
        _state.update { it.copy(isLoadingMore = true) }

        try {
            val more = repository.getCashRegisterHistory(PAGE_SIZE, nextOffset)
            val hasMore = more.size == PAGE_SIZE

            _state.update {
                it.copy(
                    cashRegisterHistory = it.cashRegisterHistory + more,
                    isLoadingMore = false,
                    allPagesLoaded = !hasMore
                )
            }

            if (hasMore) currentOffset = nextOffset + PAGE_SIZE

        } catch (e: Exception) {
            _state.update { it.copy(isLoadingMore = false, error = mapException(e)) }
        } finally {
            isLoadingPage = false
        }
    }

    private fun mapException(e: Exception) = when {
        e.message?.contains("network", ignoreCase = true) == true ->
            CashRegisterError.NetworkError(e.message ?: "Error de red")
        e.message?.contains("Ya tienes una caja abierta") == true ->
            CashRegisterError.CashRegisterAlreadyOpen
        else -> CashRegisterError.UnknownError(e.message ?: "Error desconocido")
    }
}
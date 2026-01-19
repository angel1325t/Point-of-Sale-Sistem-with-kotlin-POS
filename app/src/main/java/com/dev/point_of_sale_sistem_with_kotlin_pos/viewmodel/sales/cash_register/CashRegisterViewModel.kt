package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.cash_register

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.cash_register.CashRegisterIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.HybridCashRegisterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CashRegisterViewModel(
    private val context: Context,
    private val repository: CashRegisterRepository,
    private val hybridRepository: HybridCashRegisterRepository
) : ViewModel() {

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

    // ═══════════════════════════════════════════════════
    // 🔓 CARGA DE CAJA ACTUAL (HÍBRIDO)
    // ═══════════════════════════════════════════════════

    private fun loadCurrentCashRegister() = viewModelScope.launch {
        try {
            Log.d(TAG, "Loading current cash register...")
            val open = hybridRepository.getOpenCashRegister()
            Log.d(TAG, "Cash register loaded: ${open != null}")
            _state.update { it.copy(isLoading = false, currentCashRegister = open) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cash register", e)
            _state.update { it.copy(isLoading = false, error = mapException(e)) }
        }
    }

    // ═══════════════════════════════════════════════════
    // 🔓 APERTURA DE CAJA (HÍBRIDO)
    // ═══════════════════════════════════════════════════

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
                // ✅ USAR HYBRID REPOSITORY
                hybridRepository.openCashRegister(cashRegisterId, initialBalance)
                    .onSuccess { history ->
                        _state.update {
                            it.copy(isLoading = false, currentCashRegister = history, success = true)
                        }
                        Log.d(TAG, "Cash register opened successfully: ${history.history_id}")
                    }
                    .onFailure { e ->
                        Log.e(TAG, "Error opening cash register", e)
                        _state.update { it.copy(isLoading = false, error = mapException(e as Exception)) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception opening cash register", e)
                _state.update { it.copy(isLoading = false, error = mapException(e)) }
            }
        }

    // ═══════════════════════════════════════════════════
    // 🔒 CIERRE DE CAJA (HÍBRIDO)
    // ═══════════════════════════════════════════════════

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
            // ✅ USAR HYBRID REPOSITORY
            hybridRepository.closeCashRegister(realFinalBalance)
                .onSuccess { closedRegister ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentCashRegister = null,
                            success = true,
                            lastClosedRegister = closedRegister
                        )
                    }
                    Log.d(TAG, "Cash register closed successfully: ${closedRegister.history_id}")
                }
                .onFailure { e ->
                    Log.e(TAG, "Error closing cash register", e)
                    _state.update { it.copy(isLoading = false, error = mapException(e as Exception)) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception closing cash register", e)
            _state.update { it.copy(isLoading = false, error = mapException(e)) }
        }
    }

    // ═══════════════════════════════════════════════════
    // 📋 GESTIÓN DE CAJAS (ONLINE ONLY)
    // ═══════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════
    // 📜 HISTORIAL DE CAJAS (ONLINE ONLY)
    // ═══════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════
    // 🔧 HELPERS
    // ═══════════════════════════════════════════════════

    private fun mapException(e: Exception) = when {
        e is java.net.UnknownHostException ||
        e.message?.contains("network", ignoreCase = true) == true ||
        e.message?.contains("host", ignoreCase = true) == true ||
        e.message?.contains("connection", ignoreCase = true) == true -> {
            CashRegisterError.NetworkError("Sin conexión a internet. La operación se guardará localmente.")
        }
        e.message?.contains("Ya tienes una caja abierta") == true ->
            CashRegisterError.CashRegisterAlreadyOpen
        else -> CashRegisterError.UnknownError(e.message ?: "Error desconocido")
    }
}
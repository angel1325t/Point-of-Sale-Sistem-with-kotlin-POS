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
import kotlinx.coroutines.launch

class CashRegisterViewModel(private val repository: CashRegisterRepository) : ViewModel() {

    companion object {
        private const val TAG = "CashRegisterVM"
    }

    private val _state = MutableStateFlow(CashRegisterState(isLoading = true))
    val state: StateFlow<CashRegisterState> = _state

    fun processIntent(intent: CashRegisterIntent) {
        Log.d(TAG, "Processing intent: $intent")
        when (intent) {
            is CashRegisterIntent.LoadCurrentCashRegister -> loadCurrentCashRegister()
            is CashRegisterIntent.OpenCashRegister -> openCashRegister(intent.cashRegisterId, intent.initialBalance)
            is CashRegisterIntent.CloseCashRegister -> closeCashRegister(intent.finalBalance)
            is CashRegisterIntent.CreateCashRegister -> createCashRegister(intent.name)
            is CashRegisterIntent.UpdateCashRegister -> updateCashRegister(intent.cashRegisterId, intent.newName)
            is CashRegisterIntent.DeleteCashRegister -> deleteCashRegister(intent.cashRegisterId)
            is CashRegisterIntent.LoadAllCashRegisters -> loadAllCashRegisters()
            is CashRegisterIntent.LoadCashRegisterHistory -> loadCashRegisterHistory()

            // NUEVOS INTENTS PARA CONTROL DE ESTADO
            is CashRegisterIntent.ClearSuccess -> {
                _state.value = _state.value.copy(success = false)
            }
            is CashRegisterIntent.ClearError -> {
                _state.value = _state.value.copy(error = null)
            }

            // EL MÁS IMPORTANTE: LIMPIA TODO Y RECARGA
            is CashRegisterIntent.RefreshAllData -> refreshAllData()
        }
    }

    // MÉTODO CLAVE: limpia el estado y recarga todo desde cero
    private fun refreshAllData() {
        Log.d(TAG, "RefreshAllData → Reiniciando estado parcialmente")
        _state.value = _state.value.copy(isLoading = true) // solo loading
        loadAllCashRegisters()
        loadCurrentCashRegister()
    }


    private fun loadCurrentCashRegister() = viewModelScope.launch {
        try {
            val openRegister = repository.getOpenCashRegister()
            _state.value = _state.value.copy(
                isLoading = false,
                currentCashRegister = openRegister
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading current cash register", e)
            _state.value = _state.value.copy(
                isLoading = false,
                error = mapException(e)
            )
        }
    }

    private fun openCashRegister(cashRegisterId: String, initialBalance: Double) = viewModelScope.launch {
        if (initialBalance < 0) {
            _state.value = _state.value.copy(error = CashRegisterError.ValidationError("El saldo inicial no puede ser negativo"))
            return@launch
        }

        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            val history = repository.openCashRegister(cashRegisterId, initialBalance)
            _state.value = _state.value.copy(
                isLoading = false,
                currentCashRegister = history,
                success = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo caja: ${e.message}", e)
            _state.value = _state.value.copy(isLoading = false, error = mapException(e))
        }
    }



    private fun closeCashRegister(finalBalance: Double? = null) = viewModelScope.launch {
        val current = _state.value.currentCashRegister
        if (current == null) {
            _state.value = _state.value.copy(error = CashRegisterError.CashRegisterNotFound)
            return@launch
        }

        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            val balanceToClose = finalBalance ?: current.initial_balance
            repository.closeCashRegister(balanceToClose)

            _state.value = _state.value.copy(
                isLoading = false,
                currentCashRegister = null,
                success = true
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = mapException(e)
            )
        }
    }

    private fun createCashRegister(name: String) = viewModelScope.launch {
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = CashRegisterError.ValidationError("El nombre no puede estar vacío"))
            return@launch
        }

        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            repository.createCashRegister(name)
            _state.value = _state.value.copy(isLoading = false, success = true)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = mapException(e))
        }
    }


    private fun updateCashRegister(cashRegisterId: String, newName: String) = viewModelScope.launch {
        if (newName.isBlank()) {
            _state.value = _state.value.copy(error = CashRegisterError.ValidationError("El nombre no puede estar vacío"))
            return@launch
        }

        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            repository.updateCashRegister(cashRegisterId, newName)
            _state.value = _state.value.copy(isLoading = false, success = true)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = mapException(e))
        }
    }


    private fun deleteCashRegister(cashRegisterId: String) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            val deleted = repository.deleteCashRegister(cashRegisterId)
            _state.value = _state.value.copy(
                isLoading = false,
                success = deleted
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = mapException(e))
        }
    }


    private fun loadAllCashRegisters() = viewModelScope.launch {
        try {
            val list = repository.getAllCashRegisters()
            _state.value = _state.value.copy(isLoading = false, allCashRegisters = list)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = mapException(e))
        }
    }

    private fun loadCashRegisterHistory() = viewModelScope.launch {
        try {
            val historyList = repository.getCashRegisterHistory()
            _state.value = _state.value.copy(cashRegisterHistory = historyList)
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = mapException(e))
        }
    }

    private fun mapException(e: Exception) = when {
        e.message?.contains("network", ignoreCase = true) == true ->
            CashRegisterError.NetworkError(e.message ?: "Error de red")
        else -> CashRegisterError.UnknownError(e.message ?: "Error desconocido")
    }
}
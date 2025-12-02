package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.braches.BranchIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches.BranchError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches.BranchState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.BranchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BranchViewModel(private val repository: BranchRepository) : ViewModel() {

    private val TAG = "BranchViewModel"

    private val _state = MutableStateFlow(BranchState())
    val state: StateFlow<BranchState> = _state.asStateFlow()

    init {
        Log.d(TAG, "Inicializando → Cargando sucursales")
        handleIntent(BranchIntent.LoadBranches)
    }

    fun handleIntent(intent: BranchIntent) {
        Log.d(TAG, "Intent recibido: $intent")

        when (intent) {
            is BranchIntent.LoadBranches -> loadBranches()
            is BranchIntent.CreateBranch -> {
                Log.d(TAG, "Creando sucursal con alias=${intent.alias}")
                createBranch(intent.alias, intent.address, intent.phone, intent.city)
            }
            is BranchIntent.UpdateBranch -> {
                Log.d(TAG, "Actualizando sucursal ${intent.branchId}")
                updateBranch(intent.branchId, intent.alias, intent.address, intent.phone, intent.city)
            }
            is BranchIntent.DeleteBranch -> {
                Log.d(TAG, "Eliminando sucursal ${intent.branchId}")
                deleteBranch(intent.branchId)
            }
            is BranchIntent.ToggleBranchStatus -> {
                Log.d(TAG, "Cambiando estado sucursal ${intent.branchId} a active=${intent.active}")
                toggleBranchStatus(intent.branchId, intent.active)
            }
            is BranchIntent.ClearError -> {
                Log.d(TAG, "Limpiando errores y successMessage")
                clearError()
            }
        }
    }

    private fun loadBranches() {
        Log.d(TAG, "Cargando sucursales...")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.getBranches()
                .onSuccess { branches ->
                    Log.d(TAG, "Sucursales recibidas: ${branches.size}")
                    _state.value = _state.value.copy(
                        branches = branches,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    Log.e(TAG, "Error al cargar sucursales: ${exception.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = BranchError.DatabaseError(exception.message ?: "Error loading branches")
                    )
                }
        }
    }

    private fun createBranch(alias: String, address: String, phone: String, city: String) {
        Log.d(TAG, "Validando datos para crear sucursal...")
        viewModelScope.launch {

            if (alias.isBlank()) {
                Log.e(TAG, "Error: alias vacío")
                _state.value = _state.value.copy(
                    error = BranchError.ValidationError("El alias no puede estar vacío")
                )
                return@launch
            }

            Log.d(TAG, "Creando sucursal en el repositorio...")
            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.createBranch(alias, address, phone, city)
                .onSuccess { newBranch ->
                    Log.d(TAG, "Sucursal creada: $newBranch")
                    _state.value = _state.value.copy(
                        branches = listOf(newBranch) + _state.value.branches,
                        isLoading = false,
                        successMessage = "Sucursal creada exitosamente",
                        error = null
                    )
                }
                .onFailure { exception ->
                    Log.e(TAG, "Error al crear sucursal: ${exception.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = BranchError.DatabaseError(exception.message ?: "Error creating branch")
                    )
                }
        }
    }

    private fun updateBranch(branchId: String, alias: String, address: String, phone: String, city: String) {
        Log.d(TAG, "Validando datos para actualizar sucursal $branchId")
        viewModelScope.launch {

            if (alias.isBlank()) {
                Log.e(TAG, "Error: alias vacío")
                _state.value = _state.value.copy(
                    error = BranchError.ValidationError("El alias no puede estar vacío")
                )
                return@launch
            }

            Log.d(TAG, "Actualizando sucursal en el repositorio...")
            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.updateBranch(branchId, alias, address, phone, city)
                .onSuccess { updatedBranch ->
                    Log.d(TAG, "Sucursal actualizada: $updatedBranch")
                    val updatedList = _state.value.branches.map {
                        if (it.branchId == branchId) updatedBranch else it
                    }
                    _state.value = _state.value.copy(
                        branches = updatedList,
                        isLoading = false,
                        successMessage = "Sucursal actualizada exitosamente",
                        error = null
                    )
                }
                .onFailure { exception ->
                    Log.e(TAG, "Error al actualizar sucursal: ${exception.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = BranchError.DatabaseError(exception.message ?: "Error updating branch")
                    )
                }
        }
    }

    private fun deleteBranch(branchId: String) {
        Log.d(TAG, "Eliminando sucursal $branchId")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.deleteBranch(branchId)
                .onSuccess {
                    Log.d(TAG, "Sucursal eliminada exitosamente")
                    val updatedList = _state.value.branches.filter {
                        it.branchId != branchId
                    }
                    _state.value = _state.value.copy(
                        branches = updatedList,
                        isLoading = false,
                        successMessage = "Sucursal eliminada exitosamente",
                        error = null
                    )
                }
                .onFailure { exception ->
                    Log.e(TAG, "Error al eliminar sucursal: ${exception.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = BranchError.DatabaseError(exception.message ?: "Error deleting branch")
                    )
                }
        }
    }

    private fun toggleBranchStatus(branchId: String, active: Boolean) {
        Log.d(TAG, "Cambiando estado sucursal $branchId → active=$active")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.toggleBranchStatus(branchId, active)
                .onSuccess {
                    Log.d(TAG, "Estado actualizado correctamente")
                    val updatedList = _state.value.branches.map {
                        if (it.branchId == branchId) it.copy(active = active) else it
                    }
                    _state.value = _state.value.copy(
                        branches = updatedList,
                        isLoading = false,
                        successMessage = "Estado actualizado exitosamente",
                        error = null
                    )
                }
                .onFailure { exception ->
                    Log.e(TAG, "Error al cambiar estado: ${exception.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = BranchError.DatabaseError(exception.message ?: "Error updating status")
                    )
                }
        }
    }

    private fun clearError() {
        Log.d(TAG, "Limpiando estado: error y successMessage")
        _state.value = _state.value.copy(error = null, successMessage = null)
    }
}

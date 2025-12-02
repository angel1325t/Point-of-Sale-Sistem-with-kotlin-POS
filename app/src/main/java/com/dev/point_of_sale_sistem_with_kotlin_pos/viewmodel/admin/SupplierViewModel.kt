package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.suppliers.SupplierIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.suppliers.SupplierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SupplierViewModel(
    private val repository: SupplierRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierState())
    val state: StateFlow<SupplierState> = _state.asStateFlow()

    init {
        handleIntent(SupplierIntent.LoadSuppliers)
    }

    // ───────────────────────────────────────────────
    // MANEJO DE INTENTS
    // ───────────────────────────────────────────────
    fun handleIntent(intent: SupplierIntent) {
        when (intent) {
            is SupplierIntent.LoadSuppliers -> loadSuppliers()
            is SupplierIntent.LoadSupplierById -> loadSupplierById(intent.supplierId)
            is SupplierIntent.SearchSupplier -> searchSuppliers(intent.query)
            is SupplierIntent.CreateSupplier -> createSupplier(
                name = intent.name,
                contact = intent.contact,
                phone = intent.phone,
                email = intent.email,
                address = intent.address
            )
            is SupplierIntent.UpdateSupplier -> updateSupplier(
                supplierId = intent.supplierId,
                name = intent.name,
                contact = intent.contact,
                phone = intent.phone,
                email = intent.email,
                address = intent.address
            )
            is SupplierIntent.DeleteSupplier -> deleteSupplier(intent.supplierId)
        }
    }

    // ───────────────────────────────────────────────
    // MÉTODOS PÚBLICOS ADICIONALES (Helper Methods)
    // ───────────────────────────────────────────────

    fun selectSupplier(supplierId: Int?) {
        if (supplierId == null) {
            clearSelection()
            return
        }
        val supplier = _state.value.suppliers.find { it.supplierId == supplierId }
        _state.update { it.copy(selectedSupplier = supplier) }
    }

    fun clearSelection() {
        _state.update {
            it.copy(
                selectedSupplier = null,
                nameError = null,
                emailError = null,
                phoneError = null
            )
        }
    }

    fun clearError() {
        _state.update {
            it.copy(
                error = null,
                operationSuccess = false,
                successMessage = null
            )
        }
    }

    fun resetState() {
        _state.update { SupplierState() }
        loadSuppliers()
    }

    fun validateSupplierName(name: String) {
        val error = when {
            name.isBlank() -> "El nombre no puede estar vacío"
            name.length < 2 -> "Debe tener al menos 2 caracteres"
            name.length > 100 -> "Máximo 100 caracteres"
            else -> null
        }
        _state.update { it.copy(nameError = error) }
    }

    fun validatePhone(phone: String?) {
        val error = when {
            !phone.isNullOrBlank() && phone.length < 8 -> "Teléfono demasiado corto"
            !phone.isNullOrBlank() && phone.length > 20 -> "Teléfono demasiado largo"
            else -> null
        }
        _state.update { it.copy(phoneError = error) }
    }

    fun validateEmail(email: String?) {
        val error = when {
            !email.isNullOrBlank() && !email.contains("@") -> "Email inválido"
            !email.isNullOrBlank() && email.length > 255 -> "Email demasiado largo"
            else -> null
        }
        _state.update { it.copy(emailError = error) }
    }

    // ───────────────────────────────────────────────
    // CRUD OPERATIONS (Private)
    // ───────────────────────────────────────────────

    private fun loadSuppliers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getAllSuppliers()
                .onSuccess { suppliers ->
                    _state.update {
                        it.copy(
                            suppliers = suppliers,
                            filteredSuppliers = suppliers,
                            isLoading = false,
                            totalItems = suppliers.size
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable as? SupplierError
                                ?: SupplierError.UnknownError(cause = throwable)
                        )
                    }
                }
        }
    }

    private fun loadSupplierById(id: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getSupplierById(id)
                .onSuccess { supplier ->
                    _state.update {
                        it.copy(
                            selectedSupplier = supplier,
                            isLoading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable as? SupplierError
                                ?: SupplierError.UnknownError(cause = throwable)
                        )
                    }
                }
        }
    }

    private fun searchSuppliers(query: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    searchQuery = query,
                    isLoading = true,
                    error = null
                )
            }

            if (query.isBlank()) {
                _state.update {
                    it.copy(
                        filteredSuppliers = it.suppliers,
                        isLoading = false
                    )
                }
                return@launch
            }

            repository.searchSuppliers(query)
                .onSuccess { results ->
                    _state.update {
                        it.copy(
                            filteredSuppliers = results,
                            isLoading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable as? SupplierError
                                ?: SupplierError.UnknownError(cause = throwable)
                        )
                    }
                }
        }
    }

    private fun createSupplier(
        name: String,
        contact: String?,
        phone: String?,
        email: String?,
        address: String?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.createSupplier(name, contact, phone, email, address)
                .onSuccess { supplier ->
                    _state.update {
                        it.copy(
                            suppliers = it.suppliers + supplier,
                            filteredSuppliers = it.filteredSuppliers + supplier,
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Proveedor creado exitosamente",
                            selectedSupplier = null,
                            nameError = null,
                            emailError = null,
                            phoneError = null,
                            totalItems = it.suppliers.size + 1
                        )
                    }
                }
                .onFailure { throwable ->
                    val error = throwable as? SupplierError
                        ?: SupplierError.UnknownError(cause = throwable)

                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error,
                            operationSuccess = false,
                            nameError = when (error) {
                                is SupplierError.ValidationError ->
                                    if (error.field == "name") error.message else it.nameError
                                is SupplierError.DuplicateNameError -> error.message
                                else -> it.nameError
                            },
                            phoneError = when (error) {
                                is SupplierError.InvalidPhoneError -> error.message
                                else -> it.phoneError
                            },
                            emailError = when (error) {
                                is SupplierError.InvalidEmailError -> error.message
                                else -> it.emailError
                            }
                        )
                    }
                }
        }
    }

    private fun updateSupplier(
        supplierId: Int,
        name: String,
        contact: String?,
        phone: String?,
        email: String?,
        address: String?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.updateSupplier(supplierId, name, contact, phone, email, address)
                .onSuccess { supplier ->
                    _state.update {
                        it.copy(
                            suppliers = it.suppliers.map { s ->
                                if (s.supplierId == supplierId) supplier else s
                            },
                            filteredSuppliers = it.filteredSuppliers.map { s ->
                                if (s.supplierId == supplierId) supplier else s
                            },
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Proveedor actualizado exitosamente",
                            selectedSupplier = null,
                            nameError = null,
                            emailError = null,
                            phoneError = null
                        )
                    }
                }
                .onFailure { throwable ->
                    val error = throwable as? SupplierError
                        ?: SupplierError.UnknownError(cause = throwable)

                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error,
                            operationSuccess = false,
                            nameError = when (error) {
                                is SupplierError.ValidationError ->
                                    if (error.field == "name") error.message else it.nameError
                                is SupplierError.DuplicateNameError -> error.message
                                else -> it.nameError
                            },
                            phoneError = when (error) {
                                is SupplierError.InvalidPhoneError -> error.message
                                else -> it.phoneError
                            },
                            emailError = when (error) {
                                is SupplierError.InvalidEmailError -> error.message
                                else -> it.emailError
                            }
                        )
                    }
                }
        }
    }

    private fun deleteSupplier(supplierId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.deleteSupplier(supplierId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            suppliers = it.suppliers.filter { s -> s.supplierId != supplierId },
                            filteredSuppliers = it.filteredSuppliers.filter { s -> s.supplierId != supplierId },
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Proveedor eliminado exitosamente",
                            selectedSupplier = null,
                            totalItems = it.suppliers.size - 1
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable as? SupplierError
                                ?: SupplierError.UnknownError(cause = throwable),
                            operationSuccess = false
                        )
                    }
                }
        }
    }
}
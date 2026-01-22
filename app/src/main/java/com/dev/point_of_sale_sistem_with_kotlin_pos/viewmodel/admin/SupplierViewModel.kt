package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.suppliers.SupplierIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.SupplierError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.SupplierState
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

    fun handleIntent(intent: SupplierIntent) {
        when (intent) {
            is SupplierIntent.LoadSuppliers -> loadSuppliers()
            is SupplierIntent.LoadSupplierById -> loadSupplierById(intent.supplierId)
            is SupplierIntent.CreateSupplier -> createSupplier(
                intent.name, intent.contact, intent.phone, intent.email, intent.address
            )
            is SupplierIntent.UpdateSupplier -> updateSupplier(
                intent.supplierId, intent.name, intent.contact,
                intent.phone, intent.email, intent.address
            )
            is SupplierIntent.DeleteSupplier -> deleteSupplier(intent.supplierId)
            is SupplierIntent.SearchSupplier -> searchSuppliers(intent.query)
            is SupplierIntent.FilterSuppliers -> filterSuppliers(intent.onlyCompleteContact)
            is SupplierIntent.ValidateName -> validateSupplierName(intent.name)
            is SupplierIntent.ValidatePhone -> validatePhone(intent.phone)
            is SupplierIntent.ValidateEmail -> validateEmail(intent.email)
            is SupplierIntent.ClearError -> clearError()
            is SupplierIntent.ClearSelectedSupplier -> clearSelectedSupplier()
        }
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = repository.getAllSuppliers()
            result.fold(
                onSuccess = { suppliers ->
                    _state.update {
                        it.copy(
                            suppliers = suppliers,
                            displaySuppliers = suppliers,
                            isLoading = false,
                            isFiltered = false,
                            searchQuery = ""
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? SupplierError ?: SupplierError.UnknownError(cause=error)
                        )
                    }
                }
            )
        }
    }

    private fun loadSupplierById(supplierId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = repository.getSupplierById(supplierId)
            result.fold(
                onSuccess = { supplier ->
                    _state.update {
                        it.copy(
                            selectedSupplier = supplier,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? SupplierError ?: SupplierError.UnknownError(cause=error)
                        )
                    }
                }
            )
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
            if (!validateBeforeSave(name, phone, email)) return@launch
            _state.update { it.copy(isLoading = true, error = null) }

            val result = repository.createSupplier(name, contact, phone, email, address)
            result.fold(
                onSuccess = { newSupplier ->
                    val updatedList = _state.value.suppliers + newSupplier
                    _state.update {
                        it.copy(
                            suppliers = updatedList,
                            displaySuppliers = updatedList,
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Proveedor creado exitosamente"
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? SupplierError ?: SupplierError.UnknownError(cause=error)
                        )
                    }
                }
            )
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
            if (!validateBeforeSave(name, phone, email)) return@launch
            _state.update { it.copy(isLoading = true, error = null) }

            val result = repository.updateSupplier(supplierId, name, contact, phone, email, address)
            result.fold(
                onSuccess = { updatedSupplier ->
                    val updatedList = _state.value.suppliers.map {
                        if (it.supplierId == supplierId) updatedSupplier else it
                    }
                    _state.update {
                        it.copy(
                            suppliers = updatedList,
                            displaySuppliers = updatedList,
                            selectedSupplier = updatedSupplier,
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Proveedor actualizado exitosamente"
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? SupplierError ?: SupplierError.UnknownError(cause=error)
                        )
                    }
                }
            )
        }
    }

    private fun deleteSupplier(supplierId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = repository.deleteSupplier(supplierId)
            result.fold(
                onSuccess = {
                    val updatedList = _state.value.suppliers.filter { it.supplierId != supplierId }
                    _state.update {
                        it.copy(
                            suppliers = updatedList,
                            displaySuppliers = updatedList,
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Proveedor eliminado exitosamente"
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? SupplierError ?: SupplierError.UnknownError(cause = error)
                        )
                    }
                }
            )
        }
    }


    private fun searchSuppliers(query: String) {
        _state.update { it.copy(searchQuery = query) }

        val filtered = if (query.isBlank()) {
            _state.value.suppliers
        } else {
            _state.value.suppliers.filter { it.matchesSearch(query) }
        }

        _state.update { it.copy(displaySuppliers = filtered) }
    }

    private fun filterSuppliers(onlyCompleteContact: Boolean) {
        val filtered = if (onlyCompleteContact) {
            _state.value.suppliers.filter { it.hasCompleteContact() }
        } else {
            _state.value.suppliers
        }

        _state.update {
            it.copy(
                displaySuppliers = filtered,
                isFiltered = onlyCompleteContact
            )
        }
    }

    fun validateSupplierName(name: String) {
        val error = when {
            name.isBlank() -> "El nombre es requerido"
            name.length < 3 -> "El nombre debe tener al menos 3 caracteres"
            name.length > 100 -> "El nombre no puede exceder 100 caracteres"
            else -> null
        }
        _state.update { it.copy(nameError = error) }
    }

    fun validatePhone(phone: String) {
        if (phone.isBlank()) {
            _state.update { it.copy(phoneError = null) }
            return
        }

        val error = when {
            phone.length < 10 -> "El teléfono debe tener al menos 10 dígitos"
            phone.length > 15 -> "El teléfono no puede exceder 15 dígitos"
            !phone.matches(Regex("^[0-9+\\-() ]+$")) -> "Formato de teléfono inválido"
            else -> null
        }
        _state.update { it.copy(phoneError = error) }
    }

    fun validateEmail(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(emailError = null) }
            return
        }

        val error = if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            "Email inválido"
        } else null

        _state.update { it.copy(emailError = error) }
    }

    private fun validateBeforeSave(name: String, phone: String?, email: String?): Boolean {
        validateSupplierName(name)
        phone?.let { validatePhone(it) }
        email?.let { validateEmail(it) }

        val currentState = _state.value
        return currentState.nameError == null &&
                currentState.phoneError == null &&
                currentState.emailError == null
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

    private fun clearSelectedSupplier() {
        _state.update {
            it.copy(
                selectedSupplier = null,
                nameError = null,
                phoneError = null,
                emailError = null
            )
        }
    }
}

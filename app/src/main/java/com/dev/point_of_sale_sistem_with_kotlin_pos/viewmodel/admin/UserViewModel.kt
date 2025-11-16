package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.users.UserIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.UserError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.UserState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import kotlinx.coroutines.launch
import java.util.UUID

class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    var state by mutableStateOf(UserState())
        private set

    fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUsers -> loadUsers()
            is UserIntent.LoadUser -> loadUser(intent.id)
            is UserIntent.CreateUser -> createUser(intent.email, intent.roleId, intent.branchId)
            is UserIntent.UpdateUser -> updateUser(intent.userId, intent.email, intent.branchId, intent.roleId)
            is UserIntent.DeleteUser -> deleteUser(intent.id)
            is UserIntent.LoadRoles -> loadRoles()
            is UserIntent.LoadBranches -> loadBranches()
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val users = repository.getAllActiveUsers()
                state = state.copy(isLoading = false, users = users)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = UserError.ConnectionError)
            }
        }
    }

    private fun loadUser(id: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val user = repository.getUserById(id)
                if (user != null) {
                    state = state.copy(isLoading = false, selectedUser = user)
                } else {
                    state = state.copy(isLoading = false, error = UserError.UserNotFound)
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = UserError.Other(e.message))
            }
        }
    }

    private fun createUser(email: String, roleId: Int, branchId: UUID) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val currentUser = repository.getCurrentUser()
                    ?: throw IllegalStateException("ERROR_NO_CURRENT_USER")

                val companyId = currentUser.company_id
                    ?: throw IllegalStateException("ERROR_NO_COMPANY_ID")

                repository.createUser(email, roleId, companyId, branchId)

                state = state.copy(
                    isLoading = false,
                    successMessage = "SUCCESS_CREATE_USER"
                )

                loadUsers()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Real error: ", e)

                val error = when {
                    e.message?.contains("duplicate", ignoreCase = true) == true ->
                        UserError.EmailAlreadyExists

                    e.message?.contains("auth", ignoreCase = true) == true ->
                        UserError.AuthError

                    else -> UserError.Other(e.message)
                }

                state = state.copy(isLoading = false, error = error)
            }
        }
    }

    private fun updateUser(userId: String, email: String?, branchId: UUID?, roleId: Int?) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                repository.updateUser(userId, branchId, roleId)
                state = state.copy(
                    isLoading = false,
                    successMessage = "SUCCESS_UPDATE_USER"
                )
                loadUsers()
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = UserError.Other(e.message))
            }
        }
    }

    private fun deleteUser(id: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                repository.deleteUser(id)
                state = state.copy(
                    isLoading = false,
                    successMessage = "SUCCESS_DELETE_USER"
                )
                loadUsers()
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = UserError.Other(e.message))
            }
        }
    }

    private fun loadRoles() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val roles = repository.getAllRoles()
                state = state.copy(isLoading = false, roles = roles)
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = UserError.Other("ERROR_LOAD_ROLES")
                )
            }
        }
    }

    private fun loadBranches() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val currentUser = repository.getCurrentUser()
                    ?: throw IllegalStateException("ERROR_NO_CURRENT_USER")

                val companyId = currentUser.company_id
                    ?: throw IllegalStateException("ERROR_NO_COMPANY_ID")

                val branches = repository.getBranchesByCompany(companyId)

                state = state.copy(isLoading = false, branches = branches)

            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = UserError.Other("ERROR_LOAD_BRANCHES")
                )
            }
        }
    }

    fun clearMessages() {
        state = state.copy(error = null, successMessage = null)
    }

    fun clearSelectedUser() {
        state = state.copy(selectedUser = null)
    }
}

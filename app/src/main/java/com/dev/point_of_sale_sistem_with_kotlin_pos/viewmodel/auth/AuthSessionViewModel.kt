package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.capabilities.CapabilitiesResolver
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.NetworkMonitor
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.SessionState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AuthSessionViewModel(
    private val supabase: SupabaseClient,
    private val sessionPreferences: SessionPreferences,
    private val networkMonitor: NetworkMonitor

) : ViewModel() {

    private val repository = AuthRepository(supabase)

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _intents = Channel<AuthIntent>(Channel.UNLIMITED)

    // 🌐 Estado de conectividad (temporal)
    private var isOfflineMode: Boolean = false

    companion object {
        private const val TAG = "AuthSessionViewModel"
    }

    init {
        Log.d(TAG, "Initializing AuthSessionViewModel")
        observeIntents()
        sendIntent(AuthIntent.CheckSession)
        updateCapabilities()
        viewModelScope.launch {
            networkMonitor.isOnline().collect { isOnline ->
                setOfflineMode(!isOnline)
            }
        }

    }

    private fun observeIntents() {
        viewModelScope.launch {
            _intents.receiveAsFlow().collect { intent ->
                when (intent) {
                    is AuthIntent.CheckSession -> handleCheckSession()
                    is AuthIntent.Logout -> handleLogout()
                    is AuthIntent.UserDisabled -> handleUserDisabled(intent)
                    is AuthIntent.ChangeBranch -> handleChangeBranch(intent.branchId)
                    else -> {}
                }
            }
        }
    }

    fun sendIntent(intent: AuthIntent) {
        viewModelScope.launch { _intents.send(intent) }
    }

    // 🧠 CAPABILITIES (cálculo central)
    private fun updateCapabilities() {
        val capabilities = CapabilitiesResolver.resolve(
            isOffline = isOfflineMode
        )

        _state.value = _state.value.copy(
            isOffline = isOfflineMode,
            capabilities = capabilities
        )
    }

    private fun handleCheckSession() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val authUser = supabase.auth.currentUserOrNull()

            if (authUser == null) {
                _state.value = SessionState()
                updateCapabilities()
                return@launch
            }

            try {
                val authId = UUID.fromString(authUser.id)

                val userInfo = repository.getUserByAuthId(authId)

                if (!userInfo.active) {
                    sessionPreferences.setUserDisabled(true)

                    _state.value = SessionState(
                        isUserDisabled = true,
                        error = AuthError.Other("USER_DISABLED")
                    )
                    updateCapabilities()
                    return@launch
                }

                sessionPreferences.setUserDisabled(false)

                _state.value = _state.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    authId = authUser.id,
                    email = authUser.email,
                    userId = userInfo.user_id,
                    branchId = userInfo.branch_id,
                    isUserDisabled = false,
                    error = null
                )

                updateCapabilities()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading session", e)

                _state.value = SessionState(
                    error = AuthError.Other("SESSION_LOAD_FAILED")
                )
                updateCapabilities()
            }
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(
            isLoading = false,
            isAuthenticated = false,
            isUserDisabled = false,
            error = null,
            successMessage = null
        )
    }

    private fun handleUserDisabled(intent: AuthIntent.UserDisabled) {
        viewModelScope.launch {
            _state.value = SessionState(
                isUserDisabled = true
            )
            sessionPreferences.setUserDisabled(true)
            updateCapabilities()
        }
    }


    private fun handleChangeBranch(branchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                sessionPreferences.saveBranchId(branchId)

                _state.value = _state.value.copy(
                    isLoading = false,
                    branchId = branchId,
                    successMessage = "BRANCH_CHANGED"
                )

                kotlinx.coroutines.delay(2000)
                _state.value = _state.value.copy(successMessage = null)

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "BRANCH_CHANGE_FAILED")
                )
            }
        }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            repository.logout()
            sessionPreferences.clearBranchId()

            _state.value = SessionState()
            updateCapabilities()
        }
    }

    // 🔧 Se usará en el Paso 4
    fun setOfflineMode(isOffline: Boolean) {
        if (isOfflineMode != isOffline) {
            isOfflineMode = isOffline
            updateCapabilities()
        }
    }
}

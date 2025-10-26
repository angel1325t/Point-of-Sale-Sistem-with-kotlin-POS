package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.jan.supabase.SupabaseClient

class RegisterViewModelFactory(
    private val supabase: SupabaseClient,
    private val authSessionViewModel: AuthSessionViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            // Retrieve SavedStateHandle from CreationExtras
            val savedStateHandle = extras.createSavedStateHandle()
            return RegisterViewModel(supabase, authSessionViewModel, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
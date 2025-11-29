package com.dev.point_of_sale_sistem_with_kotlin_pos.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLifecycleObserver : DefaultLifecycleObserver {

    // True = la app volvió al foreground y NECESITA validación
    private val _requireBiometric = MutableStateFlow(false)
    val requireBiometric = _requireBiometric.asStateFlow()

    private var wasInBackground = false

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        // La app se fue al background
        wasInBackground = true
    }

    override fun onStart(owner: LifecycleOwner) {
        // La app volvió al foreground
        if (wasInBackground) {
            _requireBiometric.value = true
        }
        wasInBackground = false
    }

    fun reset() {
        _requireBiometric.value = false
    }
}

package com.dev.point_of_sale_sistem_with_kotlin_pos.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLifecycleObserver : DefaultLifecycleObserver {

    private val _requireBiometric = MutableStateFlow(false)
    val requireBiometric = _requireBiometric.asStateFlow()

    private var wasInBackground = false
    private var _isFirstLaunch = true

    val isFirstLaunch: Boolean
        get() = _isFirstLaunch

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        wasInBackground = true
    }

    override fun onStart(owner: LifecycleOwner) {
        if (_isFirstLaunch) {
            _isFirstLaunch = false
            return
        }

        if (wasInBackground) {
            _requireBiometric.value = true
        }

        wasInBackground = false
    }

    fun reset() {
        _requireBiometric.value = false
    }

    fun resetFirstLaunch() {
        _isFirstLaunch = true
    }
}
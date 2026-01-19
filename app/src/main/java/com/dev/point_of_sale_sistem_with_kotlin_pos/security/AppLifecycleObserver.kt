package com.dev.point_of_sale_sistem_with_kotlin_pos.security

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppLifecycleObserver : DefaultLifecycleObserver {

    private const val TAG = "AppLifecycleObserver"

    private var wasInBackground = false
    var isFirstLaunch = true
        private set

    private val _requireBiometric = MutableStateFlow(false)
    val requireBiometric: StateFlow<Boolean> get() = _requireBiometric

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "APP → BACKGROUND")
        if (!isFirstLaunch) {
            wasInBackground = true
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "APP → FOREGROUND")
        if (wasInBackground && !isFirstLaunch) {
            Log.d(TAG, "REGRESÓ DEL BACKGROUND → pedir biometría")
            _requireBiometric.value = true
        }

        wasInBackground = false
        isFirstLaunch = false
    }

    fun reset() {
        Log.d(TAG, "RESET biometric trigger")
        _requireBiometric.value = false
    }

    fun resetFirstLaunch() {
        _isFirstLaunch = true
    }
}
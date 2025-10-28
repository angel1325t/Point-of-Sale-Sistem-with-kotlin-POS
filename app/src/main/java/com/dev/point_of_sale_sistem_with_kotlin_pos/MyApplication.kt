package com.dev.point_of_sale_sistem_with_kotlin_pos

import android.app.Application
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.createSupabaseClient
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar Supabase con persistencia de sesión
        supabase = createSupabaseClient(applicationContext)
    }
}
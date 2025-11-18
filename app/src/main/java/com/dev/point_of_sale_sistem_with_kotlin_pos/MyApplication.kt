package com.dev.point_of_sale_sistem_with_kotlin_pos

import android.app.Application
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.createSupabaseClient
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase

class MyApplication : Application() {

    // ✅ Instancia global de SessionPreferences
    lateinit var sessionPreferences: SessionPreferences
        private set

    override fun onCreate() {
        super.onCreate()

        try {
            supabase = createSupabaseClient(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            sessionPreferences = SessionPreferences(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
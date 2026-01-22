package com.dev.point_of_sale_sistem_with_kotlin_pos.repository

import android.content.Context
import com.dev.point_of_sale_sistem_with_kotlin_pos.BuildConfig
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.utils.AndroidSessionManager
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.utils.UUIDSerializer
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.util.UUID

// Función para crear el cliente con contexto
fun createSupabaseClient(context: Context) = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_KEY
) {

    install(Auth) {
        sessionManager = AndroidSessionManager(context)
        autoSaveToStorage = true
        autoLoadFromStorage = true
    }

    install(Postgrest) {
        serializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true  // ← AGREGA ESTA LÍNEA
                serializersModule = SerializersModule {
                    contextual(UUID::class, UUIDSerializer)
                }
            }
        )
    }

    install(Storage)

    install(Functions)
}

// Variable global para usar en los ViewModels
lateinit var supabase: io.github.jan.supabase.SupabaseClient
package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities

enum class SyncStatus {
    PENDING,   // pendiente de subir
    SYNCED,    // ya sincronizado
    ERROR      // falló la sync
}

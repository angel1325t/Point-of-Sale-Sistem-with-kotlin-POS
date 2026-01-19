package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.utils

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleError

fun SaleError.toUserMessage(): String =
    when (this) {
        SaleError.Network ->
            "Error de conexión. Verifica tu internet."

        SaleError.ValidationFailed ->
            "Datos inválidos. Verifica la información."

        is SaleError.Server ->
            message

        is SaleError.Unknown ->
            "Error inesperado. Intenta nuevamente."
    }

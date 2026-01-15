package com.dev.point_of_sale_sistem_with_kotlin_pos.data.mappers

import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.relations.OfflineSaleWithDetails
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetail
import java.time.LocalDateTime
import java.util.UUID

fun OfflineSaleWithDetails.toDomainSale(): Sale {
    return Sale(
        saleId = UUID.fromString(sale.localSaleId),
        userId = UUID.fromString(sale.userId),
        saleDate = LocalDateTime.now(),
        subtotal = sale.subtotal,
        itbis = sale.itbis,
        total = sale.total,
        paymentMethod = sale.paymentMethod,
        status = sale.status,
        createdAt = LocalDateTime.now(),
        globalDiscount = 0.0,
        cashRegisterHistoryId = sale.cashRegisterHistoryId,
        saleDetails = details.map {
            SaleDetail(
                saleDetailId = 0,
                saleId = sale.localSaleId,
                productId = it.productId,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                discount = it.discount,
                finalPrice = it.finalPrice,
                createdAt = LocalDateTime.now()
            )
        }
    )
}


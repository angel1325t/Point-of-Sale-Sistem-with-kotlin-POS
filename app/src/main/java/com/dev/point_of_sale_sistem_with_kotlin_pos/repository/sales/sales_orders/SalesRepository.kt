package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleCreatedDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetailInsertDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleInsertDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

@Serializable
data class ProductStockUpdate(
    val stock: Int
)

class SalesRepository(
    private val supabase: SupabaseClient,
) {
    companion object {
        private const val TAG = "SalesRepository"
    }

    /**
     * Crea una venta completa en Supabase con todos sus detalles
     * y actualiza el inventario de productos
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createSale(sale: Sale): Result<SaleCreatedDTO> = runCatching {

        Log.d(TAG, "Creating sale with ${sale.saleDetails.size} items")

        // 🔐 VALIDAR QUE TENGA cash_register_history_id
        if (sale.cashRegisterHistoryId.isBlank()) {
            throw IllegalStateException("No se puede crear venta sin caja abierta")
        }

        // 1️⃣ VALIDAR STOCK ANTES DE CREAR LA VENTA
        for (detail in sale.saleDetails) {
            val currentStock = getCurrentStock(detail.productId)
            if (currentStock < detail.quantity) {
                throw IllegalStateException(
                    "Stock insuficiente para producto ${detail.productId}. " +
                            "Disponible: $currentStock, Requerido: ${detail.quantity}"
                )
            }
        }

        // 2️⃣ CREAR LA VENTA CON cash_register_history_id
        val saleDTO = SaleInsertDTO(
            userId = sale.userId.toString(),
            saleDate = sale.saleDate.toString(),
            subtotal = sale.subtotal,
            itbis = sale.itbis,
            total = sale.total,
            paymentMethod = sale.paymentMethod,
            status = sale.status,
            globalDiscount = sale.globalDiscount,
            isCreditNote = sale.isCreditNote,
            originalSaleId = sale.originalSaleId?.toString(),
            creditRemaining = sale.creditRemaining,
            cashRegisterHistoryId = sale.cashRegisterHistoryId,
            localId = null // Supabase generará su propio UUID
        )

        Log.d(TAG, "Inserting sale - Cash Register History ID: ${sale.cashRegisterHistoryId}")
        Log.d(TAG, "Sale totals - Subtotal: ${sale.subtotal}, ITBIS: ${sale.itbis}, Total: ${sale.total}")

        val createdSale = supabase.from("sales")
            .insert(saleDTO) {
                select(
                    Columns.list(
                        "sale_id",
                        "invoice_number"
                    )
                )
            }
            .decodeSingle<SaleCreatedDTO>()

        Log.d(TAG, "Sale created: ${createdSale.saleId} - Invoice: ${createdSale.invoiceNumber}")

        // 3️⃣ CREAR LOS DETALLES
        val detailsDTO = sale.saleDetails.map {
            SaleDetailInsertDTO(
                saleId = createdSale.saleId,
                productId = it.productId,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                discount = it.discount,
                finalPrice = it.finalPrice
            )
        }

        supabase.from("sale_details").insert(detailsDTO)
        Log.d(TAG, "Sale details inserted: ${detailsDTO.size} items")

        // 4️⃣ ACTUALIZAR STOCK DE CADA PRODUCTO
        for (detail in sale.saleDetails) {
            updateProductStock(detail.productId, -detail.quantity)
        }

        Log.d(TAG, "Stock updated for all products")

        createdSale
    }

    /**
     * Obtiene el stock actual de un producto
     */
    private suspend fun getCurrentStock(productId: Int): Int {
        return try {
            @Serializable
            data class StockResponse(val current_stock: Int)

            val response = supabase.from("products")
                .select(Columns.list("current_stock")) {
                    filter {
                        eq("product_id", productId)
                    }
                }
                .decodeSingle<StockResponse>()

            response.current_stock
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stock for product $productId", e)
            throw e
        }
    }

    /**
     * Actualiza el stock de un producto (suma o resta)
     * @param productId ID del producto
     * @param quantityChange Cantidad a sumar (+) o restar (-)
     */
    private suspend fun updateProductStock(productId: Int, quantityChange: Int) {
        try {
            val currentStock = getCurrentStock(productId)
            val newStock = currentStock + quantityChange

            if (newStock < 0) {
                throw IllegalStateException("El stock no puede ser negativo para producto $productId")
            }

            supabase.from("products")
                .update(
                    {
                        set("current_stock", newStock)
                    }
                ) {
                    filter {
                        eq("product_id", productId)
                    }
                }

            Log.d(TAG, "Product $productId stock updated: $currentStock -> $newStock (${if (quantityChange > 0) "+" else ""}$quantityChange)")

        } catch (e: Exception) {
            Log.e(TAG, "Error updating stock for product $productId", e)
            throw e
        }
    }

    /**
     * Revierte el stock de una venta (útil para devoluciones)
     */
    suspend fun revertSaleStock(saleId: String): Result<Unit> = runCatching {
        @Serializable
        data class SaleDetailResponse(
            val product_id: Int,
            val quantity: Int
        )

        // Obtener detalles de la venta
        val details = supabase.from("sale_details")
            .select(Columns.list("product_id", "quantity")) {
                filter {
                    eq("sale_id", saleId)
                }
            }
            .decodeList<SaleDetailResponse>()

        // Revertir stock (sumar las cantidades)
        for (detail in details) {
            updateProductStock(detail.product_id, detail.quantity)
        }

        Log.d(TAG, "Stock reverted for sale: $saleId")
    }
}
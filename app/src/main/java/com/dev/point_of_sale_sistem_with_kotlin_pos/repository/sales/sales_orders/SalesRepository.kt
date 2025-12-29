package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetailInsertDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleInsertDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.format.DateTimeFormatter

class SalesRepository(
    private val supabase: SupabaseClient,
    private val productRepository: SalesProductRepository
) {
    companion object {
        private const val TAG = "SalesRepository"
    }

    /**
     * Crea una venta completa en Supabase con todos sus detalles
     * y actualiza el inventario de productos
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createSale(sale: Sale): Result<String> {
        return try {
            Log.d(TAG, "Creating sale: ${sale.saleId}")

            // 1. Insertar la venta principal
            val saleDTO = SaleInsertDTO(
                saleId = sale.saleId.toString(),
                userId = sale.userId.toString(),
                saleDate = sale.saleDate.format(DateTimeFormatter.ISO_DATE_TIME),
                total = sale.total,
                paymentMethod = sale.paymentMethod,
                status = sale.status,
                globalDiscount = sale.globalDiscount
            )

            supabase.from("sales")
                .insert(saleDTO)

            Log.d(TAG, "Sale created successfully")

            // 2. Insertar los detalles de venta
            if (sale.saleDetails.isNotEmpty()) {
                val detailsDTO = sale.saleDetails.map { detail ->
                    SaleDetailInsertDTO(
                        saleId = sale.saleId.toString(),
                        productId = detail.productId,
                        quantity = detail.quantity,
                        unitPrice = detail.unitPrice,
                        discount = detail.discount,
                        finalPrice = detail.finalPrice
                    )
                }

                supabase.from("sale_details")
                    .insert(detailsDTO)

                Log.d(TAG, "Sale details created: ${detailsDTO.size} items")

                // 3. Reducir stock de cada producto
                sale.saleDetails.forEach { detail ->
                    productRepository.reduceStock(
                        productId = detail.productId,
                        quantity = detail.quantity
                    ).onFailure { e ->
                        Log.e(TAG, "Warning: Could not reduce stock for product ${detail.productId}", e)
                        // No falla toda la venta por esto, solo registra el error
                    }
                }
            }

            Result.success(sale.saleId.toString())

        } catch (e: Exception) {
            Log.e(TAG, "Error creating sale", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene una venta completa con sus detalles
     */
    suspend fun getSaleById(saleId: String): Result<Sale?> {
        return try {
            // Implementar si necesitas recuperar ventas
            Result.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sale", e)
            Result.failure(e)
        }
    }
}
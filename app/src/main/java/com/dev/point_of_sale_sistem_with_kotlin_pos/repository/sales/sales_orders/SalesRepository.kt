package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetailInsertDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleInsertDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.utils.BusinessInfo
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.InvoiceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.io.File
import java.time.format.DateTimeFormatter

class SalesRepository(
    private val supabase: SupabaseClient,
    private val productRepository: SalesProductRepository,
    private val context: Context // 🔥 NUEVO: Para facturación
) {
    companion object {
        private const val TAG = "SalesRepository"
    }

    // 🔥 NUEVO: Repository de facturación
    private val invoiceRepository by lazy {
        InvoiceRepository(
            context = context,
            businessInfo = BusinessInfo(
                name = "Mi Empresa POS",                       // 🔥 CAMBIAR por tu nombre
                address = "Av. Principal #123, Santo Domingo", // 🔥 CAMBIAR por tu dirección
                phone = "(809) 555-1234",                      // 🔥 CAMBIAR por tu teléfono
                email = "ventas@miempresa.com",                // 🔥 CAMBIAR por tu email
                taxId = "123-4567890-1"                        // 🔥 CAMBIAR por tu RNC
            )
        )
    }

    /**
     * Crea una venta completa en Supabase con todos sus detalles
     * y actualiza el inventario de productos
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createSale(sale: Sale): Result<SaleResult> {
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
                    }
                }
            }

            Result.success(SaleResult(saleId = sale.saleId.toString()))

        } catch (e: Exception) {
            Log.e(TAG, "Error creating sale", e)
            Result.failure(e)
        }
    }

    /**
     * 🔥 NUEVO: Genera e imprime la factura de una venta
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    suspend fun generateAndPrintInvoice(
        sale: Sale,
        productsCache: Map<Int, ProductDTO>
    ): Result<File>
    {
        return try {
            Log.d(TAG, "Generating invoice for sale: ${sale.saleId}")

            // Crear mapa de productos para la factura
            val productsMap = productsCache.mapKeys { it.key.toString() }
                .mapValues { it.value.name }

            // Generar e imprimir factura
            invoiceRepository.generateAndPrintInvoice(
                sale = sale,
                productsMap = productsMap
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating invoice", e)
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

// 🔥 NUEVO: Resultado de crear venta
data class SaleResult(
    val saleId: String,
    val invoiceFile: File? = null
)
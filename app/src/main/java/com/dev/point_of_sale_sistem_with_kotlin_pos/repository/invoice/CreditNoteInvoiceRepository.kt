package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.BusinessInfo
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds.RefundableSale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetail
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.utils.CreditNoteInvoiceGenerator
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.utils.InvoicePrinter
import java.io.File
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * Repositorio coordinador de facturación para Notas de Crédito
 * Orquesta la generación e impresión de facturas de devolución
 */
class CreditNoteInvoiceRepository(
    private val context: Context,
    private val businessInfo: BusinessInfo,
    private val activityRef: WeakReference<Activity>? = null
) {
    companion object {
        private const val TAG = "CreditNoteInvoiceRepo"
    }

    constructor(
        context: Context,
        businessInfo: BusinessInfo,
        activity: Activity
    ) : this(context, businessInfo, WeakReference(activity))

    private val invoiceGenerator = CreditNoteInvoiceGenerator(context)
    private val invoicePrinter by lazy {
        activityRef?.get()?.let { InvoicePrinter(it) }
    }

    /**
     * Proceso completo: Genera la factura de nota de crédito y abre el diálogo de impresión
     *
     * @param sale Nota de crédito (RefundableSale con isCreditNote = true)
     * @param productsMap Mapa de productId -> nombre del producto
     * @return Result con el File del PDF generado
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    suspend fun generateAndPrintCreditNoteInvoice(
        sale: RefundableSale,
        productsMap: Map<String, String>
    ): Result<File> = runCatching {

        val invoiceNumber = sale.invoiceNumber

        if (!sale.isCreditNote) {
            throw IllegalArgumentException("La venta proporcionada no es una nota de crédito")
        }

        Log.d(TAG, "Starting credit note invoice generation and print for: $invoiceNumber")

        // 1. Convertir RefundableSale a Sale para el generador
        val saleForInvoice = convertToSale(sale)

        // 2. Generar PDF
        val pdfFile = invoiceGenerator.generateCreditNoteInvoice(
            sale = saleForInvoice,
            productsMap = productsMap,
            businessInfo = businessInfo,
            invoiceNumber = invoiceNumber
        ).getOrThrow()

        Log.d(TAG, "Credit note invoice PDF generated: ${pdfFile.absolutePath}")

        // 3. Imprimir
        invoicePrinter?.printInvoice(
            pdfFile = pdfFile,
            jobName = "Nota de Crédito #$invoiceNumber"
        ) ?: Log.w(TAG, "Activity not available, cannot print")

        pdfFile
    }

    /**
     * Convierte RefundableSale a Sale para el generador de facturas
     */
    private fun convertToSale(refundableSale: RefundableSale): Sale {
        return Sale(
            saleId = refundableSale.saleId,
            userId = UUID.randomUUID(), // No se usa en la factura
            saleDate = refundableSale.saleDate,
            createdAt = refundableSale.saleDate,
            total = refundableSale.total,
            paymentMethod = refundableSale.paymentMethod,
            status = "completed",
            globalDiscount = 0.0,
            invoiceNumber = refundableSale.invoiceNumber,
            isCreditNote = refundableSale.isCreditNote,
            originalSaleId = refundableSale.originalSaleId,
            creditRemaining = refundableSale.creditRemaining ?: 0.0,
            subtotal = refundableSale.total / 1.18, // Calcular subtotal sin ITBIS
            itbis = refundableSale.total - (refundableSale.total / 1.18), // Calcular ITBIS
            saleDetails = refundableSale.details.map { detail ->
                SaleDetail(
                    saleDetailId = detail.saleDetailId,
                    saleId = refundableSale.saleId.toString(),
                    productId = detail.productId,
                    quantity = detail.quantity,
                    unitPrice = detail.unitPrice,
                    discount = detail.discount,
                    finalPrice = detail.finalPrice,
                    createdAt = refundableSale.saleDate,
                )
            }
        )
    }
}
package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.utils.BusinessInfo
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.utils.InvoiceGenerator
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.utils.InvoicePrinter
import java.io.File
import java.lang.ref.WeakReference

/**
 * Repositorio coordinador de facturación
 * Orquesta la generación e impresión de facturas
 */
class InvoiceRepository(
    private val context: Context,
    private val businessInfo: BusinessInfo,
    private val activityRef: WeakReference<Activity>? = null // 🔥 NUEVO: Activity opcional
) {
    companion object {
        private const val TAG = "InvoiceRepository"
    }

    // 🔥 NUEVO: Constructor adicional que acepta Activity directamente
    constructor(
        context: Context,
        businessInfo: BusinessInfo,
        activity: Activity
    ) : this(context, businessInfo, WeakReference(activity))

    private val invoiceGenerator = InvoiceGenerator(context)
    private val invoicePrinter by lazy {
        activityRef?.get()?.let { InvoicePrinter(it) }
    }

    /**
     * Proceso completo: Genera la factura y abre el diálogo de impresión
     *
     * @param sale Venta completada
     * @param productsMap Mapa de productId -> nombre del producto
     * @return Result con el File del PDF generado (por si se necesita después)
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    suspend fun generateAndPrintInvoice(
        sale: Sale,
        productsMap: Map<String, String>
    ): Result<File> = runCatching {
        Log.d(TAG, "Starting invoice generation and print for sale: ${sale.saleId}")

        // 1. Generar PDF
        val pdfFile = invoiceGenerator.generateInvoice(
            sale = sale,
            productsMap = productsMap,
            businessInfo = businessInfo
        ).getOrThrow()

        Log.d(TAG, "Invoice PDF generated: ${pdfFile.absolutePath}")

        // 2. Abrir diálogo de impresión (si hay Activity disponible)
        val printer = invoicePrinter
        if (printer != null) {
            val invoiceNumber = sale.saleId.toString().takeLast(8).uppercase()
            printer.printInvoice(
                pdfFile = pdfFile,
                jobName = "Factura #$invoiceNumber"
            )
            Log.d(TAG, "Print dialog opened successfully")
        } else {
            Log.w(
                TAG,
                "Cannot open print dialog: Activity not available. PDF saved at: ${pdfFile.absolutePath}"
            )
        }

        pdfFile
    }

    /**
     * Solo genera la factura sin imprimir
     * Útil si solo necesitas el PDF
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun generateInvoiceOnly(
        sale: Sale,
        productsMap: Map<String, String>
    ): Result<File> = runCatching {
        Log.d(TAG, "Generating invoice only for sale: ${sale.saleId}")

        invoiceGenerator.generateInvoice(
            sale = sale,
            productsMap = productsMap,
            businessInfo = businessInfo
        ).getOrThrow()
    }

    /**
     * Solo imprime un PDF ya generado
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun printExistingInvoice(pdfFile: File, invoiceNumber: String) {
        Log.d(TAG, "Printing existing invoice: $invoiceNumber")
        invoicePrinter?.printInvoice(pdfFile, "Factura #$invoiceNumber")
            ?: Log.w(TAG, "Cannot print: Activity not available")
    }
}
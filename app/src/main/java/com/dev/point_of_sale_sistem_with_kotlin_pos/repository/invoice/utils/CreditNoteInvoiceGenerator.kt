package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.BusinessInfo
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

/**
 * Generador de facturas para Notas de Crédito
 * Usa un diseño diferente al de ventas normales
 */
class CreditNoteInvoiceGenerator(
    private val context: Context
) {
    companion object {
        private const val TAG = "CreditNoteInvGenerator"
        private const val PAGE_WIDTH = 595  // A4
        private const val PAGE_HEIGHT = 842 // A4
        private const val MARGIN = 40
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun generateCreditNoteInvoice(
        sale: Sale,
        productsMap: Map<String, String>,
        businessInfo: BusinessInfo,
        invoiceNumber: String
    ): Result<File> = runCatching {

        Log.d(TAG, "Generating credit note invoice: $invoiceNumber")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var yPosition = MARGIN.toFloat()

        // ═══════════════════════════════════════════
        // ENCABEZADO - NOTA DE CRÉDITO
        // ═══════════════════════════════════════════
        yPosition = drawCreditNoteHeader(canvas, businessInfo, invoiceNumber, yPosition)

        // ═══════════════════════════════════════════
        // INFORMACIÓN DE LA DEVOLUCIÓN
        // ═══════════════════════════════════════════
        yPosition = drawCreditNoteInfo(canvas, sale, yPosition)

        // ═══════════════════════════════════════════
        // PRODUCTOS DEVUELTOS
        // ═══════════════════════════════════════════
        yPosition = drawRefundedProducts(canvas, sale, productsMap, yPosition)

        // ═══════════════════════════════════════════
        // TOTALES
        // ═══════════════════════════════════════════
        yPosition = drawCreditNoteTotals(canvas, sale, yPosition)

        // ═══════════════════════════════════════════
        // QR CODE CON INVOICE NUMBER
        // ═══════════════════════════════════════════
        drawQRCode(canvas, invoiceNumber, yPosition)

        // ═══════════════════════════════════════════
        // PIE DE PÁGINA
        // ═══════════════════════════════════════════
        drawCreditNoteFooter(canvas)

        document.finishPage(page)

        // Guardar PDF
        val fileName = "credit_note_$invoiceNumber.pdf"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { output ->
            document.writeTo(output)
        }
        document.close()

        Log.d(TAG, "Credit note invoice generated: ${file.absolutePath}")
        file
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun drawCreditNoteHeader(
        canvas: Canvas,
        businessInfo: BusinessInfo,
        invoiceNumber: String,
        startY: Float
    ): Float {
        var y = startY

        val titlePaint = Paint().apply {
            color = Color.rgb(220, 53, 69) // Color rojo para nota de crédito
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            "NOTA DE CRÉDITO",
            PAGE_WIDTH / 2f,
            y,
            titlePaint
        )
        y += 40f

        val businessPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(businessInfo.name, PAGE_WIDTH / 2f, y, businessPaint)
        y += 25f

        val infoPaint = Paint().apply {
            color = Color.GRAY
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }

        businessInfo.taxId?.let {
            canvas.drawText("RNC: $it", PAGE_WIDTH / 2f, y, infoPaint)
            y += 20f
        }

        businessInfo.address?.let {
            canvas.drawText(it, PAGE_WIDTH / 2f, y, infoPaint)
            y += 20f
        }

        businessInfo.phone?.let {
            canvas.drawText("Tel: $it", PAGE_WIDTH / 2f, y, infoPaint)
            y += 30f
        }

        // Línea separadora
        val linePaint = Paint().apply {
            color = Color.rgb(220, 53, 69)
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, linePaint)
        y += 30f

        // Número de factura
        val invoicePaint = Paint().apply {
            color = Color.rgb(220, 53, 69)
            textSize = 18f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("No. $invoiceNumber", PAGE_WIDTH / 2f, y, invoicePaint)
        y += 35f

        return y
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun drawCreditNoteInfo(
        canvas: Canvas,
        sale: Sale,
        startY: Float
    ): Float {
        var y = startY

        val labelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isFakeBoldText = true
        }

        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        // Fecha de emisión
        canvas.drawText("Fecha de emisión:", MARGIN.toFloat(), y, labelPaint)
        canvas.drawText(
            sale.saleDate.format(dateFormatter),
            300f,
            y,
            valuePaint
        )
        y += 25f

        // Venta original (si existe)
        sale.originalSaleId?.let { originalId ->
            canvas.drawText("Venta original:", MARGIN.toFloat(), y, labelPaint)
            canvas.drawText(
                originalId.toString().take(8) + "...",
                300f,
                y,
                valuePaint
            )
            y += 25f
        }

        // Método de devolución
        canvas.drawText("Método de devolución:", MARGIN.toFloat(), y, labelPaint)
        canvas.drawText(
            when (sale.paymentMethod) {
                "cash" -> "Efectivo"
                "card" -> "Tarjeta"
                "credit_note" -> "Crédito"
                else -> sale.paymentMethod
            },
            300f,
            y,
            valuePaint
        )
        y += 35f

        return y
    }

    private fun drawRefundedProducts(
        canvas: Canvas,
        sale: Sale,
        productsMap: Map<String, String>,
        startY: Float
    ): Float {
        var y = startY

        // Título de sección
        val sectionPaint = Paint().apply {
            color = Color.rgb(220, 53, 69)
            textSize = 14f
            isFakeBoldText = true
        }

        canvas.drawText("PRODUCTOS DEVUELTOS", MARGIN.toFloat(), y, sectionPaint)
        y += 30f

        // Cabecera de tabla
        val headerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(220, 53, 69)
            style = Paint.Style.FILL
        }

        canvas.drawRect(
            MARGIN.toFloat(),
            y - 18f,
            (PAGE_WIDTH - MARGIN).toFloat(),
            y + 5f,
            headerBgPaint
        )

        canvas.drawText("Producto", MARGIN + 5f, y, headerPaint)
        canvas.drawText("Cant.", 350f, y, headerPaint)
        canvas.drawText("Precio", 420f, y, headerPaint)
        canvas.drawText("Total", 500f, y, headerPaint)

        y += 25f

        // Detalles de productos
        val productPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        sale.saleDetails.forEach { detail ->
            val productName = productsMap[detail.productId.toString()] ?: "Producto ${detail.productId}"

            // Nombre del producto (truncado si es muy largo)
            val truncatedName = if (productName.length > 35) {
                productName.take(32) + "..."
            } else {
                productName
            }

            canvas.drawText(truncatedName, MARGIN + 5f, y, productPaint)
            canvas.drawText(detail.quantity.toString(), 350f, y, productPaint)
            canvas.drawText("$${String.format("%.2f", detail.unitPrice)}", 420f, y, productPaint)
            canvas.drawText("$${String.format("%.2f", detail.finalPrice)}", 500f, y, productPaint)

            y += 22f
        }

        y += 15f
        return y
    }

    private fun drawCreditNoteTotals(
        canvas: Canvas,
        sale: Sale,
        startY: Float
    ): Float {
        var y = startY

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        canvas.drawLine(350f, y, (PAGE_WIDTH - MARGIN).toFloat(), y, linePaint)
        y += 20f

        val labelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }

        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }

        // Subtotal
        canvas.drawText("Subtotal:", 450f, y, labelPaint)
        canvas.drawText(
            "$${String.format("%.2f", sale.subtotal)}",
            (PAGE_WIDTH - MARGIN - 10).toFloat(),
            y,
            valuePaint
        )
        y += 25f

        // ITBIS
        canvas.drawText("ITBIS (18%):", 450f, y, labelPaint)
        canvas.drawText(
            "$${String.format("%.2f", sale.itbis)}",
            (PAGE_WIDTH - MARGIN - 10).toFloat(),
            y,
            valuePaint
        )
        y += 25f

        // Total a devolver
        val totalPaint = Paint().apply {
            color = Color.rgb(220, 53, 69)
            textSize = 16f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("TOTAL A DEVOLVER:", 450f, y, totalPaint)
        canvas.drawText(
            "$${String.format("%.2f", sale.total)}",
            (PAGE_WIDTH - MARGIN - 10).toFloat(),
            y,
            totalPaint
        )
        y += 30f

        // Crédito disponible
        val creditPaint = Paint().apply {
            color = Color.rgb(40, 167, 69) // Verde
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("Crédito disponible:", 450f, y, creditPaint)
        canvas.drawText(
            "$${String.format("%.2f", sale.creditRemaining ?: 0.0)}",
            (PAGE_WIDTH - MARGIN - 10).toFloat(),
            y,
            creditPaint
        )
        y += 40f

        return y
    }

    private fun drawQRCode(
        canvas: Canvas,
        invoiceNumber: String,
        startY: Float
    ) {
        try {
            val qrSize = 150
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(
                invoiceNumber,
                BarcodeFormat.QR_CODE,
                qrSize,
                qrSize
            )

            val bitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.RGB_565)
            for (x in 0 until qrSize) {
                for (y in 0 until qrSize) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }

            val qrX = (PAGE_WIDTH - qrSize) / 2f
            val qrY = startY

            canvas.drawBitmap(bitmap, qrX, qrY, null)

            // Texto debajo del QR
            val qrTextPaint = Paint().apply {
                color = Color.GRAY
                textSize = 10f
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText(
                "Escanea para verificar",
                PAGE_WIDTH / 2f,
                qrY + qrSize + 20f,
                qrTextPaint
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error generating QR code", e)
        }
    }

    private fun drawCreditNoteFooter(canvas: Canvas) {
        val footerY = PAGE_HEIGHT - 50f

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            "Esta nota de crédito puede ser aplicada a futuras compras",
            PAGE_WIDTH / 2f,
            footerY,
            footerPaint
        )

        canvas.drawText(
            "Válida por 90 días desde la fecha de emisión",
            PAGE_WIDTH / 2f,
            footerY + 15f,
            footerPaint
        )
    }
}
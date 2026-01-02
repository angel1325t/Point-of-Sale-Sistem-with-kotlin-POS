package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.utils


import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetail
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

/**
 * Generador de facturas en PDF
 * Crea PDFs profesionales con información completa de la venta
 */
class InvoiceGenerator(private val context: Context) {

    companion object {
        private const val TAG = "InvoiceGenerator"

        // Dimensiones página A4 en puntos (72 DPI)
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842

        // Márgenes
        private const val MARGIN_LEFT = 40f
        private const val MARGIN_RIGHT = 40f
        private const val MARGIN_TOP = 40f

        // Colores
        private const val COLOR_PRIMARY = 0xFF2196F3.toInt()
        private const val COLOR_SECONDARY = 0xFF757575.toInt()
        private const val COLOR_TEXT = 0xFF212121.toInt()
        private const val COLOR_LINE = 0xFFE0E0E0.toInt()
    }

    /**
     * Genera la factura en PDF y la guarda en caché
     * @return File del PDF generado
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun generateInvoice(
        sale: Sale,
        productsMap: Map<String, String>, // productId -> productName
        businessInfo: BusinessInfo
    ): Result<File> = runCatching {
        Log.d(TAG, "Generating invoice for sale: ${sale.saleId}")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var yPosition = MARGIN_TOP

        // ═══════════════════════════════════════════════════
        // 📋 ENCABEZADO DE LA EMPRESA
        // ═══════════════════════════════════════════════════
        val titlePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText(businessInfo.name, MARGIN_LEFT, yPosition, titlePaint)
        yPosition += 30f

        val infoPaint = Paint().apply {
            color = COLOR_SECONDARY
            textSize = 10f
            isAntiAlias = true
        }

        canvas.drawText(businessInfo.address, MARGIN_LEFT, yPosition, infoPaint)
        yPosition += 15f
        canvas.drawText("Tel: ${businessInfo.phone}", MARGIN_LEFT, yPosition, infoPaint)
        yPosition += 15f
        canvas.drawText("RNC: ${businessInfo.taxId}", MARGIN_LEFT, yPosition, infoPaint)
        yPosition += 30f

        // ═══════════════════════════════════════════════════
        // 🧾 INFORMACIÓN DE LA FACTURA
        // ═══════════════════════════════════════════════════
        val invoiceTitlePaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("FACTURA", PAGE_WIDTH - 150f, MARGIN_TOP, invoiceTitlePaint)

        val invoiceNumberPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 12f
            isAntiAlias = true
        }

        val saleIdShort = sale.saleId.toString().takeLast(8).uppercase()
        canvas.drawText("No. $saleIdShort", PAGE_WIDTH - 150f, MARGIN_TOP + 25f, invoiceNumberPaint)

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        canvas.drawText(
            "Fecha: ${sale.saleDate.format(dateFormatter)}",
            PAGE_WIDTH - 150f,
            MARGIN_TOP + 40f,
            invoiceNumberPaint
        )

        // Línea separadora
        val linePaint = Paint().apply {
            color = COLOR_LINE
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN_LEFT, yPosition, PAGE_WIDTH - MARGIN_RIGHT, yPosition, linePaint)
        yPosition += 20f

        // ═══════════════════════════════════════════════════
        // 📦 TABLA DE PRODUCTOS
        // ═══════════════════════════════════════════════════
        val headerPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Headers de la tabla
        canvas.drawText("Producto", MARGIN_LEFT, yPosition, headerPaint)
        canvas.drawText("Cant.", PAGE_WIDTH - 280f, yPosition, headerPaint)
        canvas.drawText("Precio", PAGE_WIDTH - 220f, yPosition, headerPaint)
        canvas.drawText("Desc.", PAGE_WIDTH - 160f, yPosition, headerPaint)
        canvas.drawText("Total", PAGE_WIDTH - 100f, yPosition, headerPaint)
        yPosition += 5f

        canvas.drawLine(MARGIN_LEFT, yPosition, PAGE_WIDTH - MARGIN_RIGHT, yPosition, linePaint)
        yPosition += 15f

        // Items de la venta
        val itemPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 10f
            isAntiAlias = true
        }

        val itemBoldPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        sale.saleDetails.forEach { detail ->
            val productName = productsMap[detail.productId.toString()]
                ?: "Producto Desconocido"


            // Nombre del producto (con wrap si es muy largo)
            val maxProductNameWidth = PAGE_WIDTH - 350f
            val wrappedName = wrapText(productName, maxProductNameWidth, itemPaint)

            wrappedName.forEachIndexed { index, line ->
                canvas.drawText(line, MARGIN_LEFT, yPosition, itemPaint)
                if (index < wrappedName.size - 1) yPosition += 12f
            }

            // Datos numéricos
            canvas.drawText(detail.quantity.toString(), PAGE_WIDTH - 280f, yPosition, itemPaint)
            canvas.drawText("$${String.format("%.2f", detail.unitPrice)}", PAGE_WIDTH - 220f, yPosition, itemPaint)

            val discountText = if (detail.discount > 0) "$${String.format("%.2f", detail.discount)}" else "-"
            canvas.drawText(discountText, PAGE_WIDTH - 160f, yPosition, itemPaint)
            canvas.drawText("$${String.format("%.2f", detail.finalPrice)}", PAGE_WIDTH - 100f, yPosition, itemBoldPaint)

            yPosition += 20f
        }

        yPosition += 10f
        canvas.drawLine(MARGIN_LEFT, yPosition, PAGE_WIDTH - MARGIN_RIGHT, yPosition, linePaint)
        yPosition += 20f

        // ═══════════════════════════════════════════════════
        // 💰 TOTALES
        // ═══════════════════════════════════════════════════
        val totalPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Subtotal (antes de descuento global)
        val subtotal = sale.saleDetails.sumOf { it.unitPrice * it.quantity }
        canvas.drawText("Subtotal:", PAGE_WIDTH - 200f, yPosition, itemPaint)
        canvas.drawText("$${String.format("%.2f", subtotal)}", PAGE_WIDTH - 100f, yPosition, itemPaint)
        yPosition += 18f

        // Descuento Global
        if (sale.globalDiscount > 0) {
            canvas.drawText("Descuento Global:", PAGE_WIDTH - 200f, yPosition, itemPaint)
            canvas.drawText("-$${String.format("%.2f", sale.globalDiscount)}", PAGE_WIDTH - 100f, yPosition, itemPaint)
            yPosition += 18f
        }

        // TOTAL FINAL
        canvas.drawText("TOTAL:", PAGE_WIDTH - 200f, yPosition, totalPaint)
        canvas.drawText("$${String.format("%.2f", sale.total)}", PAGE_WIDTH - 100f, yPosition, totalPaint)
        yPosition += 30f

        // ═══════════════════════════════════════════════════
        // 💳 MÉTODO DE PAGO
        // ═══════════════════════════════════════════════════
        val paymentMethodText = when (sale.paymentMethod) {
            "cash" -> "Efectivo"
            "card" -> "Tarjeta de Crédito/Débito"
            "transfer" -> "Transferencia Bancaria"
            else -> sale.paymentMethod
        }

        canvas.drawText("Método de pago: $paymentMethodText", MARGIN_LEFT, yPosition, itemPaint)
        yPosition += 30f

        // ═══════════════════════════════════════════════════
        // 📝 FOOTER
        // ═══════════════════════════════════════════════════
        val footerPaint = Paint().apply {
            color = COLOR_SECONDARY
            textSize = 9f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val footerY = PAGE_HEIGHT - 60f
        canvas.drawText("¡Gracias por su compra!", PAGE_WIDTH / 2f, footerY, footerPaint)
        canvas.drawText(businessInfo.email, PAGE_WIDTH / 2f, footerY + 15f, footerPaint)

        document.finishPage(page)

        // ═══════════════════════════════════════════════════
        // 💾 GUARDAR EN CACHÉ
        // ═══════════════════════════════════════════════════
        val cacheDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val fileName = "invoice_${sale.saleId}.pdf"
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { outputStream ->
            document.writeTo(outputStream)
        }
        document.close()

        Log.d(TAG, "Invoice generated: ${file.absolutePath}")
        file
    }

    /**
     * Divide texto largo en múltiples líneas
     */
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)

            if (width > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}

/**
 * Información del negocio para la factura
 */
data class BusinessInfo(
    val name: String,
    val address: String,
    val phone: String,
    val email: String,
    val taxId: String // RNC o Tax ID
)
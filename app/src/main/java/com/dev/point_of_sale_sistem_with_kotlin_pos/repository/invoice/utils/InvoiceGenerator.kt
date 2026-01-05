package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.BusinessInfo
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

class InvoiceGenerator(private val context: Context) {

    companion object {
        private const val TAG = "InvoiceGenerator"

        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842

        private const val MARGIN_LEFT = 40f
        private const val MARGIN_RIGHT = 40f
        private const val MARGIN_TOP = 40f

        private const val COLOR_PRIMARY = 0xFF2196F3.toInt()
        private const val COLOR_SECONDARY = 0xFF757575.toInt()
        private const val COLOR_TEXT = 0xFF212121.toInt()
        private const val COLOR_LINE = 0xFFE0E0E0.toInt()
        private const val COLOR_HEADER_BG = 0xFFF5F5F5.toInt()

        private const val QR_SIZE = 100f
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun generateInvoice(
        sale: Sale,
        productsMap: Map<String, String>,
        businessInfo: BusinessInfo,
        invoiceNumber: String
    ): Result<File> = runCatching {

        val document = PdfDocument()
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        )
        val canvas = page.canvas

        var y = MARGIN_TOP

        /* ================= HEADER SECTION ================= */

        // Business name with larger font
        val titlePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(businessInfo.name, MARGIN_LEFT, y, titlePaint)
        y += 35f

        // Business information
        val infoPaint = Paint().apply {
            color = COLOR_SECONDARY
            textSize = 10f
            isAntiAlias = true
        }

        fun drawIfNotNull(text: String?) {
            text?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText(it, MARGIN_LEFT, y, infoPaint)
                y += 14f
            }
        }

        drawIfNotNull(businessInfo.address)
        drawIfNotNull(businessInfo.city)
        drawIfNotNull(businessInfo.phone?.let { "Tel: $it" })
        drawIfNotNull(businessInfo.email?.let { "Email: $it" })
        drawIfNotNull(businessInfo.taxId?.let { "RNC: $it" })

        /* ================= INVOICE INFO BOX ================= */

        val boxX = PAGE_WIDTH - MARGIN_RIGHT - 200f
        val boxY = MARGIN_TOP
        val boxWidth = 200f
        val boxHeight = 90f

        // Draw box background
        val boxPaint = Paint().apply {
            color = COLOR_HEADER_BG
            style = Paint.Style.FILL
        }
        val rect = RectF(boxX, boxY, boxX + boxWidth, boxY + boxHeight)
        canvas.drawRoundRect(rect, 8f, 8f, boxPaint)

        // Draw box border
        val borderPaint = Paint().apply {
            color = COLOR_PRIMARY
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        // Invoice text inside box
        var boxTextY = boxY + 25f

        val invoiceTitlePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FACTURA", boxX + boxWidth / 2, boxTextY, invoiceTitlePaint)
        boxTextY += 22f

        val invoiceDetailPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 11f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("No. $invoiceNumber", boxX + boxWidth / 2, boxTextY, invoiceDetailPaint)
        boxTextY += 18f

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        canvas.drawText(
            sale.saleDate.format(formatter),
            boxX + boxWidth / 2,
            boxTextY,
            invoiceDetailPaint
        )

        y = maxOf(y, boxY + boxHeight) + 30f

        /* ================= SEPARATOR LINE ================= */

        val linePaint = Paint().apply {
            color = COLOR_PRIMARY
            strokeWidth = 3f
        }
        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, linePaint)
        y += 25f

        /* ================= PRODUCTS TABLE ================= */

        // Table header background
        val headerBgPaint = Paint().apply {
            color = COLOR_HEADER_BG
            style = Paint.Style.FILL
        }
        val headerRect = RectF(
            MARGIN_LEFT,
            y - 5f,
            PAGE_WIDTH - MARGIN_RIGHT,
            y + 18f
        )
        canvas.drawRect(headerRect, headerBgPaint)

        // Table headers
        val headerPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val colProduct = MARGIN_LEFT + 5f
        val colQty = PAGE_WIDTH - 280f
        val colPrice = PAGE_WIDTH - 220f
        val colDisc = PAGE_WIDTH - 160f
        val colTotal = PAGE_WIDTH - 100f

        canvas.drawText("Producto", colProduct, y + 10f, headerPaint)
        canvas.drawText("Cant.", colQty, y + 10f, headerPaint)
        canvas.drawText("Precio", colPrice, y + 10f, headerPaint)
        canvas.drawText("Desc.", colDisc, y + 10f, headerPaint)
        canvas.drawText("Total", colTotal, y + 10f, headerPaint)

        y += 20f

        val thinLinePaint = Paint().apply {
            color = COLOR_LINE
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, thinLinePaint)
        y += 15f

        // Table items
        val itemPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 10f
            isAntiAlias = true
        }

        sale.saleDetails.forEach { d ->
            val name = productsMap[d.productId.toString()] ?: "Producto"
            val itemStartY = y

            // Product name with wrapping
            val wrappedLines = wrapText(name, PAGE_WIDTH - 350f, itemPaint)
            wrappedLines.forEach {
                canvas.drawText(it, colProduct, y, itemPaint)
                y += 13f
            }

            // Align other columns with the first line
            canvas.drawText(d.quantity.toString(), colQty, itemStartY, itemPaint)
            canvas.drawText("$${"%.2f".format(d.unitPrice)}", colPrice, itemStartY, itemPaint)
            canvas.drawText(
                if (d.discount > 0) "$${"%.2f".format(d.discount)}" else "-",
                colDisc,
                itemStartY,
                itemPaint
            )

            val totalPaint = Paint(itemPaint).apply {
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(
                "$${"%.2f".format(d.finalPrice)}",
                colTotal,
                itemStartY,
                totalPaint
            )

            y += 5f
        }

        /* ================= TOTALS SECTION ================= */

        y += 10f
        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, thinLinePaint)
        y += 20f

        val subtotal = sale.saleDetails.sumOf { it.unitPrice * it.quantity }
        val labelX = PAGE_WIDTH - 200f
        val valueX = PAGE_WIDTH - 80f

        val totalLabelPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 11f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val totalValuePaint = Paint(totalLabelPaint).apply {
            typeface = Typeface.DEFAULT_BOLD
        }

        canvas.drawText("Subtotal:", labelX, y, totalLabelPaint)
        canvas.drawText("$${"%.2f".format(subtotal)}", valueX, y, totalValuePaint)
        y += 16f

        if (sale.globalDiscount > 0) {
            canvas.drawText("Descuento:", labelX, y, totalLabelPaint)
            canvas.drawText("-$${"%.2f".format(sale.globalDiscount)}", valueX, y, totalValuePaint)
            y += 16f
        }

        // ITBIS (18%)
        canvas.drawText("ITBIS (18%):", labelX, y, totalLabelPaint)
        canvas.drawText("$${"%.2f".format(sale.itbis)}", valueX, y, totalValuePaint)
        y += 20f

        // Final total - sin recuadro azul, solo más grande y negrita
        val finalTotalPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("TOTAL:", labelX, y, finalTotalPaint)
        canvas.drawText("$${"%.2f".format(sale.total)}", valueX, y, finalTotalPaint)
        y += 35f

        /* ================= QR CODE SECTION ================= */

        y += 15f
        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, thinLinePaint)
        y += 20f

        generateQRCode(invoiceNumber, QR_SIZE.toInt())?.let { qrBitmap ->
            val qrX = MARGIN_LEFT
            canvas.drawBitmap(qrBitmap, qrX, y, null)

            // QR description
            val qrLabelPaint = Paint().apply {
                color = COLOR_SECONDARY
                textSize = 9f
                isAntiAlias = true
            }
            canvas.drawText("Escanea para verificar", qrX, y + QR_SIZE + 15f, qrLabelPaint)
            canvas.drawText("Factura: $invoiceNumber", qrX, y + QR_SIZE + 28f, qrLabelPaint)
        }

        /* ================= FOOTER ================= */

        val footerPaint = Paint().apply {
            color = COLOR_SECONDARY
            textSize = 10f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val footerY = PAGE_HEIGHT - 60f

        val thanksPaint = Paint(footerPaint).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            color = COLOR_PRIMARY
        }
        canvas.drawText("¡Gracias por su compra!", PAGE_WIDTH / 2f, footerY, thanksPaint)

        businessInfo.email?.let { email ->
            canvas.drawText(email, PAGE_WIDTH / 2f, footerY + 18f, footerPaint)
        }

        // Bottom line
        canvas.drawLine(
            MARGIN_LEFT,
            PAGE_HEIGHT - 35f,
            PAGE_WIDTH - MARGIN_RIGHT,
            PAGE_HEIGHT - 35f,
            thinLinePaint
        )

        document.finishPage(page)

        /* ================= SAVE PDF ================= */

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_${invoiceNumber.replace("/", "_")}.pdf")

        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        Log.d(TAG, "Invoice generated successfully: ${file.absolutePath}")
        file
    }

    private fun generateQRCode(text: String, size: Int): Bitmap? =
        runCatching {
            val matrix = QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size,
                mapOf(EncodeHintType.MARGIN to 1)
            )
            Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
                for (x in 0 until size)
                    for (y in 0 until size)
                        setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }.getOrNull()

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val result = mutableListOf<String>()
        var line = ""
        text.split(" ").forEach {
            val test = if (line.isEmpty()) it else "$line $it"
            if (paint.measureText(test) > maxWidth) {
                if (line.isNotEmpty()) result.add(line)
                line = it
            } else line = test
        }
        if (line.isNotEmpty()) result.add(line)
        return result
    }
}
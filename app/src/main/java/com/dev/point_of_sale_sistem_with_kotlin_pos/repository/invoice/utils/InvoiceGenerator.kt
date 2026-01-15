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

        private const val QR_SIZE = 100
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateInvoice(
        sale: Sale,
        productsMap: Map<String, String>,
        businessInfo: BusinessInfo,
        invoiceNumber: String
    ): Result<File> = runCatching {

        // 🔑 FIX DEFINITIVO DEL QR
        val cleanInvoiceNumber =
            invoiceNumber.trim().replace("\\s+".toRegex(), "")

        val document = PdfDocument()
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        )
        val canvas = page.canvas

        var y = MARGIN_TOP

        /* ================= HEADER ================= */

        val titlePaint = Paint().apply {
            color = COLOR_PRIMARY
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(businessInfo.name, MARGIN_LEFT, y, titlePaint)
        y += 35f

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

        /* ================= INVOICE BOX ================= */

        val boxX = PAGE_WIDTH - MARGIN_RIGHT - 200f
        val boxY = MARGIN_TOP
        val boxWidth = 200f
        val boxHeight = 90f

        val rect = RectF(boxX, boxY, boxX + boxWidth, boxY + boxHeight)

        Paint().apply {
            color = COLOR_HEADER_BG
            style = Paint.Style.FILL
        }.also { canvas.drawRoundRect(rect, 8f, 8f, it) }

        Paint().apply {
            color = COLOR_PRIMARY
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }.also { canvas.drawRoundRect(rect, 8f, 8f, it) }

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
        canvas.drawText(
            "No. $cleanInvoiceNumber",
            boxX + boxWidth / 2,
            boxTextY,
            invoiceDetailPaint
        )
        boxTextY += 18f

        canvas.drawText(
            sale.saleDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            boxX + boxWidth / 2,
            boxTextY,
            invoiceDetailPaint
        )

        y = maxOf(y, boxY + boxHeight) + 30f

        /* ================= PRODUCTS TABLE ================= */

        val headerBgPaint = Paint().apply {
            color = COLOR_HEADER_BG
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            MARGIN_LEFT,
            y - 5f,
            PAGE_WIDTH - MARGIN_RIGHT,
            y + 18f,
            headerBgPaint
        )

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

        val itemPaint = Paint().apply {
            color = COLOR_TEXT
            textSize = 10f
            isAntiAlias = true
        }

        sale.saleDetails.forEach { d ->
            val name = productsMap[d.productId.toString()] ?: "Producto"
            val itemStartY = y

            wrapText(name, PAGE_WIDTH - 350f, itemPaint).forEach {
                canvas.drawText(it, colProduct, y, itemPaint)
                y += 13f
            }

            canvas.drawText(d.quantity.toString(), colQty, itemStartY, itemPaint)
            canvas.drawText("$${"%.2f".format(d.unitPrice)}", colPrice, itemStartY, itemPaint)
            canvas.drawText(
                if (d.discount > 0) "$${"%.2f".format(d.discount)}" else "-",
                colDisc,
                itemStartY,
                itemPaint
            )
            canvas.drawText(
                "$${"%.2f".format(d.finalPrice)}",
                colTotal,
                itemStartY,
                Paint(itemPaint).apply { typeface = Typeface.DEFAULT_BOLD }
            )
            y += 5f
        }

        /* ================= QR ================= */

        y += 20f
        generateQRCode(cleanInvoiceNumber)?.let {
            canvas.drawBitmap(it, MARGIN_LEFT, y, null)
        }

        document.finishPage(page)

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_$cleanInvoiceNumber.pdf")

        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        Log.d(TAG, "Invoice generated successfully: ${file.absolutePath}")
        file
    }

    private fun generateQRCode(text: String): Bitmap? =
        runCatching {
            val matrix = QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                QR_SIZE,
                QR_SIZE,
                mapOf(EncodeHintType.MARGIN to 1)
            )

            Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.RGB_565).apply {
                for (x in 0 until QR_SIZE)
                    for (y in 0 until QR_SIZE)
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

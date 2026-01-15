package com.dev.point_of_sale_sistem_with_kotlin_pos.utils

import android.content.Context
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.BusinessInfo
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.CashRegisterSalesReport
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.CashierSalesReport
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.CategorySalesReport
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.ProductSalesReport
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.ReportSummary
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.ReportType
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.reports.ReportsState
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReportPrintManager(private val context: Context) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "DO")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    @RequiresApi(Build.VERSION_CODES.O)
    fun printReport(
        reportType: ReportType,
        state: ReportsState,
        businessInfo: BusinessInfo
    ) {
        val htmlContent = generateReportHTML(reportType, state, businessInfo)

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                createWebPrintJob(webView, getReportTitle(reportType, businessInfo.name))
            }
        }

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateReportHTML(
        reportType: ReportType,
        state: ReportsState,
        businessInfo: BusinessInfo
    ): String {
        val summary = state.summary ?: return ""

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                ${getCommonStyles()}
            </style>
        </head>
        <body>
            <div class="content-wrapper">
                ${generateHeader(businessInfo, reportType, summary)}
                ${generateSummarySection(summary)}
                ${generateDetailSection(reportType, state)}
            </div>
            ${generateFooter(businessInfo)}
        </body>
        </html>
        """.trimIndent()
    }

    private fun getCommonStyles(): String = """
        @page {
            size: letter;
            margin: 1.5cm 1cm 2cm 1cm;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            font-size: 10pt;
            color: #1a1a1a;
            margin: 0;
            padding: 0;
            position: relative;
            min-height: 100vh;
        }
        
        .header {
            border-bottom: 3px solid #6A5AF9;
            padding-bottom: 20px;
            margin-bottom: 30px;
        }
        
        .header-top {
            display: grid;
            grid-template-columns: 1fr auto;
            gap: 20px;
        }
        
        .business-name {
            font-size: 24pt;
            font-weight: 700;
            color: #1a1a1a;
            margin: 0 0 12px 0;
        }
        
        .business-details {
            font-size: 9pt;
            color: #666;
            line-height: 1.8;
        }
        
        .business-details div {
            margin: 4px 0;
        }
        
        .business-details strong {
            color: #333;
            font-weight: 600;
            min-width: 70px;
            display: inline-block;
        }
        
        .report-info {
            text-align: right;
        }
        
        .report-title {
            font-size: 16pt;
            font-weight: 700;
            color: #6A5AF9;
            margin: 0 0 10px 0;
        }
        
        .report-period {
            font-size: 11pt;
            color: #333;
            margin: 5px 0;
            font-weight: 500;
        }
        
        .report-meta {
            font-size: 9pt;
            color: #999;
            margin: 3px 0;
        }
        
        .summary-section {
            margin: 30px 0;
            padding-bottom: 20px;
            border-bottom: 1px solid #6A5AF9;
        }
        
        .summary-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 30px;
            margin-top: 20px;
        }
        
        .summary-card {
            text-align: center;
            padding: 20px 15px;
            border-left: 4px solid #6A5AF9;
            background: #fafafa;
        }
        
        .summary-label {
            font-size: 9.5pt;
            color: #666;
            margin-bottom: 10px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            font-weight: 500;
        }
        
        .summary-value {
            font-size: 22pt;
            font-weight: 700;
            color: #1a1a1a;
        }
        
        .detail-section {
            margin-top: 30px;
            page-break-inside: avoid;
        }
        
        .section-title {
            font-size: 14pt;
            font-weight: 700;
            color: #6A5AF9;
            padding-bottom: 8px;
            margin-bottom: 15px;
            border-bottom: 2px solid #6A5AF9;
        }
        
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 15px;
            border: 1px solid #e8e8e8;
        }
        
        thead {
            background: #f5f5f5;
            border-bottom: 2px solid #6A5AF9;
        }
        
        th {
            padding: 12px 10px;
            text-align: left;
            font-size: 9.5pt;
            font-weight: 700;
            color: #333;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        
        td {
            padding: 12px 10px;
            border-bottom: 1px solid #e8e8e8;
            font-size: 10pt;
            color: #1a1a1a;
        }
        
        tbody tr:nth-child(even) {
            background-color: #f9f9f9;
        }
        
        .text-right {
            text-align: right;
        }
        
        .text-center {
            text-align: center;
        }
        
        .amount {
            font-weight: 600;
            color: #1a1a1a;
        }
        
        .rank {
            display: inline-block;
            min-width: 35px;
            height: 35px;
            line-height: 35px;
            text-align: center;
            font-weight: 700;
            font-size: 11pt;
        }
        
        .rank-1 {
            background: #6A5AF9;
            color: white;
        }
        
        .rank-2 {
            background: #0080FF;
            color: white;
        }
        
        .rank-3 {
            background: #3399FF;
            color: white;
        }
        
        .rank-other {
            background: #f0f0f0;
            color: #666;
            font-weight: 600;
        }
        
        .footer {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            padding: 20px 1cm;
            border-top: 2px solid #e8e8e8;
            background: white;
        }
        
        .footer-content {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 30px;
            font-size: 9pt;
            color: #666;
            line-height: 1.6;
        }
        
        .footer-section h4 {
            font-size: 10pt;
            color: #6A5AF9;
            margin: 0 0 8px 0;
            font-weight: 700;
        }
        
        .footer-section div {
            margin: 3px 0;
        }
        
        .footer-copyright {
            text-align: center;
            margin-top: 15px;
            padding-top: 15px;
            border-top: 1px solid #e8e8e8;
            font-size: 8pt;
            color: #999;
        }
        
        .footer-copyright p {
            margin: 3px 0;
        }
        
        .statistics {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 20px;
            margin: 20px 0;
        }
        
        .stat-card {
            padding: 15px;
            border-left: 4px solid #6A5AF9;
            background: #fafafa;
        }
        
        .stat-label {
            font-size: 9pt;
            color: #666;
            margin-bottom: 6px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            font-weight: 500;
        }
        
        .stat-value {
            font-size: 18pt;
            font-weight: 700;
            color: #1a1a1a;
        }
        
        .content-wrapper {
            padding-bottom: 200px;
        }
        
        @media print {
            .page-break {
                page-break-after: always;
            }
            
            thead {
                display: table-header-group;
            }
            
            tr {
                page-break-inside: avoid;
            }
            
            .detail-section {
                page-break-inside: avoid;
            }
            
            table {
                page-break-inside: auto;
            }
        }
    """.trimIndent()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateHeader(
        businessInfo: BusinessInfo,
        reportType: ReportType,
        summary: ReportSummary
    ): String {
        val reportName = when (reportType) {
            is ReportType.ByCashRegister -> "Reporte por Caja Registradora"
            is ReportType.ByCashier -> "Reporte por Cajero"
            is ReportType.ByProduct -> "Reporte por Producto"
            is ReportType.ByCategory -> "Reporte por Categoría"
        }

        val currentDateTime = java.time.LocalDateTime.now().format(dateFormat)

        val businessDetails = buildString {
            businessInfo.address?.let { append("<div><strong>Dirección:</strong> $it</div>") }
            businessInfo.city?.let { append("<div><strong>Ciudad:</strong> $it</div>") }
            businessInfo.phone?.let { append("<div><strong>Teléfono:</strong> $it</div>") }
            businessInfo.email?.let { append("<div><strong>Email:</strong> $it</div>") }
            businessInfo.taxId?.let { append("<div><strong>RNC:</strong> $it</div>") }
        }

        return """
        <div class="header">
            <div class="header-top">
                <div>
                    <h1 class="business-name">${businessInfo.name}</h1>
                    <div class="business-details">
                        $businessDetails
                    </div>
                </div>
                <div class="report-info">
                    <h2 class="report-title">$reportName</h2>
                    <p class="report-period">Período: ${summary.period}</p>
                    <p class="report-meta">Fecha de generación: $currentDateTime</p>
                </div>
            </div>
        </div>
        """.trimIndent()
    }

    private fun generateSummarySection(summary: ReportSummary): String = """
        <div class="summary-section">
            <h3 style="margin: 0 0 5px 0; font-size: 14pt; color: #6A5AF9; font-weight: 700;">Resumen Ejecutivo</h3>
            <div class="summary-grid">
                <div class="summary-card">
                    <div class="summary-label">Ingresos Totales</div>
                    <div class="summary-value">${currencyFormat.format(summary.totalRevenue)}</div>
                </div>
                <div class="summary-card">
                    <div class="summary-label">Número de Ventas</div>
                    <div class="summary-value">${summary.totalSales}</div>
                </div>
                <div class="summary-card">
                    <div class="summary-label">Ticket Promedio</div>
                    <div class="summary-value">${currencyFormat.format(summary.averageTicket)}</div>
                </div>
            </div>
        </div>
    """.trimIndent()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateDetailSection(reportType: ReportType, state: ReportsState): String {
        return when (reportType) {
            is ReportType.ByCashRegister -> generateCashRegisterTable(state.cashRegisterReports)
            is ReportType.ByCashier -> generateCashierTable(state.cashierReports)
            is ReportType.ByProduct -> generateProductTable(state.productReports)
            is ReportType.ByCategory -> generateCategoryTable(state.categoryReports)
        }
    }

    private fun generateCashRegisterTable(reports: List<CashRegisterSalesReport>): String {
        if (reports.isEmpty()) return "<p style='text-align: center; color: #95A5A6; padding: 40px;'>No hay datos disponibles para este período</p>"

        val sortedReports = reports.sortedByDescending { it.totalSales }
        val totalRevenue = reports.sumOf { it.totalSales }

        val rows = sortedReports.mapIndexed { index, report ->
            val rank = getRankBadge(index + 1)
            val percentage = (report.totalSales / totalRevenue * 100)

            """
            <tr>
                <td class="text-center">$rank</td>
                <td><strong>${report.cashRegisterName}</strong></td>
                <td class="text-right amount">${currencyFormat.format(report.totalSales)}</td>
                <td class="text-right">${report.salesCount}</td>
                <td class="text-right amount">${currencyFormat.format(report.averageTicket)}</td>
                <td class="text-right" style="font-weight: 600;">${String.format("%.1f%%", percentage)}</td>
            </tr>
            """
        }.joinToString("")

        return """
        <div class="detail-section">
            <h3 class="section-title">Detalle por Caja Registradora</h3>
            <table>
                <thead>
                    <tr>
                        <th class="text-center" style="width: 80px;">Ranking</th>
                        <th>Caja</th>
                        <th class="text-right">Ventas Totales</th>
                        <th class="text-right">Transacciones</th>
                        <th class="text-right">Ticket Promedio</th>
                        <th class="text-right">Participación</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>
            ${generateStatistics(sortedReports.map { it.totalSales })}
        </div>
        """.trimIndent()
    }

    private fun generateCashierTable(reports: List<CashierSalesReport>): String {
        if (reports.isEmpty()) return "<p style='text-align: center; color: #95A5A6; padding: 40px;'>No hay datos disponibles para este período</p>"

        val sortedReports = reports.sortedByDescending { it.totalSales }
        val totalRevenue = reports.sumOf { it.totalSales }

        val rows = sortedReports.mapIndexed { index, report ->
            val rank = getRankBadge(index + 1)
            val percentage = (report.totalSales / totalRevenue * 100)

            """
            <tr>
                <td class="text-center">$rank</td>
                <td><strong>${report.username}</strong></td>
                <td class="text-right amount">${currencyFormat.format(report.totalSales)}</td>
                <td class="text-right">${report.salesCount}</td>
                <td class="text-right amount">${currencyFormat.format(report.averageTicket)}</td>
                <td class="text-right" style="font-weight: 600;">${String.format("%.1f%%", percentage)}</td>
            </tr>
            """
        }.joinToString("")

        return """
        <div class="detail-section">
            <h3 class="section-title">Detalle por Cajero</h3>
            <table>
                <thead>
                    <tr>
                        <th class="text-center" style="width: 80px;">Ranking</th>
                        <th>Cajero</th>
                        <th class="text-right">Ventas Totales</th>
                        <th class="text-right">Transacciones</th>
                        <th class="text-right">Ticket Promedio</th>
                        <th class="text-right">Participación</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>
            ${generateStatistics(sortedReports.map { it.totalSales })}
        </div>
        """.trimIndent()
    }

    private fun generateProductTable(reports: List<ProductSalesReport>): String {
        if (reports.isEmpty()) return "<p style='text-align: center; color: #95A5A6; padding: 40px;'>No hay datos disponibles para este período</p>"

        val sortedReports = reports.sortedByDescending { it.totalRevenue }
        val totalRevenue = reports.sumOf { it.totalRevenue }
        val totalQuantity = reports.sumOf { it.quantitySold }

        val rows = sortedReports.mapIndexed { index, report ->
            val rank = getRankBadge(index + 1)
            val percentage = (report.totalRevenue / totalRevenue * 100)

            """
            <tr>
                <td class="text-center">$rank</td>
                <td><strong>${report.productName}</strong><br>
                    <small style="color: #7F8C8D; font-size: 8.5pt;">${report.categoryName}</small>
                </td>
                <td class="text-right">${report.quantitySold}</td>
                <td class="text-right amount">${currencyFormat.format(report.totalRevenue)}</td>
                <td class="text-right amount">${currencyFormat.format(report.averagePrice)}</td>
                <td class="text-right" style="font-weight: 600;">${String.format("%.1f%%", percentage)}</td>
            </tr>
            """
        }.joinToString("")

        return """
        <div class="detail-section">
            <h3 class="section-title">Detalle por Producto</h3>
            
            <div class="statistics">
                <div class="stat-card">
                    <div class="stat-label">Total Unidades Vendidas</div>
                    <div class="stat-value">$totalQuantity</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">Productos Diferentes</div>
                    <div class="stat-value">${reports.size}</div>
                </div>
            </div>
            
            <table>
                <thead>
                    <tr>
                        <th class="text-center" style="width: 80px;">Ranking</th>
                        <th>Producto</th>
                        <th class="text-right">Cantidad</th>
                        <th class="text-right">Ingresos</th>
                        <th class="text-right">Precio Promedio</th>
                        <th class="text-right">Participación</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>
        </div>
        """.trimIndent()
    }

    private fun generateCategoryTable(reports: List<CategorySalesReport>): String {
        if (reports.isEmpty()) return "<p style='text-align: center; color: #95A5A6; padding: 40px;'>No hay datos disponibles para este período</p>"

        val sortedReports = reports.sortedByDescending { it.totalRevenue }
        val totalRevenue = reports.sumOf { it.totalRevenue }

        val rows = sortedReports.mapIndexed { index, report ->
            val rank = getRankBadge(index + 1)
            val percentage = (report.totalRevenue / totalRevenue * 100)

            """
            <tr>
                <td class="text-center">$rank</td>
                <td><strong>${report.categoryName}</strong></td>
                <td class="text-right">${report.productsSold}</td>
                <td class="text-right">${report.salesCount}</td>
                <td class="text-right amount">${currencyFormat.format(report.totalRevenue)}</td>
                <td class="text-right" style="font-weight: 600;">${String.format("%.1f%%", percentage)}</td>
            </tr>
            """
        }.joinToString("")

        return """
        <div class="detail-section">
            <h3 class="section-title">Detalle por Categoría</h3>
            <table>
                <thead>
                    <tr>
                        <th class="text-center" style="width: 80px;">Ranking</th>
                        <th>Categoría</th>
                        <th class="text-right">Productos Vendidos</th>
                        <th class="text-right">Transacciones</th>
                        <th class="text-right">Ingresos Totales</th>
                        <th class="text-right">Participación</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>
            ${generateStatistics(sortedReports.map { it.totalRevenue })}
        </div>
        """.trimIndent()
    }

    private fun getRankBadge(position: Int): String {
        val (rankClass, display) = when (position) {
            1 -> "rank-1" to "1"
            2 -> "rank-2" to "2"
            3 -> "rank-3" to "3"
            else -> "rank-other" to "$position"
        }
        return """<span class="rank $rankClass">$display</span>"""
    }

    private fun generateStatistics(values: List<Double>): String {
        if (values.isEmpty()) return ""

        val max = values.maxOrNull() ?: 0.0
        val min = values.minOrNull() ?: 0.0

        return """
        <div class="statistics" style="margin-top: 25px;">
            <div class="stat-card">
                <div class="stat-label">Valor Máximo</div>
                <div class="stat-value">${currencyFormat.format(max)}</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Valor Mínimo</div>
                <div class="stat-value">${currencyFormat.format(min)}</div>
            </div>
        </div>
        """.trimIndent()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateFooter(businessInfo: BusinessInfo): String {
        val timestamp = java.time.LocalDateTime.now().format(dateFormat)

        val contactInfo = buildString {
            append("<h4>Información de Contacto</h4>")
            businessInfo.address?.let { append("<div><strong>Dirección:</strong> $it</div>") }
            businessInfo.city?.let { append("<div><strong>Ciudad:</strong> $it</div>") }
            businessInfo.phone?.let { append("<div><strong>Teléfono:</strong> $it</div>") }
            businessInfo.email?.let { append("<div><strong>Email:</strong> $it</div>") }
        }

        val legalInfo = buildString {
            append("<h4>Información Legal</h4>")
            append("<div><strong>Razón Social:</strong> ${businessInfo.name}</div>")
            businessInfo.taxId?.let { append("<div><strong>RNC:</strong> $it</div>") }
            append("<div><strong>Fecha de emisión:</strong> $timestamp</div>")
        }

        return """
        <div class="footer">
            <div class="footer-content">
                <div>
                    $contactInfo
                </div>
                <div>
                    $legalInfo
                </div>
            </div>
            <div class="footer-copyright">
                <p>Este documento fue generado automáticamente por el Sistema de Punto de Venta</p>
                <p>© ${java.time.Year.now().value} ${businessInfo.name}. Todos los derechos reservados.</p>
            </div>
        </div>
        """.trimIndent()
    }

    private fun getReportTitle(reportType: ReportType, businessName: String): String {
        val reportTypeName = when (reportType) {
            is ReportType.ByCashRegister -> "Cajas"
            is ReportType.ByCashier -> "Cajeros"
            is ReportType.ByProduct -> "Productos"
            is ReportType.ByCategory -> "Categorias"
        }
        return "${businessName}_Reporte_$reportTypeName"
    }

    private fun createWebPrintJob(webView: WebView, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val printAdapter: PrintDocumentAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.createPrintDocumentAdapter(jobName)
        } else {
            @Suppress("DEPRECATION")
            webView.createPrintDocumentAdapter()
        }

        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, printAdapter, attributes)
    }
}
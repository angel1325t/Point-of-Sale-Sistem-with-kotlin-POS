package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.dashboard.DashboardIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.DashboardState
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.TopProduct
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.dashboard.DashboardViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Sección principal del Dashboard que se integra en HomeScreen
 */
@Composable
fun DashboardSection(
    viewModel: DashboardViewModel,
    state: DashboardState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con título y botón de refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Panel de Control",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { viewModel.handleIntent(DashboardIntent.RefreshData) },
                enabled = !state.isRefreshing
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar",
                    tint = if (state.isRefreshing) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }

        when {
            state.isLoading && !state.hasData -> {
                LoadingDashboard()
            }

            state.error != null -> {
                ErrorDashboard(
                    error = state.error.message,
                    onRetry = { viewModel.handleIntent(DashboardIntent.LoadDashboard) },
                    onDismiss = { viewModel.handleIntent(DashboardIntent.ClearError) }
                )
            }

            !state.hasData -> {
                EmptyDashboard()
            }

            else -> {
                // Indicador de refresh
                if (state.isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Tarjetas de resumen
                SummaryCards(state = state)

                // Productos más vendidos
                TopProductsCard(
                    products = state.topProducts,
                    modifier = Modifier.fillMaxWidth()
                )

                // Gráfico de ingresos
                RevenueChart(
                    data = state.revenueData,
                    modifier = Modifier.fillMaxWidth()
                )

                // Márgenes de ganancia
                ProfitMarginsCard(
                    margins = state.profitMargins,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// TARJETAS DE RESUMEN
// ═══════════════════════════════════════════════════════════

/**
 * Tarjetas de resumen con métricas clave
 */
@Composable
private fun SummaryCards(state: DashboardState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total de ingresos
            MetricCard(
                title = "Ingresos Totales",
                value = formatCurrency(state.summary.totalRevenue),
                icon = Icons.Default.AttachMoney,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Total de ventas
            MetricCard(
                title = "Ventas",
                value = state.summary.totalSales.toString(),
                icon = Icons.Default.ShoppingCart,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ticket promedio
            MetricCard(
                title = "Ticket Promedio",
                value = formatCurrency(state.summary.averageTicket),
                icon = Icons.Default.Receipt,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )

            // Margen de ganancia
            MetricCard(
                title = "Margen",
                value = "${String.format("%.1f", state.profitMargins.profitMargin)}%",
                icon = Icons.Default.TrendingUp,
                color = if (state.profitMargins.hasProfit) {
                    Color(0xFF4CAF50)
                } else {
                    Color(0xFFF44336)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Tarjeta individual de métrica
 */
@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PRODUCTOS MÁS VENDIDOS
// ═══════════════════════════════════════════════════════════

/**
 * Tarjeta de productos más vendidos
 */
@Composable
private fun TopProductsCard(
    products: List<TopProduct>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Productos Más Vendidos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (products.isEmpty()) {
                EmptyProductsList()
            } else {
                products.forEachIndexed { index, product ->
                    TopProductItem(
                        product = product,
                        rank = index + 1
                    )
                    if (index < products.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}

/**
 * Item individual de producto más vendido
 */
@Composable
private fun TopProductItem(product: TopProduct, rank: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ranking badge
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = when (rank) {
                1 -> Color(0xFFFFD700) // Oro
                2 -> Color(0xFFC0C0C0) // Plata
                3 -> Color(0xFFCD7F32) // Bronce
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Info del producto
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.productName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${product.quantitySold} unidades vendidas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Ingresos y porcentaje
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatCurrency(product.revenue),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${String.format("%.1f", product.percentage)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyProductsList() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay datos de productos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════
// GRÁFICO DE INGRESOS
// ═══════════════════════════════════════════════════════════

/**
 * Gráfico simplificado de ingresos
 */
@Composable
private fun RevenueChart(
    data: List<com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.RevenueDataPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Evolución de Ingresos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {
                EmptyChartView()
            } else {
                SimpleBarChart(data = data)
            }
        }
    }
}

/**
 * Gráfico de barras simple (sin librerías externas)
 */
@Composable
private fun SimpleBarChart(
    data: List<com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.RevenueDataPoint>
) {
    val maxRevenue = data.maxOfOrNull { it.revenue } ?: 1.0
    val displayData = data.takeLast(7) // Últimos 7 días

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        displayData.forEach { point ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fecha
                Text(
                    text = formatDateShort(point.date),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(50.dp),
                    fontWeight = FontWeight.Medium
                )

                // Barra
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((point.revenue / maxRevenue).toFloat().coerceIn(0.05f, 1f))
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }

                // Valor
                Text(
                    text = formatCurrencyShort(point.revenue),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(70.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyChartView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay datos de ingresos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════
// MÁRGENES DE GANANCIA
// ═══════════════════════════════════════════════════════════

/**
 * Tarjeta de márgenes de ganancia
 */
@Composable
private fun ProfitMarginsCard(
    margins: com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard.ProfitMargins,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Análisis de Rentabilidad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ProfitRow(label = "Ingresos Totales", value = margins.totalRevenue)
            ProfitRow(label = "Costo Total (Est.)", value = margins.totalCost)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ProfitRow(
                label = "Ganancia Bruta",
                value = margins.grossProfit,
                isHighlight = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (margins.hasProfit) {
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                } else {
                    Color(0xFFF44336).copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (margins.hasProfit) {
                                Icons.Default.TrendingUp
                            } else {
                                Icons.Default.TrendingDown
                            },
                            contentDescription = null,
                            tint = if (margins.hasProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                        Text(
                            text = "Margen de Ganancia",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", margins.profitMargin)}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (margins.hasProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfitRow(
    label: String,
    value: Double,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isHighlight) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = formatCurrency(value),
            style = if (isHighlight) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// ESTADOS DE CARGA Y ERROR
// ═══════════════════════════════════════════════════════════

@Composable
private fun LoadingDashboard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Cargando dashboard...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorDashboard(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Error al cargar el dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
private fun EmptyDashboard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShowChart,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No hay datos disponibles",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Realiza algunas ventas para ver estadísticas aquí",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// FUNCIONES AUXILIARES
// ═══════════════════════════════════════════════════════════

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "DO"))
    return formatter.format(amount)
}

private fun formatCurrencyShort(amount: Double): String {
    return when {
        amount >= 1000000 -> String.format("%.1fM", amount / 1000000)
        amount >= 1000 -> String.format("%.1fK", amount / 1000)
        else -> amount.toInt().toString()
    }
}

private fun formatDateShort(date: String): String {
    // Formato: "2024-01-15" -> "01/15"
    return try {
        val parts = date.split("-")
        if (parts.size >= 3) {
            "${parts[1]}/${parts[2]}"
        } else {
            date.takeLast(5)
        }
    } catch (e: Exception) {
        date.takeLast(5)
    }
}
package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.purchase_orders.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.*

/* -------------------- ORDER LIST ITEM - SIMPLIFICADO ---------------------- */

@Composable
fun PurchaseOrderListItem(
    order: PurchaseOrder,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsReceived: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Header con estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pedido #${order.orderId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OrderStatusBadge(status = order.status)
            }

            Divider()

            // Información del pedido
            InfoRow(
                icon = Icons.Default.Store,
                label = "Proveedor",
                value = order.supplierName
            )

            InfoRow(
                icon = Icons.Default.Inventory,
                label = "Producto",
                value = order.productName
            )

            InfoRow(
                icon = Icons.Default.Numbers,
                label = "Cantidad",
                value = order.quantity.toString()
            )

            InfoRow(
                icon = Icons.Default.CalendarToday,
                label = "Fecha de pedido",
                value = order.orderDate
            )

            if (order.receivedDate != null) {
                InfoRow(
                    icon = Icons.Default.CheckCircle,
                    label = "Recibido el",
                    value = order.receivedDate
                )
            }

            if (!order.notes.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.Default.Notes,
                    label = "Notas",
                    value = order.notes
                )
            }

            // Acciones según el estado - SIMPLIFICADO
            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    order.isPending -> {
                        // Botón prominente para recibir pedido
                        Button(
                            onClick = onMarkAsReceived,
                            modifier = Modifier.padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Recibir pedido",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Recibir")
                        }
                    }
                }

                if (!order.isReceived) {
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.Edit, "Editar")
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/* -------------------- INFO ROW ---------------------- */

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/* -------------------- STATUS BADGE - SIMPLIFICADO ---------------------- */

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (color, icon) = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.secondary to Icons.Default.Schedule
        OrderStatus.RECEIVED -> MaterialTheme.colorScheme.tertiary to Icons.Default.CheckCircle
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                status.toSpanish(),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* -------------------- EMPTY STATE ---------------------- */

@Composable
fun EmptyOrdersState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No hay pedidos",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Crea tu primer pedido de reposición",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* -------------------- SEARCH BAR ---------------------- */

@Composable
fun OrderSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Buscar por proveedor o producto...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, null)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

/* -------------------- DELETE DIALOG ---------------------- */

@Composable
fun DeleteOrderDialog(
    order: PurchaseOrder,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("¿Eliminar pedido?", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Estás a punto de eliminar el pedido:")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pedido #${order.orderId}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Producto: ${order.productName}")
                Text("Cantidad: ${order.quantity}")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Esta acción no se puede deshacer.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/* -------------------- LOADING INDICATOR ---------------------- */

@Composable
fun LoadingIndicator(message: String = "Cargando...") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message)
    }
}

/* -------------------- ERROR MESSAGE ---------------------- */

@Composable
fun ErrorMessage(
    error: PurchaseOrderError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(error.message)

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cerrar")
                }

                if (error.isRecoverable()) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}
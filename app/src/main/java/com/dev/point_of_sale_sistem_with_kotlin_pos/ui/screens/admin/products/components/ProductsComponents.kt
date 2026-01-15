package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.DiscountType
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.Category
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.utils.toDiscountType

/* ---------------------------------------------------------
                DIALOG: ELIMINAR PRODUCTO
---------------------------------------------------------- */
@Composable
fun DeleteProductDialog(
    product: ProductDTO,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                stringResource(R.string.product_delete_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.product_delete_message))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                product.description?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    stringResource(R.string.product_delete_warning),
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
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.product_delete_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/* ---------------------------------------------------------
                    PRODUCT LIST ITEM
---------------------------------------------------------- */
@Composable
fun ProductListItem(
    product: ProductDTO,
    isLowStock: Boolean,
    isOutOfStock: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Nombre
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Descripción
                product.description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Precio
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (product.discountType.toDiscountType() != DiscountType.NONE) {
                        Text(
                            "RD$ ${product.finalPrice}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "RD$ ${product.price}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    } else {
                        Text(
                            "RD$ ${product.price}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Stock
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = when {
                            isOutOfStock -> MaterialTheme.colorScheme.error
                            isLowStock -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        stringResource(R.string.product_stock_label, product.currentStock),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isOutOfStock -> MaterialTheme.colorScheme.error
                            isLowStock -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isLowStock || isOutOfStock) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Código de barras
                product.barcode?.let {
                    Text(
                        stringResource(R.string.product_barcode_label, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Acciones
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------------
                    STATE: EMPTY LIST
---------------------------------------------------------- */
@Composable
fun EmptyProductsState(
    isSearching: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isSearching) Icons.Default.SearchOff else Icons.Default.Inventory,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            if (isSearching)
                stringResource(R.string.product_empty_search)
            else
                stringResource(R.string.product_empty_list),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        Text(
            if (isSearching)
                stringResource(R.string.product_empty_search_message)
            else
                stringResource(R.string.product_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ---------------------------------------------------------
                SEARCH BAR (REUSABLE)
---------------------------------------------------------- */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, stringResource(R.string.action_clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

/* ---------------------------------------------------------
                    ERROR MESSAGE
---------------------------------------------------------- */
@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.cash_register_error_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

/* ---------------------------------------------------------
                    LOADING INDICATOR
---------------------------------------------------------- */
@Composable
fun LoadingIndicator(
    message: String = "Cargando..."
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/* ---------------------------------------------------------
                DIALOG: SELECCIONAR CATEGORÍA
---------------------------------------------------------- */
@Composable
fun CategoryPickerDialog(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.category_select),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category.categoryId == selectedCategoryId

                    Card(
                        onClick = { onSelect(category.categoryId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                category.name,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.category_close))
            }
        }
    )
}

/* ---------------------------------------------------------
                DIALOG: CONFIGURAR DESCUENTO
---------------------------------------------------------- */
@Composable
fun DiscountPickerDialog(
    currentType: DiscountType,
    currentValue: Double,
    onConfirm: (DiscountType, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var value by remember { mutableStateOf(currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.product_discount_configure),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Selector de tipo
                Text(
                    stringResource(R.string.discount_type),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                // Opciones de tipo
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscountType.values().forEach { type ->
                        Card(
                            onClick = {
                                selectedType = type
                                if (type == DiscountType.NONE) {
                                    value = "0"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedType == type)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = selectedType == type,
                                    onClick = {
                                        selectedType = type
                                        if (type == DiscountType.NONE) {
                                            value = "0"
                                        }
                                    }
                                )
                                Text(
                                    when (type) {
                                        DiscountType.NONE -> stringResource(R.string.discount_none)
                                        DiscountType.PERCENT -> stringResource(R.string.discount_percent)
                                        DiscountType.FIXED -> stringResource(R.string.discount_fixed)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Input de valor (solo si no es NONE)
                if (selectedType != DiscountType.NONE) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = {
                            Text(
                                when (selectedType) {
                                    DiscountType.PERCENT -> stringResource(R.string.discount_percentage_label)
                                    DiscountType.FIXED -> stringResource(R.string.discount_amount_label)
                                    else -> stringResource(R.string.discount_value)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (selectedType == DiscountType.PERCENT)
                                    Icons.Default.Percent
                                else
                                    Icons.Default.AttachMoney,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = value.toDoubleOrNull() == null || value.toDoubleOrNull()!! < 0,
                        supportingText = {
                            if (selectedType == DiscountType.PERCENT) {
                                Text(stringResource(R.string.discount_helper_percent))
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalValue = if (selectedType == DiscountType.NONE) {
                        0.0
                    } else {
                        value.toDoubleOrNull() ?: 0.0
                    }
                    onConfirm(selectedType, finalValue)
                },
                shape = RoundedCornerShape(12.dp),
                enabled = selectedType == DiscountType.NONE ||
                        (value.toDoubleOrNull()?.let { it >= 0 } == true)
            ) {
                Text(stringResource(R.string.discount_apply))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
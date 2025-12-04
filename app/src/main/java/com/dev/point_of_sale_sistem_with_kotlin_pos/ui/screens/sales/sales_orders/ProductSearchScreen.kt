package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders.SalesIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.sales_orders.SalesViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSearchScreen(
    viewModel: SalesViewModel,
    products: List<ProductDTO>,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductDTO?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sales_search_product)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            // SEARCH BAR WITH SUGGESTIONS
            ExposedDropdownMenuBox(
                expanded = expanded && searchQuery.isNotEmpty(),
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        expanded = it.isNotEmpty()
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.sales_search_by_name)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; expanded = false }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )

                val filteredProducts = products.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.barcode?.contains(searchQuery, ignoreCase = true) == true
                }

                ExposedDropdownMenu(
                    expanded = expanded && searchQuery.isNotEmpty(),
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.heightIn(max = 250.dp)
                ) {
                    if (filteredProducts.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Sin resultados") },
                            onClick = {}
                        )
                    } else {
                        filteredProducts.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(product.name, fontWeight = FontWeight.Bold)
                                        product.barcode?.let {
                                            Text("Código: $it", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(
                                            "Stock: ${product.currentStock}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (product.currentStock > product.minimumStock)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                onClick = {
                                    selectedProduct = product
                                    searchQuery = product.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    selectedProduct?.let { product ->
        AddToCartDialog(
            product = product,
            onDismiss = { selectedProduct = null },
            onConfirm = { quantity, discount ->
                viewModel.handleIntent(
                    SalesIntent.AddSaleDetail(
                        productId = product.productId,
                        quantity = quantity,
                        unitPrice = product.price,
                        discount = discount
                    )
                )
                selectedProduct = null
                onNavigateBack()
            }
        )
    }
}

// ---------------- ADD TO CART DIALOG ----------------

@Composable
private fun AddToCartDialog(
    product: ProductDTO,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, discount: Double) -> Unit
) {
    var quantity by remember { mutableStateOf("1") }
    var discount by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar al carrito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Text(product.name, fontWeight = FontWeight.Bold)

                Text(
                    text = stringResource(R.string.sales_price_format, product.price),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider()

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { char -> char.isDigit() } },
                    label = { Text("Cantidad") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = discount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) discount = it
                    },
                    label = { Text("Descuento") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                val qty = quantity.toIntOrNull() ?: 0
                val disc = discount.toDoubleOrNull() ?: 0.0
                val subtotal = (product.price * qty) - disc

                if (qty > 0) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal")
                            Text(
                                stringResource(R.string.sales_price_format, subtotal),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    val disc = discount.toDoubleOrNull() ?: 0.0
                    if (qty > 0 && qty <= product.currentStock) onConfirm(qty, disc)
                },
                enabled = (quantity.toIntOrNull() ?: 0) > 0 &&
                        (quantity.toIntOrNull() ?: 0) <= product.currentStock
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

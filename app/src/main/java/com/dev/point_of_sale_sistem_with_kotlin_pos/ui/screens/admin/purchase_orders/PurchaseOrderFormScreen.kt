package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.purchase_orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.purchase_orders.PurchaseOrderIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.suppliers.SupplierIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.products.ProductsIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.Supplier
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.Product
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderFormScreen(
    orderViewModel: PurchaseOrderViewModel,
    supplierViewModel: SupplierViewModel,
    productViewModel: ProductsViewModel,
    orderId: Int?,
    onNavigateBack: () -> Unit
) {
    val orderState by orderViewModel.state.collectAsState()
    val supplierState by supplierViewModel.state.collectAsState()
    val productState by productViewModel.state.collectAsState()

    val isEditMode = orderId != null

    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableStateOf("") }
    var orderDate by remember { mutableStateOf(
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    ) }
    var notes by remember { mutableStateOf("") }

    var showSupplierPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }

    // Cargar proveedores y productos
    LaunchedEffect(Unit) {
        supplierViewModel.handleIntent(SupplierIntent.LoadSuppliers)
        productViewModel.handleIntent(ProductsIntent.LoadProducts)
    }

    // Cargar datos en modo edición
    LaunchedEffect(orderId) {
        if (orderId != null) {
            orderViewModel.handleIntent(PurchaseOrderIntent.LoadOrderById(orderId))
        }
    }

    // Actualizar campos en modo edición
    LaunchedEffect(orderState.selectedOrder) {
        orderState.selectedOrder?.let { order ->
            selectedSupplier = supplierState.suppliers.find { it.supplierId == order.supplierId }
            selectedProduct = productState.products.find { it.productId == order.productId }
            quantity = order.quantity.toString()
            orderDate = order.orderDate
            notes = order.notes ?: ""
        }
    }

    // Navegar atrás al completar la operación
    LaunchedEffect(orderState.operationSuccess) {
        if (orderState.operationSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Editar Pedido" else "Nuevo Pedido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->

        if (orderState.isLoading && isEditMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // PROVEEDOR
                OutlinedCard(
                    onClick = { showSupplierPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Proveedor *",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                selectedSupplier?.name ?: "Seleccionar proveedor",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedSupplier != null)
                                    FontWeight.Bold else FontWeight.Normal
                            )
                            selectedSupplier?.let { supplier ->
                                Text(
                                    supplier.getContactSummary(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }

                if (orderState.supplierError != null) {
                    Text(
                        orderState.supplierError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // PRODUCTO
                OutlinedCard(
                    onClick = { showProductPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Producto *",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                selectedProduct?.name ?: "Seleccionar producto",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedProduct != null)
                                    FontWeight.Bold else FontWeight.Normal
                            )
                            selectedProduct?.let { product ->
                                Text(
                                    "Stock actual: ${product.currentStock}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (product.currentStock <= product.minimumStock)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }

                if (orderState.productError != null) {
                    Text(
                        orderState.productError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // CANTIDAD
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        it.toIntOrNull()?.let { qty ->
                            orderViewModel.handleIntent(
                                PurchaseOrderIntent.ValidateQuantity(qty)
                            )
                        }
                    },
                    label = { Text("Cantidad *") },
                    leadingIcon = { Icon(Icons.Default.Numbers, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = orderState.quantityError != null,
                    supportingText = {
                        orderState.quantityError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // FECHA
                OutlinedTextField(
                    value = orderDate,
                    onValueChange = { orderDate = it },
                    label = { Text("Fecha del pedido") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = false
                )

                // NOTAS
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (Opcional)") },
                    leadingIcon = { Icon(Icons.Default.Notes, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // BOTONES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (!isEditMode) {
                                orderViewModel.handleIntent(
                                    PurchaseOrderIntent.CreateOrder(
                                        supplierId = selectedSupplier?.supplierId ?: 0,
                                        productId = selectedProduct?.productId ?: 0,
                                        quantity = quantity.toIntOrNull() ?: 0,
                                        orderDate = orderDate,
                                        notes = notes.ifBlank { null }
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedSupplier != null &&
                                selectedProduct != null &&
                                quantity.toIntOrNull() != null &&
                                quantity.toIntOrNull()!! > 0 &&
                                !orderState.isLoading
                    ) {
                        if (orderState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (isEditMode) "Actualizar" else "Crear Pedido")
                        }
                    }
                }

                // ERROR
                if (orderState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                orderState.error!!.message,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOGO SELECCIONAR PROVEEDOR
    if (showSupplierPicker) {
        SupplierPickerDialog(
            suppliers = supplierState.suppliers,
            selectedSupplier = selectedSupplier,
            onSelect = {
                selectedSupplier = it
                showSupplierPicker = false
                orderViewModel.handleIntent(
                    PurchaseOrderIntent.ValidateSupplier(it?.supplierId)
                )
            },
            onDismiss = { showSupplierPicker = false }
        )
    }

    // DIALOGO SELECCIONAR PRODUCTO
    if (showProductPicker) {
        ProductPickerDialog(
            products = productState.products,
            selectedProduct = selectedProduct,
            onSelect = {
                selectedProduct = it
                showProductPicker = false
                orderViewModel.handleIntent(
                    PurchaseOrderIntent.ValidateProduct(it?.productId)
                )
            },
            onDismiss = { showProductPicker = false }
        )
    }
}

/* -------------------- SUPPLIER PICKER DIALOG ---------------------- */

@Composable
fun SupplierPickerDialog(
    suppliers: List<Supplier>,
    selectedSupplier: Supplier?,
    onSelect: (Supplier?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Proveedor") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suppliers) { supplier ->
                    val isSelected = supplier.supplierId == selectedSupplier?.supplierId

                    Card(
                        onClick = { onSelect(supplier) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Store, null)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    supplier.name,
                                    fontWeight = if (isSelected)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    supplier.getContactSummary(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

/* -------------------- PRODUCT PICKER DIALOG ---------------------- */

@Composable
fun ProductPickerDialog(
    products: List<Product>,
    selectedProduct: Product?,
    onSelect: (Product?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Producto") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
                    val isSelected = product.productId == selectedProduct?.productId
                    val needsRestock = product.currentStock <= product.minimumStock

                    Card(
                        onClick = { onSelect(product) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (needsRestock) Icons.Default.Warning
                                else Icons.Default.Inventory,
                                null,
                                tint = if (needsRestock)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    product.name,
                                    fontWeight = if (isSelected)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    "Stock: ${product.currentStock} (Mín: ${product.minimumStock})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (needsRestock)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
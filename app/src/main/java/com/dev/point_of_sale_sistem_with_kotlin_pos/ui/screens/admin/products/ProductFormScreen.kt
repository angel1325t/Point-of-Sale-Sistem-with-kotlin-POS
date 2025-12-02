package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.products.ProductsIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.ProductsViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.components.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    viewModel: ProductsViewModel,
    productId: Int?,
    categories: List<Category>,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val isEditMode = productId != null

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var minimumStock by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Int?>(null) }
    var barcode by remember { mutableStateOf("") }

    var showCategoryPicker by remember { mutableStateOf(false) }

    // ----------------------------------------------------
    // CARGAR PRODUCTO EN EDICIÓN
    // ----------------------------------------------------
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.handleIntent(ProductsIntent.LoadProductById(productId))
        }
    }

    // Cuando el producto llega del estado, llenar los campos
    LaunchedEffect(state.selectedProduct) {
        state.selectedProduct?.let { p ->
            name = p.name
            description = p.description ?: ""
            price = p.price.toString()
            minimumStock = p.minimumStock.toString()
            barcode = p.barcode ?: ""
            categoryId = p.categoryId
        }
    }

    // Cerrar pantalla cuando se crea/actualiza
    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Editar Producto" else "Nuevo Producto",
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

        if (state.isLoading && isEditMode) {
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

                // ------------------------
                // NOMBRE
                // ------------------------
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre *") },
                    leadingIcon = { Icon(Icons.Default.Inventory, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // DESCRIPCIÓN
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    leadingIcon = { Icon(Icons.Default.Description, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                // PRECIO
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Precio (RD$)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = price.toDoubleOrNull() == null
                )

                // STOCK MÍNIMO
                OutlinedTextField(
                    value = minimumStock,
                    onValueChange = { minimumStock = it },
                    label = { Text("Stock mínimo *") },
                    leadingIcon = { Icon(Icons.Default.Warning, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = minimumStock.toIntOrNull() == null
                )

                // BARCODE
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Código de barras (Opcional)") },
                    leadingIcon = { Icon(Icons.Default.QrCode, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // CATEGORÍA
                OutlinedCard(
                    onClick = { showCategoryPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val categoryName = categories.find { it.categoryId == categoryId }?.name

                        Text(
                            text = categoryName ?: "Seleccionar categoría",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }

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
                            if (isEditMode && productId != null) {
                                viewModel.handleIntent(
                                    ProductsIntent.UpdateProduct(
                                        productId = productId,
                                        name = name,
                                        description = description.ifBlank { null },
                                        price = price.toDoubleOrNull() ?: 0.0,
                                        barcode = barcode.ifBlank { null },
                                        categoryId = categoryId ?: 0,
                                        image = null,
                                        currentStock = 0,
                                        minimumStock = minimumStock.toIntOrNull() ?: 0
                                    )
                                )
                            } else {
                                viewModel.handleIntent(
                                    ProductsIntent.CreateProduct(
                                        name = name,
                                        description = description.ifBlank { null },
                                        price = price.toDoubleOrNull() ?: 0.0,
                                        barcode = barcode.ifBlank { null },
                                        categoryId = categoryId ?: 0,
                                        image = null,
                                        currentStock = 0,
                                        minimumStock = minimumStock.toIntOrNull() ?: 0
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = name.isNotBlank() &&
                                price.toDoubleOrNull() != null &&
                                minimumStock.toIntOrNull() != null &&
                                categoryId != null
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (isEditMode) "Actualizar" else "Crear")
                        }
                    }
                }

                // ERROR
                if (state.error != null) {
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
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                state.error!!.message,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------------------
    // DIALOG PARA SELECCIONAR CATEGORÍA
    // ------------------------------
    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = categories,
            selectedCategoryId = categoryId,
            onSelect = {
                categoryId = it
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }
}

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.products.ProductsIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.Category
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.utils.toDiscountType
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.components.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.ProductViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.utils.toServerString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    viewModel: ProductViewModel,
    productId: Int?,
    categories: List<Category>,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isEditMode = productId != null

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var currentStock by remember { mutableStateOf("") }
    var minimumStock by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Int?>(null) }
    var discountType by remember { mutableStateOf(DiscountType.NONE) }
    var discountValue by remember { mutableStateOf("") }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDiscountPicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Precio final con descuento
    val finalPrice = remember(price, discountType, discountValue) {
        val p = price.toDoubleOrNull() ?: 0.0
        val d = discountValue.toDoubleOrNull() ?: 0.0
        when (discountType) {
            DiscountType.NONE -> p
            DiscountType.PERCENT -> p * (1 - d / 100)
            DiscountType.FIXED -> (p - d).coerceAtLeast(0.0)
        }
    }

    // Función helper para obtener el mensaje de error (NO @Composable)
    fun getErrorMessage(error: ProductsError): String {
        return when (error) {
            // Errores de red
            is ProductsError.NetworkError -> context.getString(R.string.error_network)
            is ProductsError.TimeoutError -> context.getString(R.string.error_timeout)
            is ProductsError.NoInternetConnection -> context.getString(R.string.error_no_internet)

            // Errores de validación
            is ProductsError.ValidationError -> context.getString(R.string.error_validation_generic, error.field, error.message)
            is ProductsError.InvalidProductName -> context.getString(R.string.error_product_name_invalid)
            is ProductsError.ProductNameTooShort -> context.getString(R.string.error_product_name_too_short)
            is ProductsError.ProductNameTooLong -> context.getString(R.string.error_product_name_too_long)
            is ProductsError.InvalidPrice -> context.getString(R.string.error_price_invalid)
            is ProductsError.PriceZeroOrNegative -> context.getString(R.string.error_price_zero_or_negative)
            is ProductsError.InvalidStock -> context.getString(R.string.error_stock_invalid)
            is ProductsError.StockNegative -> context.getString(R.string.error_stock_negative)
            is ProductsError.InvalidCategory -> context.getString(R.string.error_category_invalid)
            is ProductsError.InvalidBarcode -> context.getString(R.string.error_barcode_invalid)
            is ProductsError.InvalidDiscount -> context.getString(R.string.error_discount_invalid)
            is ProductsError.DiscountValueInvalid -> context.getString(R.string.error_discount_value_invalid)
            is ProductsError.DiscountPercentageExceeded -> context.getString(R.string.error_discount_percentage_exceeded)

            // Errores de base de datos
            is ProductsError.ProductNotFound -> context.getString(R.string.error_product_not_found)
            is ProductsError.DuplicateBarcode -> context.getString(R.string.error_duplicate_barcode, error.barcode)
            is ProductsError.DatabaseError -> context.getString(R.string.error_database)
            is ProductsError.UnauthorizedAccess -> context.getString(R.string.error_unauthorized)

            // Errores de operaciones
            is ProductsError.CreateProductFailed -> context.getString(R.string.error_create_product_failed)
            is ProductsError.UpdateProductFailed -> context.getString(R.string.error_update_product_failed)
            is ProductsError.DeleteProductFailed -> context.getString(R.string.error_delete_product_failed)
            is ProductsError.LoadProductsFailed -> context.getString(R.string.error_load_products_failed)
            is ProductsError.SearchProductsFailed -> context.getString(R.string.error_search_products_failed)

            // Errores de storage
            is ProductsError.ImageUploadFailed -> context.getString(R.string.error_image_upload_failed)
            is ProductsError.BarcodeGenerationFailed -> context.getString(R.string.error_barcode_generation_failed)
            is ProductsError.StorageError -> context.getString(R.string.error_storage)

            // Errores de stock
            is ProductsError.InsufficientStock -> context.getString(R.string.error_insufficient_stock)
            is ProductsError.StockUpdateFailed -> context.getString(R.string.error_stock_update_failed)

            // Error genérico
            is ProductsError.UnknownError -> {
                if (error.message.isNullOrBlank()) {
                    context.getString(R.string.error_unknown_simple)
                } else {
                    context.getString(R.string.error_unknown, error.message)
                }
            }
        }
    }

    // Función para obtener mensaje de éxito
    fun getSuccessMessage(messageKey: String): String {
        return when (messageKey) {
            "product_created_success" -> context.getString(R.string.product_created_success)
            "product_updated_success" -> context.getString(R.string.product_updated_success)
            else -> messageKey
        }
    }

    // Cargar producto al editar
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.handleIntent(ProductsIntent.LoadProductById(productId))
        }
    }

    // Actualizar campos cuando se carga el producto
    LaunchedEffect(state.selectedProduct) {
        state.selectedProduct?.let { product ->
            name = product.name
            description = product.description.orEmpty()
            price = String.format("%.2f", product.price)
            currentStock = product.currentStock.toString()
            minimumStock = product.minimumStock.toString()
            categoryId = product.categoryId
            discountType = product.discountType.toDiscountType()
            discountValue = if (product.discountValue > 0) product.discountValue.toString() else ""
        }
    }

    // Mensajes
    LaunchedEffect(state.successMessage, state.error) {
        state.successMessage?.let { messageKey ->
            snackbarHostState.showSnackbar(getSuccessMessage(messageKey))
            viewModel.clearMessages()
            onNavigateBack()
        }
        state.error?.let { error ->
            snackbarHostState.showSnackbar(getErrorMessage(error))
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEditMode) stringResource(R.string.product_edit)
                        else stringResource(R.string.product_new),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Nombre
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    viewModel.handleIntent(ProductsIntent.ValidateProductName(it))
                },
                label = { Text(stringResource(R.string.product_name_required)) },
                leadingIcon = { Icon(Icons.Default.Inventory, null) },
                singleLine = true,
                isError = name.isNotBlank() && name.length < 2,
                supportingText = {
                    if (name.isNotBlank() && name.length < 2) {
                        Text(stringResource(R.string.error_product_name_too_short))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Descripción
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.product_description_optional)) },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Precio
            OutlinedTextField(
                value = price,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("\\d*\\.?\\d{0,2}"))) {
                        price = newValue
                        newValue.toDoubleOrNull()?.let { p ->
                            viewModel.handleIntent(ProductsIntent.ValidatePrice(p))
                        }
                    }
                },
                label = { Text(stringResource(R.string.product_price_required)) },
                leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                singleLine = true,
                isError = price.toDoubleOrNull()?.let { it <= 0 } == true,
                supportingText = {
                    when {
                        price.toDoubleOrNull()?.let { it <= 0 } == true -> {
                            Text(
                                stringResource(R.string.error_price_zero_or_negative),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        discountType != DiscountType.NONE && price.toDoubleOrNull() ?: 0.0 > 0 -> {
                            Text(
                                stringResource(R.string.product_price_final, String.format("%.2f", finalPrice)),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Descuento
            OutlinedCard(onClick = { showDiscountPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.product_discount), fontWeight = FontWeight.Medium)
                        Text(
                            when (discountType) {
                                DiscountType.NONE -> stringResource(R.string.discount_none)
                                DiscountType.PERCENT -> stringResource(R.string.discount_percent_value, discountValue)
                                DiscountType.FIXED -> stringResource(R.string.discount_fixed_value, discountValue)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }

            // Stock actual
            OutlinedTextField(
                value = currentStock,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("\\d+"))) {
                        currentStock = it
                        it.toIntOrNull()?.let { stock ->
                            viewModel.handleIntent(ProductsIntent.ValidateStock(stock))
                        }
                    }
                },
                label = { Text(stringResource(R.string.product_stock_current_required)) },
                leadingIcon = { Icon(Icons.Default.Inventory2, null) },
                singleLine = true,
                isError = currentStock.toIntOrNull()?.let { it < 0 } == true,
                supportingText = {
                    if (currentStock.toIntOrNull()?.let { it < 0 } == true) {
                        Text(stringResource(R.string.error_stock_negative))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Stock mínimo
            OutlinedTextField(
                value = minimumStock,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("\\d+"))) minimumStock = it
                },
                label = { Text(stringResource(R.string.product_stock_minimum_required)) },
                leadingIcon = { Icon(Icons.Default.Warning, null) },
                singleLine = true,
                isError = minimumStock.toIntOrNull()?.let { it < 0 } == true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Categoría
            OutlinedCard(onClick = { showCategoryPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        categories.find { it.categoryId == categoryId }?.name
                            ?: stringResource(R.string.product_select_category),
                        color = if (categoryId == null)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else LocalContentColor.current
                    )
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }

            // Código de barras (solo en edición)
            if (isEditMode) {
                state.selectedProduct?.barcode?.let { barcode ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.product_barcode),
                                fontWeight = FontWeight.Bold
                            )
                            Text(barcode, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Botones
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        val precio = price.toDoubleOrNull() ?: 0.0
                        val stock = currentStock.toIntOrNull() ?: 0
                        val minStock = minimumStock.toIntOrNull() ?: 0
                        val catId = categoryId ?: return@Button

                        if (precio <= 0 || name.isBlank() || name.length < 2 || catId <= 0) return@Button

                        if (isEditMode && productId != null) {
                            viewModel.handleIntent(
                                ProductsIntent.UpdateProduct(
                                    productId = productId,
                                    name = name,
                                    description = description.ifBlank { null },
                                    price = precio,
                                    barcode = null,
                                    categoryId = catId,
                                    image = null,
                                    currentStock = stock,
                                    minimumStock = minStock
                                )
                            )
                        } else {
                            viewModel.handleIntent(
                                ProductsIntent.CreateProduct(
                                    name = name,
                                    description = description.ifBlank { null },
                                    price = precio,
                                    barcode = null,
                                    categoryId = catId,
                                    image = null,
                                    currentStock = stock,
                                    minimumStock = minStock
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank() &&
                            name.length >= 2 &&
                            (price.toDoubleOrNull() ?: 0.0) > 0 &&
                            (currentStock.toIntOrNull() ?: -1) >= 0 &&
                            (minimumStock.toIntOrNull() ?: -1) >= 0 &&
                            categoryId != null &&
                            !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            if (isEditMode) stringResource(R.string.update_text)
                            else stringResource(R.string.create_text)
                        )
                    }
                }
            }
        }
    }

    // Diálogos
    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = categories,
            selectedCategoryId = categoryId,
            onSelect = { categoryId = it; showCategoryPicker = false },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showDiscountPicker) {
        DiscountPickerDialog(
            currentType = discountType,
            currentValue = discountValue.toDoubleOrNull() ?: 0.0,
            onConfirm = { type, value ->
                discountType = type
                discountValue = if (value > 0) value.toString() else ""
                showDiscountPicker = false
            },
            onDismiss = { showDiscountPicker = false }
        )
    }
}
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

    // Precio final
    val finalPrice = remember(price, discountType, discountValue) {
        val p = price.toDoubleOrNull() ?: 0.0
        val d = discountValue.toDoubleOrNull() ?: 0.0
        when (discountType) {
            DiscountType.NONE -> p
            DiscountType.PERCENT -> p * (1 - d / 100)
            DiscountType.FIXED -> (p - d).coerceAtLeast(0.0)
        }
    }

    // Cargar producto
    LaunchedEffect(productId) {
        productId?.let {
            viewModel.handleIntent(ProductsIntent.LoadProductById(it))
        }
    }

    // Setear datos al editar
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
        state.successMessage?.let {
            snackbarHostState.showSnackbar(
                if (it == "product_created_success")
                    context.getString(R.string.product_created_success)
                else
                    context.getString(R.string.product_updated_success)
            )
            onNavigateBack()
        }

        state.error?.let {
            snackbarHostState.showSnackbar(context.getString(R.string.error_unknown_simple))
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
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.product_name_required)) },
                leadingIcon = { Icon(Icons.Default.Inventory, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.product_description_optional)) },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(stringResource(R.string.product_price_required)) },
                leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    if (discountType != DiscountType.NONE && price.toDoubleOrNull() != null) {
                        Text(
                            stringResource(
                                R.string.product_price_final,
                                String.format("%.2f", finalPrice)
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            OutlinedCard(onClick = { showDiscountPicker = true }) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.product_discount))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }

            OutlinedTextField(
                value = currentStock,
                onValueChange = { currentStock = it },
                label = { Text(stringResource(R.string.product_stock_current_required)) },
                leadingIcon = { Icon(Icons.Default.Inventory2, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = minimumStock,
                onValueChange = { minimumStock = it },
                label = { Text(stringResource(R.string.product_stock_minimum_required)) },
                leadingIcon = { Icon(Icons.Default.Warning, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedCard(onClick = { showCategoryPicker = true }) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        categories.find { it.categoryId == categoryId }?.name
                            ?: stringResource(R.string.product_select_category)
                    )
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                onClick = {
                    val p = price.toDoubleOrNull() ?: return@Button
                    val stock = currentStock.toIntOrNull() ?: 0
                    val minStock = minimumStock.toIntOrNull() ?: 0
                    val cat = categoryId ?: return@Button

                    if (isEditMode && productId != null) {
                        viewModel.handleIntent(
                            ProductsIntent.UpdateProduct(
                                productId,
                                name,
                                description.ifBlank { null },
                                p,
                                null,
                                cat,
                                null,
                                stock,
                                minStock
                            )
                        )
                    } else {
                        viewModel.handleIntent(
                            ProductsIntent.CreateProduct(
                                name,
                                description.ifBlank { null },
                                p,
                                null,
                                cat,
                                null,
                                stock,
                                minStock
                            )
                        )
                    }
                }
            ) {
                Text(if (isEditMode) stringResource(R.string.update_text) else stringResource(R.string.create_text))
            }
        }
    }

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

    if (showDiscountPicker) {
        DiscountPickerDialog(
            currentType = discountType,
            currentValue = discountValue.toDoubleOrNull() ?: 0.0,
            onConfirm = { type, value ->
                discountType = type
                discountValue = value.toString()
                showDiscountPicker = false
            },
            onDismiss = { showDiscountPicker = false }
        )
    }
}

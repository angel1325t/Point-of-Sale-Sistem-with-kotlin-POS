package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.products.ProductsIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.Category
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.DiscountType
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.utils.toDiscountType
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.components.CategoryPickerDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.components.DiscountPickerDialog
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

    // ✅ CORRECCIÓN: Declarar todos los strings ANTES de LaunchedEffect
    val productCreatedMessage = stringResource(R.string.product_created_success)
    val productUpdatedMessage = stringResource(R.string.product_updated_success)
    val errorMessage = stringResource(R.string.error_unknown_simple)
    val productEditTitle = stringResource(R.string.product_edit)
    val productNewTitle = stringResource(R.string.product_new)
    val productNameLabel = stringResource(R.string.product_name_required)
    val productDescriptionLabel = stringResource(R.string.product_description_optional)
    val productPriceLabel = stringResource(R.string.product_price_required)
    val productDiscountLabel = stringResource(R.string.product_discount)
    val productStockCurrentLabel = stringResource(R.string.product_stock_current_required)
    val productStockMinimumLabel = stringResource(R.string.product_stock_minimum_required)
    val productSelectCategoryLabel = stringResource(R.string.product_select_category)
    val updateButtonText = stringResource(R.string.update_text)
    val createButtonText = stringResource(R.string.create_text)

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

    // ✅ CORRECCIÓN: Usar las variables declaradas arriba
    LaunchedEffect(state.successMessage, state.error) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(
                if (it == "product_created_success")
                    productCreatedMessage
                else
                    productUpdatedMessage
            )
            onNavigateBack()
        }

        state.error?.let {
            snackbarHostState.showSnackbar(errorMessage)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEditMode) productEditTitle else productNewTitle,
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
                label = { Text(productNameLabel) },
                leadingIcon = { Icon(Icons.Default.Inventory, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(productDescriptionLabel) },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(productPriceLabel) },
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
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(productDiscountLabel)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }

            OutlinedTextField(
                value = currentStock,
                onValueChange = { currentStock = it },
                label = { Text(productStockCurrentLabel) },
                leadingIcon = { Icon(Icons.Default.Inventory2, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = minimumStock,
                onValueChange = { minimumStock = it },
                label = { Text(productStockMinimumLabel) },
                leadingIcon = { Icon(Icons.Default.Warning, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedCard(onClick = { showCategoryPicker = true }) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        categories.find { it.categoryId == categoryId }?.name
                            ?: productSelectCategoryLabel
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
                Text(if (isEditMode) updateButtonText else createButtonText)
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
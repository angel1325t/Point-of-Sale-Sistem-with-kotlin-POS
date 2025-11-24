package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.categories

import androidx.compose.foundation.clickable
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
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.categories.CategoryIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.Category
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormScreen(
    viewModel: CategoryViewModel,
    categoryId: Int?,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isEditMode = categoryId != null

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<Int?>(null) }
    var showParentSelector by remember { mutableStateOf(false) }

    // Cargar categoría si es modo edición
    LaunchedEffect(categoryId) {
        if (categoryId != null) {
            viewModel.handleIntent(CategoryIntent.LoadCategoryById(categoryId))
        }
        viewModel.handleIntent(CategoryIntent.LoadCategories)
    }

    // Actualizar campos cuando se carga la categoría
    LaunchedEffect(state.selectedCategory) {
        state.selectedCategory?.let { category ->
            name = category.name
            description = category.description ?: ""
            selectedParentId = category.parentId
        }
    }

    // Navegar atrás cuando la operación es exitosa
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
                        if (isEditMode) "Editar Categoría" else "Nueva Categoría",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && isEditMode) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Campo de nombre
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            viewModel.handleIntent(CategoryIntent.ValidateCategoryName(it))
                        },
                        label = { Text("Nombre *") },
                        placeholder = { Text("Ej: Electrónica") },
                        leadingIcon = {
                            Icon(Icons.Default.Category, "Nombre")
                        },
                        isError = state.nameError != null,
                        supportingText = {
                            if (state.nameError != null) {
                                Text(
                                    text = state.nameError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Campo de descripción
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        placeholder = { Text("Descripción opcional de la categoría") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, "Descripción")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Selector de categoría padre
                    ParentCategorySelector(
                        selectedParentId = selectedParentId,
                        selectedParentName = state.categories.find { it.categoryId == selectedParentId }?.name,
                        currentCategoryId = categoryId,
                        onClick = { showParentSelector = true },
                        onClear = { selectedParentId = null }
                    )

                    // Información de ayuda
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Información",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "• Los campos marcados con * son obligatorios\n" +
                                            "• Puedes asignar una categoría padre para crear subcategorías\n" +
                                            "• El nombre debe ser único",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de acción
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
                                if (isEditMode && categoryId != null) {
                                    viewModel.handleIntent(
                                        CategoryIntent.UpdateCategory(
                                            categoryId = categoryId,
                                            name = name,
                                            description = description.ifBlank { null },
                                            parentId = selectedParentId
                                        )
                                    )
                                } else {
                                    viewModel.handleIntent(
                                        CategoryIntent.CreateCategory(
                                            name = name,
                                            description = description.ifBlank { null },
                                            parentId = selectedParentId
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = name.isNotBlank() && !state.isLoading && state.nameError == null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(if (isEditMode) "Actualizar" else "Crear")
                            }
                        }
                    }

                    // Mensaje de error
                    if (state.error != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = state.error!!.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo selector de categoría padre
    if (showParentSelector) {
        ParentSelectorDialog(
            categories = state.categories.filter { it.categoryId != categoryId },
            selectedParentId = selectedParentId,
            onSelectParent = { parentId ->
                selectedParentId = parentId
                showParentSelector = false
            },
            onDismiss = { showParentSelector = false }
        )
    }
}

@Composable
private fun ParentCategorySelector(
    selectedParentId: Int?,
    selectedParentName: String?,
    currentCategoryId: Int?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Categoría Padre (Opcional)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (selectedParentId != null) Icons.Default.Folder else Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedParentName ?: "Ninguna (categoría raíz)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedParentId != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (selectedParentId != null) {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Seleccionar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (currentCategoryId != null) {
            Text(
                text = "No puedes seleccionar la categoría actual como padre",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentSelectorDialog(
    categories: List<Category>,
    selectedParentId: Int?,
    onSelectParent: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Seleccionar Categoría Padre",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Opción para ninguna categoría padre (raíz)
                Card(
                    onClick = {
                        onSelectParent(null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedParentId == null)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = if (selectedParentId == null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Ninguna (Raíz)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedParentId == null)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                            )
                            Text(
                                "Categoría de nivel superior",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedParentId == null) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Seleccionado",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lista de categorías
                if (categories.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "No hay otras categorías disponibles",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            CategoryItem(
                                category = category,
                                isSelected = category.categoryId == selectedParentId,
                                onClick = {
                                    onSelectParent(category.categoryId)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cerrar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected)
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.primary
                )
            )
        else
            CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                category.description?.let { desc ->
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
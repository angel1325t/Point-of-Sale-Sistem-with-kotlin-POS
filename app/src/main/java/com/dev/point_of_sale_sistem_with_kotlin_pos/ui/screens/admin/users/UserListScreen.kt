package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.users

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.users.UserIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.UserError
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.permissions.PermissionChecker
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListScreen(
    navController: NavHostController,
    viewModel: UserViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val state = viewModel.state
    var showDeleteDialog by remember { mutableStateOf<UserRepository.UserModel?>(null) }
    val context = LocalContext.current

    val canViewUsers = PermissionChecker.canViewUsers()
    val canCreateUsers = PermissionChecker.canCreateUsers()
    val canUpdateUsers = PermissionChecker.canUpdateUsers()
    val canDeleteUsers = PermissionChecker.canDeleteUsers()

    if (!canViewUsers) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.permission_denied), color = MaterialTheme.colorScheme.error)
        }
        return
    }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(UserIntent.LoadUsers)
    }

    // Mostrar mensajes
    LaunchedEffect(state.error, state.successMessage) {
        if (state.error != null || state.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }
    val successText = when (state.successMessage) {
        "SUCCESS_CREATE_USER" -> stringResource(R.string.success_create_user)
        "SUCCESS_UPDATE_USER" -> stringResource(R.string.success_update_user)
        "SUCCESS_DELETE_USER" -> stringResource(R.string.success_delete_user)
        else -> state.successMessage?.let { stringResource(R.string.success_generic) }
    }

    LaunchedEffect(state.successMessage) {
        successText?.let {
            Toast.makeText(
                context,
                it,
                Toast.LENGTH_SHORT
            ).show()

            viewModel.clearMessages()
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_user_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (canCreateUsers) {
                FloatingActionButton(onClick = onNavigateToCreate) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_user_description))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (state.error) {
                                UserError.ConnectionError -> context.getString(R.string.conection_error)
                                UserError.UserNotFound -> stringResource(R.string.user_not_found)
                                UserError.EmailAlreadyExists -> stringResource(R.string.email_already_exists)
                                UserError.AuthError -> stringResource(R.string.supabase_auth_error)
                                is UserError.Other -> state.error.message ?: context.getString(R.string.unknown_error)
                            },
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                state.users.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.no_user_registrated))
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.users) { user ->
                            UserCard(
                                user = user,
                                onEdit = {
                                    if (canUpdateUsers) {
                                        onNavigateToEdit(user.auth_id.toString())
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDelete = {
                                    if (canDeleteUsers) {
                                        showDeleteDialog = user
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Snackbar para mensajes de éxito
            state.successMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(message)
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    showDeleteDialog?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_user_deletion, user.username)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleIntent(UserIntent.DeleteUser(user.auth_id.toString()))
                        showDeleteDialog = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun UserCard(
    user: UserRepository.UserModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.username, style = MaterialTheme.typography.titleMedium)

                Text(
                    text = "Estado: ${if (user.active == true) stringResource(R.string.active) else stringResource(
                        R.string.inactive
                    )}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.active == true)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit),tint = Color(0xFF4CAF50))
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

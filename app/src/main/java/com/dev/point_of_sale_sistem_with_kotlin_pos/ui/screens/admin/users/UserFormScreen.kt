    package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.users

    import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.users.UserIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.UserError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.UserViewModel
import java.util.UUID

    @SuppressLint("LocalContextGetResourceValueCall")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UserFormScreen(
        viewModel: UserViewModel,
        userId: String? = null,
        onNavigateBack: () -> Unit
    ) {
        val state = viewModel.state
        val isEditMode = userId != null
        val context = LocalContext.current


        var email by remember { mutableStateOf("") }
        var selectedRoleId by remember { mutableStateOf<Int?>(null) }
        var selectedRoleName by remember { mutableStateOf("") } // 🔥 NUEVO
        var selectedBranchId by remember { mutableStateOf<String?>(null) }

        // 🔥 Estados de error
        var emailError by remember { mutableStateOf<String?>(null) }
        var roleError by remember { mutableStateOf<String?>(null) }
        var branchError by remember { mutableStateOf<String?>(null) }

        var expandedRole by remember { mutableStateOf(false) }
        var expandedBranch by remember { mutableStateOf(false) }
        var loadedUserId by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(userId) {
            if (userId == null) {
                // Solo limpiar si NO estamos en modo editar
                viewModel.clearSelectedUser()
                loadedUserId = null
                email = ""
                selectedRoleId = null
                selectedBranchId = null
                emailError = null
                roleError = null
                branchError = null
            }
        }
        LaunchedEffect(Unit) {
            viewModel.handleIntent(UserIntent.LoadRoles)
            viewModel.handleIntent(UserIntent.LoadBranches)

            if (isEditMode) {
                viewModel.handleIntent(UserIntent.LoadUser(userId!!))
            }
        }

        LaunchedEffect(state.selectedUser, state.roles) {
            val user = state.selectedUser ?: return@LaunchedEffect
            if (state.roles.isEmpty()) return@LaunchedEffect

            if (loadedUserId != user.auth_id.toString()) {
                email = user.email ?: ""

                val role = state.roles.find { it.role_id == user.role_id }
                selectedRoleId = role?.role_id
                selectedRoleName = role?.name ?: ""

                selectedBranchId = user.branch_id?.toString()
                loadedUserId = user.auth_id.toString()
            }
        }



        LaunchedEffect(state.successMessage) {
            state.successMessage?.let { msg ->
                val messageText = when (msg) {
                    "SUCCESS_CREATE_USER" ->
                        context.getString(R.string.success_create_user)
                    "SUCCESS_UPDATE_USER" ->
                        context.getString(R.string.success_update_user)
                    else ->
                        context.getString(R.string.success_generic)
                }

                Toast.makeText(context, messageText, Toast.LENGTH_SHORT).show()

                viewModel.clearMessages()
                onNavigateBack()
            }
        }



        DisposableEffect(Unit) {
            onDispose {
                viewModel.clearSelectedUser()
                viewModel.clearMessages()
            }
        }

        fun validateFields(): Boolean {
            var isValid = true

            // Validación email (solo en modo creación)
            if (!isEditMode) {
                if (email.isBlank()) {
                    emailError = context.getString(R.string.email_required_validation)
                    isValid = false
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailError = context.getString(R.string.invalid_format)
                    isValid = false
                } else {
                    emailError = null
                }
            }

            // Validación rol
            if (selectedRoleId == null) {
                roleError = context.getString(R.string.role_validation)
                isValid = false
            } else {
                roleError = null
            }

            // Validación sucursal
            if (selectedBranchId == null) {
                branchError = context.getString(R.string.branch_validation)
                isValid = false
            } else {
                branchError = null
            }

            return isValid
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isEditMode) stringResource(R.string.edit_user_title) else stringResource(R.string.new_user_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.go_back))
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ----------------------------- EMAIL -----------------------------
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    label = { Text(stringResource(R.string.email)) },
                    isError = emailError != null,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEditMode
                )

                if (emailError != null) {
                    Text(emailError!!, color = MaterialTheme.colorScheme.error)
                }

                // ----------------------------- ROL -----------------------------
                ExposedDropdownMenuBox(
                    expanded = expandedRole,
                    onExpandedChange = { expandedRole = !expandedRole }
                ) {
                    OutlinedTextField(
                        value = selectedRoleName, // 🔥 YA NO usa find()
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.role)) },
                        isError = roleError != null,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRole)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedRole,
                        onDismissRequest = { expandedRole = false }
                    ) {
                        state.roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = {
                                    selectedRoleId = role.role_id
                                    selectedRoleName = role.name // 🔥 CLAVE
                                    roleError = null
                                    expandedRole = false
                                }
                            )
                        }
                    }
                }


                if (roleError != null) {
                    Text(roleError!!, color = MaterialTheme.colorScheme.error)
                }

                // ----------------------------- SUCURSAL -----------------------------
                ExposedDropdownMenuBox(
                    expanded = expandedBranch,
                    onExpandedChange = { expandedBranch = !expandedBranch }
                ) {
                    OutlinedTextField(
                        value = state.branches.find { it.branch_id?.toString() == selectedBranchId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.branch)) },
                        isError = branchError != null,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBranch)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedBranch,
                        onDismissRequest = { expandedBranch = false }
                    ) {
                        state.branches.forEach { branch ->
                            DropdownMenuItem(
                                text = { Text(branch.name) },
                                onClick = {
                                    selectedBranchId = branch.branch_id?.toString()
                                    branchError = null
                                    expandedBranch = false
                                }
                            )
                        }
                    }
                }

                if (branchError != null) {
                    Text(branchError!!, color = MaterialTheme.colorScheme.error)
                }

                // ----------------------------- BOTÓN GUARDAR -----------------------------
                Button(
                    onClick = {
                        if (!validateFields()) return@Button

                        if (isEditMode) {
                            viewModel.handleIntent(
                                UserIntent.UpdateUser(
                                    authId = loadedUserId!!,
                                    email = email,
                                    branchId = selectedBranchId?.let { UUID.fromString(it) },
                                    roleId = selectedRoleId
                                )
                            )
                        } else {
                            viewModel.handleIntent(
                                UserIntent.CreateUser(
                                    email = email,
                                    roleId = selectedRoleId!!,
                                    branchId = UUID.fromString(selectedBranchId!!)
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    Text(if (isEditMode) stringResource(R.string.update_text) else stringResource(R.string.create_text))
                }

                // ----------------------------- ERRORES DE BACKEND -----------------------------
                state.error?.let { error ->
                    Text(
                        text = when (error) {
                            UserError.ConnectionError -> context.getString(R.string.conection_error)
                            UserError.UserNotFound -> stringResource(R.string.user_not_found)
                            UserError.EmailAlreadyExists -> stringResource(R.string.email_already_exists)
                            UserError.AuthError -> stringResource(R.string.supabase_auth_error)
                            is UserError.Other -> error.message ?: context.getString(R.string.unknown_error)
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

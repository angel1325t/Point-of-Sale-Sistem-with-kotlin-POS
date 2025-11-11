package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users

import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository

data class UserState(
    val isLoading: Boolean = false,
    val users: List<UserRepository.UserModel> = emptyList(),
    val selectedUser: UserRepository.UserModel? = null,
    val roles: List<UserRepository.RoleModel> = emptyList(),
    val branches: List<UserRepository.BranchModel> = emptyList(),
    val error: UserError? = null,
    val successMessage: String? = null
)
package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.users

import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import java.util.UUID

sealed class UserIntent {
    object LoadUsers : UserIntent()
    data class LoadUser(val id: String) : UserIntent()
    data class CreateUser(val email: String, val roleId: Int, val branchId: UUID) : UserIntent()
    data class UpdateUser(val userId: String, val email: String?,val branchId: UUID?, val roleId: Int?) : UserIntent()
    data class DeleteUser(val id: String) : UserIntent()
    object LoadRoles : UserIntent()
    object LoadBranches : UserIntent()

}
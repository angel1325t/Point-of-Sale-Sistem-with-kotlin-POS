package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.braches

sealed class BranchIntent {
    object LoadBranches : BranchIntent()
    data class CreateBranch(
        val alias: String,
        val address: String,
        val phone: String,
        val city: String
    ) : BranchIntent()
    data class UpdateBranch(
        val branchId: String,
        val alias: String,
        val address: String,
        val phone: String,
        val city: String
    ) : BranchIntent()
    data class DeleteBranch(val branchId: String) : BranchIntent()
    data class ToggleBranchStatus(val branchId: String, val active: Boolean) : BranchIntent()
    object ClearError : BranchIntent()
}
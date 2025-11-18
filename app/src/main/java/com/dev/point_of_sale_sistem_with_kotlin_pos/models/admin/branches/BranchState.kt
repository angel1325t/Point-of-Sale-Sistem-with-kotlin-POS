package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches
import kotlinx.serialization.Serializable

@Serializable
data class Branch(
    val branchId: String,
    val companyId: String,
    val name: String,
    val address: String?,
    val phone: String?,
    val city: String?,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class BranchState(
    val branches: List<Branch> = emptyList(),
    val isLoading: Boolean = false,
    val error: BranchError? = null,
    val successMessage: String? = null
)
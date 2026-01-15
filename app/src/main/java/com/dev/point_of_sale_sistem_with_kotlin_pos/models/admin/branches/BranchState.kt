package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Branch(
    @SerialName("branch_id")
    val branchId: String,
    @SerialName("company_id")
    val companyId: String,
    val name: String,
    val address: String?,
    val phone: String?,
    val city: String?,
    val active: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

data class BranchState(
    val branches: List<Branch> = emptyList(),
    val isLoading: Boolean = false,
    val error: BranchError? = null,
    val successMessage: String? = null
)
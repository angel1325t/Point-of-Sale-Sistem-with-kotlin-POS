package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches


sealed class BranchError {
    data class NetworkError(val message: String) : BranchError()
    data class ValidationError(val message: String) : BranchError()
    data class DatabaseError(val message: String) : BranchError()
    object UnauthorizedError : BranchError()
    object BranchNotFound : BranchError()
    data class UnknownError(val message: String) : BranchError()
}
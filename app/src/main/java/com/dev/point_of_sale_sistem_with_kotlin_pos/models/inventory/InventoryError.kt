package com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory

import androidx.annotation.StringRes
import com.dev.point_of_sale_sistem_with_kotlin_pos.R

sealed class InventoryError(
    @StringRes val messageRes: Int,
    override val cause: Throwable? = null,
    open val code: String? = null
) : Exception(null, cause) {

    data object NetworkError : InventoryError(
        messageRes = R.string.error_network,
        code = "NETWORK_ERROR"
    )

    data object TimeoutError : InventoryError(
        messageRes = R.string.error_timeout,
        code = "TIMEOUT_ERROR"
    )

    data object NotFound : InventoryError(
        messageRes = R.string.error_inventory_not_found,
        code = "NOT_FOUND"
    )

    data object NotEnoughStock : InventoryError(
        messageRes = R.string.error_not_enough_stock,
        code = "NOT_ENOUGH_STOCK"
    )

    data class ValidationError(
        @StringRes val validationMessageRes: Int,
        val field: String? = null
    ) : InventoryError(
        messageRes = validationMessageRes,
        code = "VALIDATION_ERROR"
    )

    data class UnknownError(
        val exception: Throwable? = null
    ) : InventoryError(
        messageRes = R.string.error_unknown,
        cause = exception,
        code = "UNKNOWN_ERROR"
    )
}
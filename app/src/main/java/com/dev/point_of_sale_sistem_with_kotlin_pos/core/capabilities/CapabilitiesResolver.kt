package com.dev.point_of_sale_sistem_with_kotlin_pos.core.capabilities

object CapabilitiesResolver {

    fun resolve(
        isOffline: Boolean
    ): CapabilitiesState {

        return if (isOffline) {
            CapabilitiesState(
                enabled = setOf(
                    AppCapability.CREATE_SALE,
                    AppCapability.VIEW_PRODUCTS,
                    AppCapability.UPDATE_STOCK,
                    AppCapability.OPEN_CASH_REGISTER,
                    AppCapability.CLOSE_CASH_REGISTER,
                    AppCapability.VIEW_CASH_REGISTER_HISTORY
                )
            )
        } else {
            CapabilitiesState(
                enabled = setOf(
                    AppCapability.CREATE_SALE,
                    AppCapability.VIEW_PRODUCTS,
                    AppCapability.UPDATE_STOCK,
                    AppCapability.OPEN_CASH_REGISTER,
                    AppCapability.CLOSE_CASH_REGISTER,
                    AppCapability.VIEW_CASH_REGISTER_HISTORY,
                    AppCapability.CARD_PAYMENT,
                    AppCapability.TRANSFER_PAYMENT,
                    AppCapability.CREDIT_NOTE,
                    AppCapability.REPORTS,
                    AppCapability.ADMIN_FUNCTIONS
                )
            )
        }
    }
}

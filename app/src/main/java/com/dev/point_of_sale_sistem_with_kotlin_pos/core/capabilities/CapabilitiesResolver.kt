package com.dev.point_of_sale_sistem_with_kotlin_pos.core.capabilities

object CapabilitiesResolver {

    fun resolve(
        isOffline: Boolean
    ): CapabilitiesState {

        return if (isOffline) {
            // 📴 OFFLINE: solo ventas + inventario
            CapabilitiesState(
                enabled = setOf(
                    AppCapability.CREATE_SALE,
                    AppCapability.VIEW_PRODUCTS,
                    AppCapability.UPDATE_STOCK
                )
            )
        } else {
            // 🌐 ONLINE: por ahora permitimos lo mismo
            // (luego aquí entran los permisos)
            CapabilitiesState(
                enabled = setOf(
                    AppCapability.CREATE_SALE,
                    AppCapability.VIEW_PRODUCTS,
                    AppCapability.UPDATE_STOCK
                )
            )
        }
    }
}

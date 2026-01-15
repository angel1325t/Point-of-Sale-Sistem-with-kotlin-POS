package com.dev.point_of_sale_sistem_with_kotlin_pos.core.capabilities

data class CapabilitiesState(
    val enabled: Set<AppCapability> = emptySet()
) {
    fun can(capability: AppCapability): Boolean {
        return enabled.contains(capability)
    }
}

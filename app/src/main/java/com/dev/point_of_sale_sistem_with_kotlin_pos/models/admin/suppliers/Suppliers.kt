package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers

/**
 * Modelo de dominio para Supplier
 * Este es el modelo que usas en tu UI y lógica de negocio
 */
data class Supplier(
    val supplierId: Int,
    val name: String,
    val contact: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null
) {
    /**
     * Valida si el proveedor tiene información de contacto completa
     */
    val hasCompleteContactInfo: Boolean
        get() = !contact.isNullOrBlank() &&
                (!phone.isNullOrBlank() || !email.isNullOrBlank())

    /**
     * Obtiene un resumen de la información del proveedor
     */
    fun getContactSummary(): String {
        val parts = mutableListOf<String>()

        contact?.let { parts.add(it) }
        phone?.let { parts.add("Tel: $it") }
        email?.let { parts.add(it) }

        return parts.joinToString(" • ")
    }
}
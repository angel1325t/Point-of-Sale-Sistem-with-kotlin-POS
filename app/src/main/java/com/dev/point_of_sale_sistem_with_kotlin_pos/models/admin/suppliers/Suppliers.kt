package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers

/**
 * Modelo de dominio para Supplier
 * Este es el modelo que usas en tu UI y lógica de negocio
 */
data class Supplier(
    val supplierId: Int = 0,
    val name: String,
    val contact: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    /**
     * Obtiene un resumen de la información del proveedor
     */

    fun hasCompleteContact(): Boolean {
        return !contact.isNullOrBlank() &&
                !phone.isNullOrBlank() &&
                !email.isNullOrBlank()
    }
    fun getContactSummary(): String {
        val parts = mutableListOf<String>()

        contact?.let { parts.add(it) }
        phone?.let { parts.add("Tel: $it") }
        email?.let { parts.add(it) }

        return parts.joinToString(" • ")
    }

    fun matchesSearch(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return name.lowercase().contains(lowerQuery) ||
                contact?.lowercase()?.contains(lowerQuery) == true ||
                phone?.contains(query) == true ||
                email?.lowercase()?.contains(lowerQuery) == true
    }
}
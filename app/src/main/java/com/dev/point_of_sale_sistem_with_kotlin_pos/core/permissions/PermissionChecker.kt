package com.dev.point_of_sale_sistem_with_kotlin_pos.core.permissions

/**
 * Wrapper estático para facilitar el acceso a permisos en Composables y otras clases.
 * Delega todas las verificaciones a PermissionManager.
 */
object PermissionChecker {

    // PRODUCTS
    fun canViewProducts(): Boolean = PermissionManager.hasPermission("view_products")
    fun canCreateProducts(): Boolean = PermissionManager.hasPermission("create_products")
    fun canUpdateProducts(): Boolean = PermissionManager.hasPermission("update_products")
    fun canDeleteProducts(): Boolean = PermissionManager.hasPermission("delete_products")

    // CATEGORIES
    fun canViewCategories(): Boolean = PermissionManager.hasPermission("view_categories")
    fun canCreateCategories(): Boolean = PermissionManager.hasPermission("create_categories")
    fun canUpdateCategories(): Boolean = PermissionManager.hasPermission("update_categories")
    fun canDeleteCategories(): Boolean = PermissionManager.hasPermission("delete_categories")

    // SUPPLIERS
    fun canViewSuppliers(): Boolean = PermissionManager.hasPermission("view_suppliers")
    fun canCreateSuppliers(): Boolean = PermissionManager.hasPermission("create_suppliers")
    fun canUpdateSuppliers(): Boolean = PermissionManager.hasPermission("update_suppliers")
    fun canDeleteSuppliers(): Boolean = PermissionManager.hasPermission("delete_suppliers")

    // BRANCHES
    fun canViewBranches(): Boolean = PermissionManager.hasPermission("view_branches")
    fun canCreateBranches(): Boolean = PermissionManager.hasPermission("create_branches")
    fun canUpdateBranches(): Boolean = PermissionManager.hasPermission("update_branches")
    fun canDeleteBranches(): Boolean = PermissionManager.hasPermission("delete_branches")

    // USERS
    fun canViewUsers(): Boolean = PermissionManager.hasPermission("view_users")
    fun canCreateUsers(): Boolean = PermissionManager.hasPermission("create_users")
    fun canUpdateUsers(): Boolean = PermissionManager.hasPermission("update_users")
    fun canDeleteUsers(): Boolean = PermissionManager.hasPermission("delete_users")

    // ROLES
    fun canViewRoles(): Boolean = PermissionManager.hasPermission("view_roles")
    fun canCreateRoles(): Boolean = PermissionManager.hasPermission("create_roles")
    fun canUpdateRoles(): Boolean = PermissionManager.hasPermission("update_roles")
    fun canDeleteRoles(): Boolean = PermissionManager.hasPermission("delete_roles")

    // INVENTORY
    fun canViewInventory(): Boolean = PermissionManager.hasPermission("view_inventory")
    fun canUpdateStock(): Boolean = PermissionManager.hasPermission("update_stock")

    // SALES
    fun canViewSales(): Boolean = PermissionManager.hasPermission("view_sales")
    fun canCreateSales(): Boolean = PermissionManager.hasPermission("create_sales")
    fun canCancelSales(): Boolean = PermissionManager.hasPermission("cancel_sales")

    // PURCHASES
    fun canViewPurchases(): Boolean = PermissionManager.hasPermission("view_purchases")
    fun canCreatePurchases(): Boolean = PermissionManager.hasPermission("create_purchases")
    fun canCancelPurchases(): Boolean = PermissionManager.hasPermission("cancel_purchases")

    // RETURNS
    fun canViewReturns(): Boolean = PermissionManager.hasPermission("view_returns")
    fun canCreateReturns(): Boolean = PermissionManager.hasPermission("create_returns")

    // REPORTS
    fun canViewReports(): Boolean = PermissionManager.hasPermission("view_reports")
    fun canExportReports(): Boolean = PermissionManager.hasPermission("export_reports")

    // CASH REGISTER
    fun canOpenCashRegister(): Boolean = PermissionManager.hasPermission("open_cash_register")
    fun canCloseCashRegister(): Boolean = PermissionManager.hasPermission("close_cash_register")
    fun canCreateCashRegister(): Boolean =
        PermissionManager.hasPermission("can_create_cash_register")
    fun canViewCashRegister(): Boolean =
        PermissionManager.hasPermission("can_view_cash_register")
    fun canDeleteCashRegister(): Boolean =
        PermissionManager.hasPermission("can_delete_cash_register")

    fun canViewCashTransactions(): Boolean = PermissionManager.hasPermission("view_cash_movements")

    // UTILITY METHODS
    fun hasAnyPermission(vararg permissionKeys: String): Boolean =
        PermissionManager.hasAnyPermission(*permissionKeys)

    fun hasAllPermissions(vararg permissionKeys: String): Boolean =
        PermissionManager.hasAllPermissions(*permissionKeys)
}
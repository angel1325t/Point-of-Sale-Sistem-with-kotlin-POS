package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.utils

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.DiscountType

fun String.toDiscountType(): DiscountType = when (this.lowercase()) {
    "percent" -> DiscountType.PERCENT
    "fixed" -> DiscountType.FIXED
    else -> DiscountType.NONE
}

fun DiscountType.toServerString(): String = when (this) {
    DiscountType.PERCENT -> "percent"
    DiscountType.FIXED -> "fixed"
    DiscountType.NONE -> "none"
}
package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.inventory.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.inventory.InventorySortMode

@Composable
fun InventorySortDialog(
    selected: InventorySortMode,
    onSelect: (InventorySortMode) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        InventorySortMode.NAME_ASC to stringResource(R.string.inventory_sort_name_asc),
        InventorySortMode.NAME_DESC to stringResource(R.string.inventory_sort_name_desc),
        InventorySortMode.STOCK_ASC to stringResource(R.string.inventory_sort_stock_asc),
        InventorySortMode.STOCK_DESC to stringResource(R.string.inventory_sort_stock_desc),
        InventorySortMode.LOW_STOCK_FIRST to stringResource(R.string.inventory_sort_low_stock),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.inventory_sort_title)) },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row {
                        RadioButton(
                            selected = mode == selected,
                            onClick = { onSelect(mode) }
                        )
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.inventory_error_close)) }
        }
    )
}

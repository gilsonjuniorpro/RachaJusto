package com.rachajusto.feature.groupdetail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.rachajusto.R
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.SplitMode

fun ExpenseCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.FOOD -> Icons.Rounded.Restaurant
    ExpenseCategory.DRINKS -> Icons.Rounded.LocalBar
    ExpenseCategory.LODGING -> Icons.Rounded.Hotel
    ExpenseCategory.TRANSPORT -> Icons.Rounded.DirectionsCar
    ExpenseCategory.MARKET -> Icons.Rounded.ShoppingCart
    ExpenseCategory.OTHER -> Icons.AutoMirrored.Rounded.ReceiptLong
}

fun ExpenseCategory.labelRes(): Int = when (this) {
    ExpenseCategory.FOOD -> R.string.category_food
    ExpenseCategory.DRINKS -> R.string.category_drinks
    ExpenseCategory.LODGING -> R.string.category_lodging
    ExpenseCategory.TRANSPORT -> R.string.category_transport
    ExpenseCategory.MARKET -> R.string.category_market
    ExpenseCategory.OTHER -> R.string.category_other
}

fun SplitMode.labelRes(): Int = when (this) {
    SplitMode.EQUAL -> R.string.split_mode_equal
    SplitMode.ITEMIZED -> R.string.split_mode_itemized
    SplitMode.EXACT -> R.string.split_mode_exact
    SplitMode.SHARES -> R.string.split_mode_shares
}

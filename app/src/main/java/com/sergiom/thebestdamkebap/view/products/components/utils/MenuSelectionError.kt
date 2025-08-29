package com.sergiom.thebestdamkebap.view.products.components.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sergiom.thebestdamkebap.R
import com.sergiom.thebestdamkebap.domain.menu.MenuSelectionError

/* -------------------- errores localizados -------------------- */
@Composable
internal fun MenuSelectionError.asText(): String = when (this) {
    is MenuSelectionError.CountOutOfRange ->
        stringResource(
            R.string.error_menu_group_min_max,
            groupName, min, max
        )
    is MenuSelectionError.OptionNotAllowed ->
        stringResource(
            R.string.error_menu_option_not_allowed,
            groupName
        )
}


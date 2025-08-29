package com.sergiom.thebestdamkebap.view.orders.components

import androidx.annotation.StringRes
import com.sergiom.thebestdamkebap.domain.orders.ReorderLine
import java.util.Locale
import com.sergiom.thebestdamkebap.R

fun interface Strings {
    fun get(@StringRes resId: Int, vararg args: Any): String
}

internal fun groupLabelRes(key: String): Int = when (key.lowercase()) {
    "main", "principal" -> R.string.orders_group_label_main
    "side", "acompanamiento", "acompañamiento" -> R.string.orders_group_label_side
    "drink", "bebida" -> R.string.orders_group_label_drink
    else -> R.string.orders_group_label_selection
}

internal fun buildFriendlyDetails(
    lines: List<ReorderLine>,
    strings: Strings
): List<String> = buildList {
    lines.forEach { line ->
        when (line) {
            is ReorderLine.Product -> {
                val base  = line.name?.takeIf { it.isNotBlank() } ?: prettyNameFromId(line.productId)
                val extra = if (line.removedIngredients.isEmpty()) ""
                else strings.get(R.string.menu_removed_suffix, line.removedIngredients.joinToString())
                add(strings.get(R.string.orders_line_qty_name, line.qty, base, extra))
            }
            is ReorderLine.Menu -> {
                val menuName = line.name?.takeIf { it.isNotBlank() } ?: prettyNameFromId(line.menuId)
                add(strings.get(R.string.orders_line_qty_name, line.qty, menuName, ""))

                line.selections.forEach { (groupKey, list) ->
                    val label = strings.get(groupLabelRes(groupKey))
                    val content = list.joinToString(", ") { sel ->
                        val baseSel  = prettyNameFromId(sel.productId)
                        val extraSel = if (sel.removedIngredients.isEmpty()) ""
                        else strings.get(R.string.menu_removed_suffix, sel.removedIngredients.joinToString())
                        "$baseSel$extraSel"
                    }
                    add(strings.get(R.string.orders_group_bullet, label, content))
                }
            }
        }
    }
}

/** Convierte ids tipo `cocacola-zero` → `Coca-Cola Zero`, etc. */
internal fun prettyNameFromId(id: String): String {
    val base = id.replace('_', '-').replace('-', ' ').trim()
    var s = base.lowercase(Locale.forLanguageTag("es-ES"))
    s = s.replace("cocacola", "coca-cola")
    s = s.replace("nestea limon", "nestea limón")
    s = s.replace("kebap", "kebab")
    return s.split(' ').filter { it.isNotBlank() }.joinToString(" ") { w ->
        w.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("es-ES")) else it.toString() }
    }
}

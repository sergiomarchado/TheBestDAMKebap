package com.sergiom.thebestdamkebap.view.orders.components

import com.sergiom.thebestdamkebap.domain.orders.ReorderLine
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach

/* ───────────────── helpers de formateo legible (sin claves internas) ───────────────── */

internal fun buildFriendlyDetails(lines: List<ReorderLine>): List<String> = buildList {
    lines.forEach { line ->
        when (line) {
            is ReorderLine.Product -> {
                val base = line.name?.takeIf { it.isNotBlank() } ?: prettyNameFromId(line.productId)
                val extra = if (line.removedIngredients.isEmpty()) "" else " (sin ${line.removedIngredients.joinToString()})"
                add("${line.qty} × $base$extra")
            }
            is ReorderLine.Menu -> {
                val menuName = line.name?.takeIf { it.isNotBlank() } ?: prettyNameFromId(line.menuId)
                add("${line.qty} × $menuName")
                line.selections.forEach { (groupKey, list) ->
                    val label = groupLabelEs(groupKey)
                    val content = list.joinToString(", ") { sel ->
                        val base = prettyNameFromId(sel.productId)
                        if (sel.removedIngredients.isEmpty()) base
                        else "$base (sin ${sel.removedIngredients.joinToString()})"
                    }
                    add("   · $label: $content")
                }
            }
        }
    }
}

/** Mapea claves internas a etiquetas de UI en español. */
internal fun groupLabelEs(key: String): String = when (key.lowercase()) {
    "main", "principal" -> "Principal"
    "side", "acompanamiento", "acompañamiento" -> "Acompañamiento"
    "drink", "bebida" -> "Bebida"
    else -> "Selección"
}

/** Convierte ids del tipo `cocacola-zero` → `Coca-Cola Zero`, `patatas-fritas-grandes` → `Patatas fritas grandes`. */
internal fun prettyNameFromId(id: String): String {
    val base = id.replace('_', '-').replace('-', ' ').trim()
    var s = base.lowercase(Locale.forLanguageTag("es-ES"))

    // Normalizaciones conocidas
    s = s.replace("cocacola", "coca-cola")   // usa U+2011 no-break hyphen para verse mejor
    s = s.replace("nestea limon", "nestea limón")
    s = s.replace("kebap", "kebab") // por si acaso

    // Capitaliza cada palabra
    return s.split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { w ->
            w.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("es-ES")) else it.toString() }
        }
}


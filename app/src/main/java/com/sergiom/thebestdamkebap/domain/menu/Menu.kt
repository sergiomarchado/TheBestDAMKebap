package com.sergiom.thebestdamkebap.domain.menu

import com.sergiom.thebestdamkebap.domain.catalog.Prices

/** Definición de un Menú del catálogo. */
data class Menu(
    val id: String,
    val active: Boolean,
    val name: String,
    val description: String?,
    val imagePath: String?,
    val order: Long,
    val prices: Prices,                 // céntimos (pickup/delivery)
    val groups: List<MenuGroup>         // pasos de elección (principal, acompañamiento, bebida, etc.)
)

/** Grupo/paso de elección dentro de un menú. */
data class MenuGroup(
    val id: String,                     // ej. "main", "side", "drink"
    val name: String,                   // ej. "Principal"
    val min: Int,                       // mínimo a elegir (p.ej. 1)
    val max: Int,                       // máximo a elegir (p.ej. 1 o 2)
    val allowed: List<MenuAllowed>      // opciones disponibles en el grupo
)

/** Opción permitida dentro de un grupo. */
data class MenuAllowed(
    val productId: String,              // referencia a products/{productId}
    val delta: Prices? = null,          // suplemento sobre el precio base del menú (por modo)
    val default: Boolean = false,       // es la opción por defecto del grupo
    val allowIngredientRemoval: Boolean = true // permite quitar ingredientes en el detalle
)



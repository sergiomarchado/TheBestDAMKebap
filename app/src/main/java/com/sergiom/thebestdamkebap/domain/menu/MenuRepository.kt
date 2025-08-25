package com.sergiom.thebestdamkebap.domain.menu

import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    /** Menús activos ordenados por 'order'. */
    fun observeMenus(): Flow<List<Menu>>

    /** Observa un menú concreto por id (detalle). */
    fun observeMenu(menuId: String): Flow<Menu?>
}

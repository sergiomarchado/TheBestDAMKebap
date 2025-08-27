package com.sergiom.thebestdamkebap.navigation

/**
 * Rutas de entrada al flujo de **Autenticación**.
 *
 * Evita duplicar strings mágicos (e.g., "auth/entry?start=register") por la app.
 */
object AuthRoutes {
    const val ARG_START = "start"
    const val START_LOGIN = "login"
    const val START_REGISTER = "register"

    /** Ruta patrón registrada en el NavGraph de Auth (query opcional con default). */
    const val ENTRY = "auth/entry?start={$ARG_START}"

    /** Construye la ruta concreta para entrar en Auth indicando la pantalla inicial. */
    fun entryFor(start: String): String = "auth/entry?start=$start"

    fun entryForLogin(): String = entryFor(START_LOGIN)
    fun entryForRegister(): String = entryFor(START_REGISTER)
}

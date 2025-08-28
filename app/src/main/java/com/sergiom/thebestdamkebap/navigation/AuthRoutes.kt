package com.sergiom.thebestdamkebap.navigation

/**
 * Rutas de entrada al flujo de **Autenticación**.
 *
 * Objetivo:
 * - Centralizar las rutas parametrizadas de navegación para el subgrafo de Auth.
 * - Evitar strings mágicos repetidos (ej. `"auth/entry?start=register"`).
 *
 * Estructura:
 * - `ENTRY` → ruta patrón registrada en el NavGraph de Auth. Permite un parámetro
 *   opcional (`start`) que decide si entrar en LOGIN o REGISTER.
 * - Métodos `entryFor*` → construyen rutas concretas para navegar a Auth directamente
 *   a una pantalla inicial concreta.
 */
object AuthRoutes {
    /** Nombre del argumento de query para decidir pantalla inicial (login/register). */
    const val ARG_START = "start"

    /** Posibles valores del argumento `start`. */
    const val START_LOGIN = "login"
    const val START_REGISTER = "register"

    /**
     * Ruta patrón registrada en el NavGraph de Auth.
     *
     * - Se compone como `"auth/entry?start={start}"`.
     * - El argumento `start` se resuelve en tiempo de navegación (con default a `login`).
     */
    const val ENTRY = "auth/entry?start={$ARG_START}"

    /** Construye la ruta concreta para entrar a Auth indicando pantalla inicial. */
    fun entryFor(start: String): String = "auth/entry?start=$start"

    /** Atajo para construir la ruta de entrada que abre en LOGIN. */
    fun entryForLogin(): String = entryFor(START_LOGIN)

    /** Atajo para construir la ruta de entrada que abre en REGISTER. */
    fun entryForRegister(): String = entryFor(START_REGISTER)
}

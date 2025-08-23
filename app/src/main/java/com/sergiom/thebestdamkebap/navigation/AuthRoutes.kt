// navigation/AuthRoutes.kt
package com.sergiom.thebestdamkebap.navigation

/**
 * Punto único para construir rutas de entrada al **flujo de Autenticación**.
 *
 * ¿Para qué sirve?
 * - Para no repetir literales como "auth/entry?start=register" por la app.
 * - Para abrir Auth directamente en **Login** o **Register** desde cualquier pantalla.
 *
 * Ejemplos de uso:
 * ```
 * // Abrir Auth empezando en Login
 * navController.navigate(AuthRoutes.entryForLogin())
 *
 * // Abrir Auth empezando en Register
 * navController.navigate(AuthRoutes.entryForRegister())
 * ```
 *
 * Notas:
 * - El grafo de Auth debe registrar la ruta patrón [ENTRY] con el argumento opcional [ARG_START].
 * - Si no se pasa parámetro, se usará [START_LOGIN] por defecto.
 */
object AuthRoutes {
    /** Nombre del parámetro de query usado para decidir la primera pantalla. */
    const val ARG_START = "start"

    /** Valores admitidos para [ARG_START]. */
    const val START_LOGIN = "login"
    const val START_REGISTER = "register"

    /**
     * Ruta patrón registrada en el NavGraph de Auth.
     * Incluye el placeholder del argumento opcional.
     *
     * Ej.: "auth/entry?start={start}"
     */
    const val ENTRY = "auth/entry?start={$ARG_START}"

    /**
     * Construye la ruta concreta para entrar en Auth indicando la pantalla inicial.
     *
     * @param start Usa [START_LOGIN] o [START_REGISTER]. Cualquier otro valor caerá en Login.
     * @return Ruta navegable (p. ej.: "auth/entry?start=register").
     */
    fun entryFor(start: String): String = "auth/entry?start=$start"

    /** Atajo para entrar en Auth arrancando en **Login**. */
    fun entryForLogin(): String = entryFor(START_LOGIN)

    /** Atajo para entrar en Auth arrancando en **Register**. */
    fun entryForRegister(): String = entryFor(START_REGISTER)
}

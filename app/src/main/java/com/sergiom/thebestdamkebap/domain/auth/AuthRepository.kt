package com.sergiom.thebestdamkebap.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * AuthRepository
 *
 * Contrato de autenticación que usa la app, **sin depender** de Firebase ni de librerías
 * concretas. La capa de datos será la responsable de implementarlo.
 *
 * Qué ofrece:
 * - [currentUser]: flujo con el usuario actual (o `null` si no hay sesión).
 * - Operaciones básicas: login, registro, reset de contraseña, verificación por email,
 *   sesión anónima y logout.
 *
 * Notas de uso en la UI/VM:
 * - La UI observará [currentUser] para reaccionar a cambios de sesión.
 * - Las funciones `suspend` se llaman desde coroutines (p. ej., `viewModelScope.launch { ... }`).
 */
interface AuthRepository {

    /** Flujo del usuario actual; emite `null` si no hay sesión. */
    val currentUser: Flow<DomainUser?>

    /** Crea sesión anónima **solo** si no existe ya un usuario. */
    suspend fun signInAnonymouslyIfNeeded()

    /** Inicia sesión con email/contraseña y devuelve el usuario. */
    suspend fun signInWithEmail(email: String, password: String): DomainUser

    /**
     * Registra una cuenta con email/contraseña (o vincula si el usuario actual es invitado)
     * y devuelve el usuario resultante.
     */
    suspend fun registerWithEmail(name: String?, email: String, password: String): DomainUser

    /** Envía un correo para restablecer la contraseña del email indicado. */
    suspend fun sendPasswordReset(email: String)

    /** Cierra la sesión actual (si la hay). */
    suspend fun signOut()

    /** Envía un correo de verificación al usuario actual (si lo permite el backend). */
    suspend fun requestEmailVerification()
}



// domain/auth/DomainUser.kt
package com.sergiom.thebestdamkebap.domain.auth

/**
 * Usuario de dominio, independiente de Firebase.
 * Es lo que la UI y el ViewModel consumen.
 */
data class DomainUser(
    val id: String,
    val email: String?,
    val name: String?,
    val isAnonymous: Boolean,
    val isEmailVerified: Boolean
)

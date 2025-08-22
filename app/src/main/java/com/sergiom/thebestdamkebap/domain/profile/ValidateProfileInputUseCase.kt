// domain/profile/ValidateProfileInputUseCase.kt
package com.sergiom.thebestdamkebap.domain.profile

import java.util.Calendar

/**
 * Valida y sanea los campos de perfil.
 * Reglas actuales:
 * - Nombre: máx 50.
 * - Apellidos: máx 60.
 * - Teléfono (ES): opcional; si se informa, exactamente 9 dígitos tras normalizar.
 * - Fecha de nacimiento: no futura; edad mínima 13 años.
 */
class ValidateProfileInputUseCase {

    data class Errors(
        val givenName: String? = null,
        val familyName: String? = null,
        val phone: String? = null,
        val birthDate: String? = null
    ) {
        val hasAny get() =
            givenName != null || familyName != null || phone != null || birthDate != null
    }

    data class Sanitized(
        val givenName: String?,      // null si vacío/solo espacios
        val familyName: String?,     // idem
        val phoneNormalized: String?,// solo dígitos; null si vacío
        val birthDateMillis: Long?   // null si se borra o no se estableció
    )

    data class Result(
        val errors: Errors,
        val sanitized: Sanitized
    ) {
        val valid get() = !errors.hasAny
    }

    operator fun invoke(
        givenName: String?,
        familyName: String?,
        phoneRaw: String?,
        birthDateMillis: Long?
    ): Result {
        val name = givenName?.trim().orEmpty()
        val fam  = familyName?.trim().orEmpty()
        val phoneDigits = phoneRaw.orEmpty().filter { it.isDigit() }
        val now = System.currentTimeMillis()

        var eName: String? = null
        var eFam: String?  = null
        var ePhone: String? = null
        var eBirth: String? = null

        if (name.length > 50) eName = "Máximo 50 caracteres"
        if (fam.length  > 60) eFam  = "Máximo 60 caracteres"

        if (phoneDigits.isNotEmpty() && phoneDigits.length != 9) {
            ePhone = "Debe tener 9 dígitos (España)"
        }

        if (birthDateMillis != null) {
            if (birthDateMillis > now) {
                eBirth = "La fecha no puede ser futura"
            } else if (!isAtLeastYearsOld(birthDateMillis, 13)) {
                eBirth = "Debes tener al menos 13 años"
            }
        }

        val sanitized = Sanitized(
            givenName = name.ifBlank { null },
            familyName = fam.ifBlank { null },
            phoneNormalized = phoneDigits.ifBlank { null },
            birthDateMillis = birthDateMillis
        )

        return Result(
            errors = Errors(
                givenName = eName,
                familyName = eFam,
                phone = ePhone,
                birthDate = eBirth
            ),
            sanitized = sanitized
        )
    }

    @Suppress("SameParameterValue")
    private fun isAtLeastYearsOld(birthMillis: Long, years: Int): Boolean {
        val birth = Calendar.getInstance().apply { timeInMillis = birthMillis }
        val now = Calendar.getInstance()
        var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
        return age >= years
    }
}

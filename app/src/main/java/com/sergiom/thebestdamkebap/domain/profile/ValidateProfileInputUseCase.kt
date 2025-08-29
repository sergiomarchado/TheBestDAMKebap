package com.sergiom.thebestdamkebap.domain.profile

import java.util.Calendar

class ValidateProfileInputUseCase {

    /** Códigos de error neutrales (sin texto/UI). */
    enum class ProfileError {
        GIVEN_NAME_TOO_LONG,
        FAMILY_NAME_TOO_LONG,
        PHONE_INVALID_ES,
        BIRTH_FUTURE,
        BIRTH_AGE_MIN_13,
    }

    /** Errores por campo (null = sin error). */
    data class Errors(
        val givenName: ProfileError? = null,
        val familyName: ProfileError? = null,
        val phone: ProfileError? = null,
        val birthDate: ProfileError? = null
    ) {
        val hasAny get() =
            givenName != null || familyName != null || phone != null || birthDate != null
    }

    /** Valores saneados para persistencia. */
    data class Sanitized(
        val givenName: String?,       // null si vacío/solo espacios
        val familyName: String?,      // idem
        val phoneNormalized: String?, // 9 dígitos (ES) o null si vacío
        val birthDateMillis: Long?    // null si se borra o no se estableció
    )

    /** Resultado completo: errores + valores saneados. */
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
        // 1) Trims
        val name = givenName?.trim().orEmpty()
        val fam  = familyName?.trim().orEmpty()

        // 2) Normalizar teléfono:
        //    - Solo dígitos
        //    - Si empieza por "34" y tiene 11 dígitos (p.ej. +34 612...), quitamos el prefijo
        val digitsOnly = phoneRaw.orEmpty().filter { it.isDigit() }
        val phoneNormalized =
            if (digitsOnly.length == 11 && digitsOnly.startsWith("34")) digitsOnly.drop(2) else digitsOnly

        val now = System.currentTimeMillis()

        var eName: ProfileError? = null
        var eFam: ProfileError?  = null
        var ePhone: ProfileError? = null
        var eBirth: ProfileError? = null

        // 3) Reglas
        if (name.length > 50) eName = ProfileError.GIVEN_NAME_TOO_LONG
        if (fam.length  > 60) eFam  = ProfileError.FAMILY_NAME_TOO_LONG

        // 4) Teléfono ES: si se informa, deben ser exactamente 9 dígitos
        if (phoneNormalized.isNotEmpty() && phoneNormalized.length != 9) {
            ePhone = ProfileError.PHONE_INVALID_ES
        }

        // 5) Fecha: no futura y ≥ 13 años
        if (birthDateMillis != null) {
            eBirth = when {
                birthDateMillis > now -> ProfileError.BIRTH_FUTURE
                !isAtLeastYearsOld(birthDateMillis, 13) -> ProfileError.BIRTH_AGE_MIN_13
                else -> null
            }
        }

        val sanitized = Sanitized(
            givenName = name.ifBlank { null },
            familyName = fam.ifBlank { null },
            phoneNormalized = phoneNormalized.ifBlank { null },
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

    /** ≥ `years` años. */
    @Suppress("SameParameterValue")
    private fun isAtLeastYearsOld(birthMillis: Long, years: Int): Boolean {
        val birth = Calendar.getInstance().apply { timeInMillis = birthMillis }
        val now = Calendar.getInstance()
        var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
        return age >= years
    }
}

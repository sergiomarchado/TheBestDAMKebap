package com.sergiom.thebestdamkebap.domain.profile

import java.util.Calendar

/**
 * ValidateProfileInputUseCase
 *
 * Valida y **sanea** los campos del perfil antes de guardarlos.
 *
 * Reglas actuales:
 * - Nombre: máximo 50 caracteres.
 * - Apellidos: máximo 60 caracteres.
 * - Teléfono (ES): opcional; si se informa, deben quedar **exactamente 9 dígitos** tras normalizar.
 *   (Se admite entrada con prefijo país `+34` o `34`: se elimina antes de validar.)
 * - Fecha de nacimiento: no futura y edad mínima de 13 años.
 *
 * Sugerencia de uso:
 * - Llamar a `invoke(...)` con los valores del formulario.
 * - Si `result.valid` es `true`, usar `result.sanitized` para persistir.
 * - Si hay errores, mostrar `result.errors` en los campos correspondientes.
 */
class ValidateProfileInputUseCase {

    /** Errores por campo (null = sin error). */
    data class Errors(
        val givenName: String? = null,
        val familyName: String? = null,
        val phone: String? = null,
        val birthDate: String? = null
    ) {
        val hasAny get() =
            givenName != null || familyName != null || phone != null || birthDate != null
    }

    /** Valores saneados para persistencia. */
    data class Sanitized(
        val givenName: String?,       // null si vacío/solo espacios
        val familyName: String?,      // idem
        val phoneNormalized: String?, // solo 9 dígitos (ES) o null si vacío
        val birthDateMillis: Long?    // null si se borra o no se estableció
    )

    /** Resultado completo: errores + valores saneados. */
    data class Result(
        val errors: Errors,
        val sanitized: Sanitized
    ) {
        val valid get() = !errors.hasAny
    }

    /**
     * Ejecuta la validación+saneado.
     *
     * @param givenName Nombre (puede venir con espacios).
     * @param familyName Apellidos (puede venir con espacios).
     * @param phoneRaw Teléfono tal como lo teclea el usuario (se permiten espacios, guiones, `+34`...).
     * @param birthDateMillis Fecha de nacimiento (millis desde epoch) o null.
     */
    operator fun invoke(
        givenName: String?,
        familyName: String?,
        phoneRaw: String?,
        birthDateMillis: Long?
    ): Result {
        // 1) Trims básicos
        val name = givenName?.trim().orEmpty()
        val fam  = familyName?.trim().orEmpty()

        // 2) Normalización de teléfono:
        //    - Quitamos todo excepto dígitos.
        //    - Si empieza por "34" y tiene 11 dígitos (ej. "+34 612..." → "34612..."), quitamos el prefijo país.
        val digitsOnly = phoneRaw.orEmpty().filter { it.isDigit() }
        val phoneNormalized =
            if (digitsOnly.length == 11 && digitsOnly.startsWith("34")) digitsOnly.drop(2) else digitsOnly

        val now = System.currentTimeMillis()

        var eName: String? = null
        var eFam: String?  = null
        var ePhone: String? = null
        var eBirth: String? = null

        // 3) Reglas de longitud de texto
        if (name.length > 50) eName = "Máximo 50 caracteres"
        if (fam.length  > 60) eFam  = "Máximo 60 caracteres"

        // 4) Teléfono ES: si se informa, deben ser 9 dígitos
        if (phoneNormalized.isNotEmpty() && phoneNormalized.length != 9) {
            ePhone = "Debe tener 9 dígitos (España)"
        }

        // 5) Fecha: no futura y ≥ 13 años
        if (birthDateMillis != null) {
            if (birthDateMillis > now) {
                eBirth = "La fecha no puede ser futura"
            } else if (!isAtLeastYearsOld(birthDateMillis, 13)) {
                eBirth = "Debes tener al menos 13 años"
            }
        }

        // 6) Saneado final (strings vacíos → null)
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

    /** Comprueba si `birthMillis` corresponde a al menos `years` años de edad. */
    @Suppress("SameParameterValue")
    private fun isAtLeastYearsOld(birthMillis: Long, years: Int): Boolean {
        val birth = Calendar.getInstance().apply { timeInMillis = birthMillis }
        val now = Calendar.getInstance()
        var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
        return age >= years
    }
}

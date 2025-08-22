package com.sergiom.thebestdamkebap.domain.address

class ValidateAddressInputUseCase {

    data class Errors(
        val street: String? = null,
        val number: String? = null,
        val city: String? = null,
        val postalCode: String? = null,
        val phone: String? = null,
    )
    data class Sanitized(
        val label: String?,
        val recipientName: String?,
        val phoneNormalized: String?,
        val street: String,
        val number: String,
        val floorDoor: String?,
        val city: String,
        val province: String?,
        val postalCode: String,
        val notes: String?
    )
    data class Result(val valid: Boolean, val errors: Errors, val sanitized: Sanitized)

    operator fun invoke(
        label: String?,
        recipientName: String?,
        phone: String?,
        street: String?,
        number: String?,
        floorDoor: String?,
        city: String?,
        province: String?,
        postalCode: String?,
        notes: String?
    ): Result {
        val e = Errors(
            street = if (street.isNullOrBlank()) "Calle obligatoria" else null,
            number = if (number.isNullOrBlank()) "Número obligatorio" else null,
            city = if (city.isNullOrBlank()) "Ciudad obligatoria" else null,
            postalCode = when {
                postalCode.isNullOrBlank() -> "CP obligatorio"
                !postalCode.matches(Regex("""\d{5}""")) -> "CP inválido (5 dígitos)"
                else -> null
            },
            phone = if (!phone.isNullOrBlank() && phone.filter { it.isDigit() }.length != 9)
                "Teléfono inválido (9 dígitos)" else null
        )
        val valid = listOf(e.street, e.number, e.city, e.postalCode, e.phone).all { it == null }
        val sanitized = Sanitized(
            label = label?.trim()?.takeIf { it.isNotEmpty() },
            recipientName = recipientName?.trim()?.takeIf { it.isNotEmpty() },
            phoneNormalized = phone?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() },
            street = street?.trim().orEmpty(),
            number = number?.trim().orEmpty(),
            floorDoor = floorDoor?.trim()?.takeIf { it.isNotEmpty() },
            city = city?.trim().orEmpty(),
            province = province?.trim()?.takeIf { it.isNotEmpty() },
            postalCode = postalCode?.trim().orEmpty(),
            notes = notes?.trim()?.takeIf { it.isNotEmpty() }
        )
        return Result(valid, e, sanitized)
    }
}
// domain/address/ValidateAddressInputUseCase.kt
package com.sergiom.thebestdamkebap.domain.address

class ValidateAddressInputUseCase {

    /** Códigos de error neutrales (sin texto/UI). */
    enum class AddressError {
        STREET_REQUIRED, STREET_TOO_LONG,
        NUMBER_REQUIRED, NUMBER_TOO_LONG,
        CITY_REQUIRED, CITY_TOO_LONG,
        POSTAL_REQUIRED, POSTAL_INVALID,
        PHONE_REQUIRED, PHONE_INVALID
    }

    data class Errors(
        val street: AddressError? = null,
        val number: AddressError? = null,
        val city: AddressError? = null,
        val postalCode: AddressError? = null,
        val phone: AddressError? = null,
    )

    data class Sanitized(
        val label: String?,
        val recipientName: String?,
        val phoneNormalized: String?,  // ^\+?[0-9]{9,15}$
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
        fun clean(s: String?) = s?.trim()?.takeIf { it.isNotEmpty() }

        val phoneNorm = normalizePhone(phone)

        val e = Errors(
            street = when {
                street.isNullOrBlank() -> AddressError.STREET_REQUIRED
                street.length > 120     -> AddressError.STREET_TOO_LONG
                else -> null
            },
            number = when {
                number.isNullOrBlank() -> AddressError.NUMBER_REQUIRED
                number.length > 10      -> AddressError.NUMBER_TOO_LONG
                else -> null
            },
            city = when {
                city.isNullOrBlank() -> AddressError.CITY_REQUIRED
                city.length > 80     -> AddressError.CITY_TOO_LONG
                else -> null
            },
            postalCode = when {
                postalCode.isNullOrBlank() -> AddressError.POSTAL_REQUIRED
                !postalCode.matches(Regex("""^\d{5}$""")) -> AddressError.POSTAL_INVALID
                else -> null
            },
            phone = when {
                phoneNorm == null -> AddressError.PHONE_REQUIRED
                !phoneNorm.matches(Regex("""^\+?[0-9]{9,15}$""")) ->
                    AddressError.PHONE_INVALID
                else -> null
            }
        )

        val valid = listOf(e.street, e.number, e.city, e.postalCode, e.phone).all { it == null }

        val sanitized = Sanitized(
            label = clean(label),
            recipientName = clean(recipientName),
            phoneNormalized = phoneNorm,
            street = clean(street) ?: "",
            number = clean(number) ?: "",
            floorDoor = clean(floorDoor),
            city = clean(city) ?: "",
            province = clean(province),
            postalCode = clean(postalCode) ?: "",
            notes = clean(notes)
        )

        return Result(valid, e, sanitized)
    }

    private fun normalizePhone(raw: String?): String? {
        val s = raw?.trim() ?: return null
        if (s.isEmpty()) return null
        val hasPlus = s.startsWith("+")
        val digits = s.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return if (hasPlus) "+$digits" else digits
    }
}

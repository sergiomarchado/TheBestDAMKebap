package com.sergiom.thebestdamkebap.view.address.utils

// ui (Compose), por ejemplo en AddressEditScreen.kt
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sergiom.thebestdamkebap.R
import com.sergiom.thebestdamkebap.domain.address.ValidateAddressInputUseCase.AddressError

@Composable
fun AddressError.asText(): String = when (this) {
    AddressError.STREET_REQUIRED -> stringResource(R.string.error_street_required)
    AddressError.STREET_TOO_LONG -> stringResource(R.string.error_street_too_long)
    AddressError.NUMBER_REQUIRED -> stringResource(R.string.error_number_required)
    AddressError.NUMBER_TOO_LONG -> stringResource(R.string.error_number_too_long)
    AddressError.CITY_REQUIRED   -> stringResource(R.string.error_city_required)
    AddressError.CITY_TOO_LONG   -> stringResource(R.string.error_city_too_long)
    AddressError.POSTAL_REQUIRED -> stringResource(R.string.error_postal_required)
    AddressError.POSTAL_INVALID  -> stringResource(R.string.error_postal_invalid)
    AddressError.PHONE_REQUIRED  -> stringResource(R.string.error_phone_required)
    AddressError.PHONE_INVALID   -> stringResource(R.string.error_phone_invalid)
}



package com.sergiom.thebestdamkebap.view.profile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sergiom.thebestdamkebap.R
import com.sergiom.thebestdamkebap.domain.profile.ValidateProfileInputUseCase.ProfileError

@Composable
internal fun ProfileError.asText(): String = when (this) {
    ProfileError.GIVEN_NAME_TOO_LONG -> stringResource(R.string.profile_error_given_name_too_long)
    ProfileError.FAMILY_NAME_TOO_LONG -> stringResource(R.string.profile_error_family_name_too_long)
    ProfileError.PHONE_INVALID_ES -> stringResource(R.string.profile_error_phone_invalid_es)
    ProfileError.BIRTH_FUTURE -> stringResource(R.string.profile_error_birth_future)
    ProfileError.BIRTH_AGE_MIN_13 -> stringResource(R.string.profile_error_birth_min_age_13)
}


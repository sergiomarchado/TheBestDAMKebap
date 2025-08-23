package com.sergiom.thebestdamkebap.data.profile

import com.sergiom.thebestdamkebap.domain.profile.UserProfile as DomainUserProfile

fun UserProfile.toDomain(): DomainUserProfile =
    DomainUserProfile(
        uid = uid,
        email = email,
        givenName = givenName,
        familyName = familyName,
        phone = phone,
        birthDateMillis = birthDate?.time,
        defaultAddressId = defaultAddressId,
        createdAtMillis = createdAt?.time,
        updatedAtMillis = updatedAt?.time
    )


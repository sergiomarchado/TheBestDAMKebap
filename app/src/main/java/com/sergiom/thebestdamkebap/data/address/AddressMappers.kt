package com.sergiom.thebestdamkebap.data.address

import com.sergiom.thebestdamkebap.domain.address.Address as DomainAddress

fun Address.toDomain(): DomainAddress =
    DomainAddress(
        id = id,
        label = label,
        recipientName = recipientName,
        phone = phone,
        street = street,
        number = number,
        floorDoor = floorDoor,
        city = city,
        province = province,
        postalCode = postalCode,
        notes = notes,
        lat = lat,
        lng = lng,
        createdAtMillis = createdAt?.time,
        updatedAtMillis = updatedAt?.time
    )

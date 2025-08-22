// data/address/AddressRepository.kt
package com.sergiom.thebestdamkebap.data.address

import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    /** Lista reactiva de direcciones del usuario. */
    fun observeAddresses(uid: String): Flow<List<Address>>

    /** Observa una dirección concreta (puede ser null si se borra). */
    fun observeAddress(uid: String, aid: String): Flow<Address?>

    /**
     * Crea/actualiza una dirección. Si [aid] es null, crea doc nuevo y devuelve su id.
     * No gestiona “default”; usa [setDefaultAddress] en el User doc (campo `defaultAddressId`).
     */
    suspend fun upsertAddress(uid: String, aid: String?, input: AddressInput): String

    /** Borra una dirección. */
    suspend fun deleteAddress(uid: String, aid: String)

    /** Marca la dirección por defecto en `/users/{uid}.defaultAddressId = aid`. */
    suspend fun setDefaultAddress(uid: String, aid: String)
}

// data/address/AddressRepository.kt
package com.sergiom.thebestdamkebap.data.address

import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de **direcciones de usuario**.
 *
 * Contrato (agnóstico a UI):
 * - Expone **flujos** para observar la lista y un elemento concreto.
 * - Ofrece operaciones de escritura para **crear/actualizar**, **eliminar** y **marcar como
 *   predeterminada** (campo `defaultAddressId` en `/users/{uid}`).
 *
 * Notas de diseño:
 * - La **validación y normalización** (reglas de negocio) no se hace aquí; vive en dominio
 *   (p. ej. [com.sergiom.thebestdamkebap.domain.address.ValidateAddressInputUseCase]).
 *   Este repo asume `AddressInput` ya saneado.
 * - El modelo [Address] **incluye** su `id` (no forma parte del documento en Firestore; se
 *   completa con `doc.id` en la implementación).
 * - Las marcas de tiempo (`createdAt`/`updatedAt`) se gestionan en **servidor** (serverTimestamp).
 *
 * Errores:
 * - Las funciones `suspend` pueden fallar con excepciones de la capa de datos
 *   (permisos, red, validación de backend…). Deja que suban y mapéalas en el ViewModel.
 */
interface AddressRepository {

    /**
     * Observa en tiempo real **todas** las direcciones del usuario.
     *
     * Contrato:
     * - Devuelve un `Flow` que emite una **lista completa** cada vez que hay cambios.
     * - La implementación debe **rellenar** `Address.id` con el id del documento.
     * - Orden **no garantizado** por contrato; la UI/VM puede ordenar según convenga.
     *
     * Comportamiento típico de la implementación (Firestore):
     * - Flow **frío** que se activa al coleccionarse.
     * - Emite inmediatamente (posiblemente lista vacía) y a continuación con cada snapshot.
     */
    fun observeAddresses(uid: String): Flow<List<Address>>

    /**
     * Observa en tiempo real **una dirección concreta**.
     *
     * Contrato:
     * - Emite `Address?`:
     *   - `Address` cuando existe (con `id` rellenado).
     *   - `null` si se borra o nunca existió.
     */
    fun observeAddress(uid: String, aid: String): Flow<Address?>

    /**
     * Crea o **actualiza** una dirección por **merge**.
     *
     * Reglas:
     * - Si [aid] es `null` → crea **nuevo** documento y devuelve su `id`.
     * - Si [aid] tiene valor → hace **upsert** (merge) sobre ese documento.
     * - Sólo se escriben los **campos no nulos** de [input] (merge parcial).
     * - **No** gestiona la predeterminada; usa [setDefaultAddress] aparte.
     *
     * Tiempos:
     * - `updatedAt` se actualiza siempre (serverTimestamp).
     * - `createdAt` sólo se establece en **creación** (no se sobreescribe).
     */
    suspend fun upsertAddress(uid: String, aid: String?, input: AddressInput): String

    /**
     * Elimina una dirección del usuario.
     *
     * Contrato adicional esperado de la implementación:
     * - Si la dirección eliminada era la **predeterminada**, debe limpiar
     *   `users/{uid}.defaultAddressId` de forma atómica (transacción).
     */
    suspend fun deleteAddress(uid: String, aid: String)

    /**
     * Marca una dirección como **predeterminada** en `/users/{uid}.defaultAddressId = aid`.
     *
     * Contrato:
     * - Debe **verificar** que la dirección existe; si no existe, lanzar error.
     * - No modifica otras direcciones; sólo actualiza el campo del documento de usuario.
     */
    suspend fun setDefaultAddress(uid: String, aid: String)
}

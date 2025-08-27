package com.sergiom.thebestdamkebap.domain.address

import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de **direcciones de usuario**.
 *
 * Contrato (agnóstico a UI y a la fuente de datos):
 * - Expone **flujos** para observar la lista y un elemento concreto.
 * - Ofrece operaciones de escritura para **crear/actualizar**, **eliminar** y **marcar como
 *   predeterminada** (campo `defaultAddressId` en `/users/{uid}`).
 *
 * Notas de diseño:
 * - La **validación y normalización** (reglas de negocio) vive fuera del repo (p. ej. UseCases).
 *   Este repo asume que [AddressInput] ya viene saneado.
 * - El modelo [Address] **incluye** su `id` (no forma parte del documento en Firestore; la
 *   implementación lo rellenará con `doc.id` si usa Firestore).
 * - Las marcas de tiempo (`createdAt`/`updatedAt`) se gestionan en **servidor** si la impl. es Firestore.
 *
 * Errores:
 * - Las funciones `suspend` pueden lanzar excepciones de la capa de datos (permisos, red, etc.).
 *   La capa superior decide cómo mapearlas/mostrarlas.
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
     * Comportamiento típico (si la impl. usa Firestore):
     * - Flow **frío** que se activa al coleccionarse.
     * - Emite inmediatamente (posible lista vacía) y después con cada snapshot.
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
     * - Solo se escriben los **campos no nulos** de [input] (merge parcial).
     * - **No** gestiona la predeterminada; usa [setDefaultAddress] aparte.
     *
     * Tiempos:
     * - `updatedAt` se actualiza siempre (serverTimestamp si aplica).
     * - `createdAt` solo se establece en **creación** (no se sobreescribe).
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
     * - No modifica otras direcciones; solo actualiza el campo del documento de usuario.
     */
    suspend fun setDefaultAddress(uid: String, aid: String)

    suspend fun addressExists(uid: String, aid: String): Boolean
}

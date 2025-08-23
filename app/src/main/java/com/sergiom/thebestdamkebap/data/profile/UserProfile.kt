package com.sergiom.thebestdamkebap.data.profile

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo de perfil de usuario almacenado en Firestore en `/users/{uid}`.
 *
 * Propósito:
 * - Centralizar datos básicos del usuario visibles/ed itables desde la app.
 * - Desacoplar la UI de los detalles de Firestore (timestamps, tipos, etc.).
 *
 * Notas de diseño:
 * - `email` está **desnormalizado** (copiado desde Auth) para facilitar listados/consultas
 *   en otros clientes (web/backoffice) sin tener que leer Auth.
 * - `birthDate` se guarda como `Date` (equivalente a Timestamp en Firestore). La UI lo
 *   formatea según locale.
 * - `defaultAddressId` guarda el **id** de la dirección por defecto en
 *   `/users/{uid}/addresses/{addressId}`.
 * - `createdAt` y `updatedAt` se rellenan en servidor con `@ServerTimestamp`.
 *
 * Serialización:
 * - Es un `data class` con **valores por defecto**, lo que permite a Firestore
 *   instanciarlo aunque falten campos en el documento.
 */
data class UserProfile(
    /** UID del usuario (igual que Auth). */
    val uid: String = "",
    /** Email de contacto/Acceso (puede ser `null` si la cuenta es invitado o se desvinculó). */
    val email: String? = null,
    /** Nombre de pila (opcional). */
    val givenName: String? = null,
    /** Apellidos (opcional). */
    val familyName: String? = null,
    /** Teléfono de contacto (opcional). */
    val phone: String? = null,
    /** Fecha de nacimiento (opcional). */
    val birthDate: Date? = null,
    /** Id de la dirección predeterminada en la subcolección `addresses` (opcional). */
    val defaultAddressId: String? = null,

    /** Marca de creación (servidor). Solo lectura en cliente. */
    @ServerTimestamp var createdAt: Date? = null,
    /** Marca de última actualización (servidor). Solo lectura en cliente. */
    @ServerTimestamp var updatedAt: Date? = null
)

/**
 * Entrada de **actualización parcial** de perfil (merge).
 *
 * Semántica actual (según tu `FirebaseProfileRepository`):
 * - Un campo con `null` significa **“no tocar”** ese valor en Firestore.
 * - Si el valor viene con texto vacío desde la UI, el caso de uso lo normaliza a `null`,
 *   por lo que **tampoco** lo borra. (Queda pendiente soportar borrado explícito).
 *
 * 💡 Si quieres que “vaciar” un campo en la UI se traduzca en **borrarlo** en Firestore,
 *   ajusta el repositorio para enviar `FieldValue.delete()` cuando el input venga “vacío”.
 *   (Puedo pasarte el patch cuando quieras).
 */
data class ProfileInput(
    val givenName: String? = null,
    val familyName: String? = null,
    val phone: String? = null,
    /** Epoch millis para la fecha de nacimiento; `null` ⇒ no cambiar. */
    val birthDateMillis: Long? = null,
)

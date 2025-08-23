package com.sergiom.thebestdamkebap.view.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Panel lateral (drawer) de **cuenta** dentro de Home.
 *
 * Qué muestra:
 * - Cabecera con el nombre del usuario y, si procede, su email.
 * - Accesos rápidos a secciones de cuenta: perfil, direcciones, pedidos y ajustes.
 * - Bloque inferior con acciones de sesión:
 *    - Invitado → "Iniciar sesión" y "Crear cuenta".
 *    - Usuario con sesión → "Cerrar sesión".
 *
 * Cuándo usarlo:
 * - Se renderiza como contenido del `ModalNavigationDrawer` de Home.
 * - No navega por sí mismo: expone callbacks que el shell resolverá (cerrar drawer,
 *   navegar a rutas internas o lanzar el flujo de auth).
 *
 * Parámetros:
 * @param userLabel     Etiqueta visible del usuario (p. ej., “Invitado”, nombre o email).
 * @param userEmail     Email del usuario si está disponible (se oculta para invitados).
 * @param userIsGuest   Indica si la sesión actual es de invitado.
 * @param onOpenProfile Abre la pantalla de perfil.
 * @param onManageAddresses Abre la gestión de direcciones (lista/sheet).
 * @param onOpenOrders  Abre el historial de pedidos.
 * @param onOpenSettings Abre ajustes de la cuenta.
 * @param onLogin       Inicia el flujo de login.
 * @param onRegister    Inicia el flujo de registro.
 * @param onLogout      Cierra la sesión actual.
 */
@Composable
fun HomeDrawerContent(
    userLabel: String,
    userEmail: String?,
    userIsGuest: Boolean,
    onOpenProfile: () -> Unit,
    onManageAddresses: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onLogout: () -> Unit,
) {

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(16.dp))

        // Cabecera usuario
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = userLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (!userIsGuest && !userEmail.isNullOrBlank()) {
                    Text(
                        userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Acciones de cuenta
        NavigationDrawerItem(
            label = {
                Text(
                    text = "Mi perfil",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            },
            selected = false,
            onClick = onOpenProfile,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = {
                Text(
                    text = "Mis direcciones", color = MaterialTheme.colorScheme.onPrimary
                )
            },
            selected = false,
            onClick = onManageAddresses, // abre sheet/diálogo
            icon = {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = {
                Text(
                    text = "Últimos pedidos", color = MaterialTheme.colorScheme.onPrimary
                )
            },
            selected = false,
            onClick = onOpenOrders,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = {
                Text(
                    text = "Configuración",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            },
            selected = false,
            onClick = onOpenSettings,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Sesión
        if (userIsGuest) {
            NavigationDrawerItem(
                label = {
                    Text(
                        text = "Iniciar sesión",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                selected = false,
                onClick = onLogin,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Login,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = {
                    Text(
                        text = "Crear cuenta",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                selected = false,
                onClick = onRegister,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    ) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        } else {
            NavigationDrawerItem(
                label = {
                    Text(
                        text = "Cerrar sesión",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                selected = false,
                onClick = onLogout,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    ) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

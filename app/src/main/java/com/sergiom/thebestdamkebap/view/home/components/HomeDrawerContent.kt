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
 * Drawer de **cuenta** en Home.
 *
 * Secciones:
 * 1) Cabecera de usuario (icono + label + email si aplica).
 * 2) Acciones de cuenta:
 *    - Mi Perfil
 *    - Mis Direcciones (abre un sheet/diálogo)
 *    - Últimos pedidos
 *    - Configuración
 * 3) Bloque inferior: Iniciar sesión / Crear cuenta (invitado) o Cerrar sesión.
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
    val colors = MaterialTheme.colorScheme

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
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(userLabel, style = MaterialTheme.typography.titleMedium)
                if (!userIsGuest && !userEmail.isNullOrBlank()) {
                    Text(
                        userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Acciones de cuenta
        NavigationDrawerItem(
            label = { Text("Mi perfil") }, // TODO: strings.xml
            selected = false,
            onClick = onOpenProfile,
            icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Mis direcciones") },
            selected = false,
            onClick = onManageAddresses, // abre sheet/diálogo
            icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Últimos pedidos") },
            selected = false,
            onClick = onOpenOrders,
            icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Configuración") },
            selected = false,
            onClick = onOpenSettings,
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Sesión
        if (userIsGuest) {
            NavigationDrawerItem(
                label = { Text("Iniciar sesión") },
                selected = false,
                onClick = onLogin,
                icon = { Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text("Crear cuenta") },
                selected = false,
                onClick = onRegister,
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        } else {
            NavigationDrawerItem(
                label = { Text("Cerrar sesión") },
                selected = false,
                onClick = onLogout,
                icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

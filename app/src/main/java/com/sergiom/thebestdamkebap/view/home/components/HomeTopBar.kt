// view/home/components/HomeTopBar.kt
package com.sergiom.thebestdamkebap.view.home.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    @DrawableRes logoRes: Int? = null,
    userLabel: String?,
    userIsGuest: Boolean,
    onOpenLogin: () -> Unit = {},
    onOpenRegister: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
    var userMenuOpen by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            modifier = modifier
                .padding(top = 16.dp, bottom = 6.dp)
                .height(82.dp) // barra algo más alta para alojar bien el logo de 62.dp
                .drawBehind {
                    val y = size.height - 1f
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1f
                    )
                },
            // IZQUIERDA: logo grande centrado verticalmente
            navigationIcon = {
                if (logoRes != null) {
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .height(62.dp)
                            .width(122.dp)
                    )
                } else {
                    Spacer(Modifier.width(16.dp))
                }
            },
            // CENTRO: píldora usuario (icono + texto + flecha)
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userLabel.isNullOrBlank()) {
                        // anclamos el menú a ESTA caja
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { userMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = userLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (userMenuOpen) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Menú desplegable anclado a la píldora
                            DropdownMenu(
                                expanded = userMenuOpen,
                                onDismissRequest = { userMenuOpen = false }
                            ) {
                                if (userIsGuest) {
                                    DropdownMenuItem(
                                        text = { Text("Iniciar sesión") },
                                        onClick = { userMenuOpen = false; onOpenLogin() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Registrarse") },
                                        onClick = { userMenuOpen = false; onOpenRegister() }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
                                        text = { Text("Cerrar sesión") },
                                        onClick = { userMenuOpen = false; onSignOut() }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            // DERECHA: hamburguesa un poco más grande
            actions = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Abrir menú",
                        modifier = Modifier.size(32.dp), // ↑ antes 28.dp
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
        HorizontalDivider(Modifier, 2.dp, DividerDefaults.color)

    }

}

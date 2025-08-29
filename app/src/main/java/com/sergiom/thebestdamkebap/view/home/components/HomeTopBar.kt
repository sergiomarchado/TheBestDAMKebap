// view/home/components/HomeTopBar.kt
package com.sergiom.thebestdamkebap.view.home.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sergiom.thebestdamkebap.R

/**
 * Barra superior de **Home** (Material 3).
 *
 * Objetivo clave: **altura constante** (82.dp) en todos los dispositivos.
 * - Evitamos que el TopAppBar sume la status bar: `windowInsets = WindowInsets(0.dp)`.
 * - Añadimos un Spacer previo con `windowInsetsTopHeight(WindowInsets.statusBars)`
 *   para respetar el área segura sin alterar la altura de la barra.
 *
 * Características:
 * - Logo a la izquierda (si `logoRes` != null).
 * - Píldora central con nombre de usuario y menú contextual (login/registro/cerrar sesión).
 * - Botón de menú (drawer) a la derecha.
 * - Línea inferior dibujada para separar visualmente.
 *
 * Accesibilidad / i18n:
 * - `contentDescription` del logo y del botón de menú desde `strings.xml`.
 * - El resto de iconos contextuales van con `null` al acompañarse de texto.
 */
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
        // 1) Empuja el contenido por debajo de la status bar SIN afectar a la altura del TopBar.
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // 2) TopBar de altura constante: no aplicamos insets aquí.
        TopAppBar(
            windowInsets = WindowInsets(0.dp),   // ← clave para altura fija
            modifier = modifier
                .height(82.dp)
                // Línea inferior sutil (puedes quitar la HorizontalDivider de abajo si prefieres solo esta)
                .drawBehind {
                    val y = size.height - 1f
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1f
                    )
                },

            /* IZQUIERDA: logo grande */
            navigationIcon = {
                if (logoRes != null) {
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = stringResource(R.string.home_topbar_logo_cd),
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .height(62.dp)
                            .width(122.dp)
                    )
                } else {
                    Spacer(Modifier.width(16.dp))
                }
            },

            /* CENTRO: píldora con usuario + flecha (abre menú) */
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userLabel.isNullOrBlank()) {
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
                                androidx.compose.material3.Text(
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

                            DropdownMenu(
                                expanded = userMenuOpen,
                                onDismissRequest = { userMenuOpen = false }
                            ) {
                                if (userIsGuest) {
                                    DropdownMenuItem(
                                        text = { androidx.compose.material3.Text(stringResource(R.string.home_topbar_sign_in)) },
                                        onClick = { userMenuOpen = false; onOpenLogin() }
                                    )
                                    DropdownMenuItem(
                                        text = { androidx.compose.material3.Text(stringResource(R.string.home_topbar_register)) },
                                        onClick = { userMenuOpen = false; onOpenRegister() }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
                                        text = { androidx.compose.material3.Text(stringResource(R.string.home_topbar_sign_out)) },
                                        onClick = { userMenuOpen = false; onSignOut() }
                                    )
                                }
                            }
                        }
                    }
                }
            },

            /* DERECHA: botón de menú (drawer) */
            actions = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = stringResource(R.string.home_topbar_open_menu_cd),
                        modifier = Modifier.size(40.dp), // 40 encaja mejor en 82.dp que 48
                        tint = Color.White
                    )
                }
            },

            /* Colores del AppBar */
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )

        // Si prefieres solo la línea dibujada en drawBehind, comenta esta Divider.
        HorizontalDivider(thickness = 2.dp, color = DividerDefaults.color)
    }
}

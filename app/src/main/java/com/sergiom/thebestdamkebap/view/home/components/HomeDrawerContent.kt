package com.sergiom.thebestdamkebap.view.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sergiom.thebestdamkebap.R

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
    // 🎨 Paleta neutra (sin tinte cálido)
    val drawerBg   = Color(0xFF0C0C0C)   // fondo marco
    val pillBg     = Color(0xFF171717)   // items
    val borderCol  = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
    val dividerCol = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)

    val frameShape = MaterialTheme.shapes.extraLarge
    val pill = RoundedCornerShape(26.dp)

    OutlinedCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        shape = frameShape,
        border = BorderStroke(1.25.dp, borderCol),
        colors = CardDefaults.outlinedCardColors(
            containerColor = drawerBg,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.outlinedCardElevation(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(userLabel, style = MaterialTheme.typography.titleMedium)
                    if (!userIsGuest && !userEmail.isNullOrBlank()) {
                        Text(
                            userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.25.dp, color = dividerCol)
            SectionTitle(stringResource(R.string.home_drawer_section_account))

            val itemColors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor   = pillBg,
                selectedIconColor        = MaterialTheme.colorScheme.primary,
                selectedTextColor        = MaterialTheme.colorScheme.onSurface,
                unselectedContainerColor = pillBg,
                unselectedIconColor      = MaterialTheme.colorScheme.primary,
                unselectedTextColor      = MaterialTheme.colorScheme.onSurface
            )

            @Composable
            fun DrawerItem(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
                NavigationDrawerItem(
                    label = { Text(label) },
                    selected = false,
                    onClick = onClick,
                    icon = icon,
                    colors = itemColors,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(pill)
                )
            }

            DrawerItem(stringResource(R.string.home_drawer_profile),  { Icon(Icons.Outlined.Person, null) }, onOpenProfile)
            DrawerItem(stringResource(R.string.home_drawer_addresses), { Icon(Icons.Outlined.LocationOn, null) }, onManageAddresses)
            DrawerItem(stringResource(R.string.home_drawer_recent_orders), { Icon(Icons.Outlined.FolderOpen, null) }, onOpenOrders)
            DrawerItem(stringResource(R.string.home_drawer_settings), { Icon(Icons.Outlined.Settings, null) }, onOpenSettings)

            Spacer(Modifier.weight(1f))

            HorizontalDivider(thickness = 1.25.dp, color = dividerCol)
            SectionTitle(stringResource(R.string.home_drawer_section_session))

            if (userIsGuest) {
                DrawerItem(stringResource(R.string.home_drawer_sign_in), { Icon(Icons.AutoMirrored.Outlined.Login, null) }, onLogin)
                DrawerItem(stringResource(R.string.home_drawer_create_account),  { Icon(Icons.Outlined.PersonAdd, null) }, onRegister)
            } else {
                DrawerItem(stringResource(R.string.home_drawer_sign_out), { Icon(Icons.AutoMirrored.Outlined.Logout, null) }, onLogout)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

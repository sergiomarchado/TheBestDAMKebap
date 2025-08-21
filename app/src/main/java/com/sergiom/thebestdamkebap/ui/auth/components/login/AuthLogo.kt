package com.sergiom.thebestdamkebap.ui.auth.components.login

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
 internal fun AuthLogo(@DrawableRes logoRes: Int?, shapes: Shapes) {
    if (logoRes != null) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = "Logo de DAM Burger",
            modifier = Modifier
                .size(160.dp)
                .clip(shapes.medium)
        )
        Spacer(Modifier.height(12.dp))
    } else {
        Spacer(Modifier.height(90.dp))
    }
}


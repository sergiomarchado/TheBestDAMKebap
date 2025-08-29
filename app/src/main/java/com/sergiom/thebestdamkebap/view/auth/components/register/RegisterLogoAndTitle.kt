package com.sergiom.thebestdamkebap.view.auth.components.register

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun RegisterLogoAndTitle(
    @DrawableRes logoRes: Int?,
    title: String,
    modifier: Modifier = Modifier
) {
    val shapes = MaterialTheme.shapes
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        if (logoRes != null) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = "Logo The Best DAM Kebab",
                modifier = Modifier
                    .size(160.dp)
                    .clip(shapes.medium)
            )
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(90.dp))
        }

        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
    }
}


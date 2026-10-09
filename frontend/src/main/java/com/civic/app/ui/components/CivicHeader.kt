package com.civic.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** App header on the main tabs: the wordmark on the left, the dark/light slide switch on the right. */
@Composable
fun CivicHeader(darkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit) {
    Column(Modifier.background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Civic", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            // The red dot stands for a pin on the map: the one place the wordmark carries the accent.
            Box(
                Modifier.padding(start = 3.dp, top = 9.dp).size(7.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.weight(1f))
            ThemeSlideSwitch(darkTheme = darkTheme, onDarkThemeChange = onDarkThemeChange)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * A slide switch for the colour mode. The thumb sits right (moon) in dark mode and slides left (sun) for light.
 * Exposed to accessibility services as a switch named "Dark mode".
 */
@Composable
fun ThemeSlideSwitch(darkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val trackWidth = 60.dp
    val thumbSize = 26.dp
    val inset = 3.dp
    val thumbOffset by animateDpAsState(
        targetValue = if (darkTheme) trackWidth - thumbSize - inset * 2 else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "themeThumb",
    )
    val trackColor by animateColorAsState(colors.surfaceContainerHighest, tween(220), label = "themeTrack")

    Box(
        modifier
            .semantics { contentDescription = "Dark mode" }
            .toggleable(value = darkTheme, role = Role.Switch, onValueChange = onDarkThemeChange)
            .padding(vertical = 8.dp) // keeps the touch target at 48dp tall
            .width(trackWidth)
            .height(thumbSize + inset * 2)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .padding(inset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.offset(x = thumbOffset).size(thumbSize).clip(CircleShape).background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (darkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

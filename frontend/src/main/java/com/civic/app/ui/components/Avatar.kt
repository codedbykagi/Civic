package com.civic.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * Round profile picture.
 *
 * [source] is either a local file path (device profile) or an `https://` URL (cloud account) — Coil takes both,
 * it just needs the right model type. Null falls back to the initials, so every user always has a face.
 */
@Composable
fun Avatar(source: String?, initials: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val shaped = modifier.size(size).clip(CircleShape)
    if (source.isNullOrBlank()) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = shaped) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    initials,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    } else {
        AsyncImage(
            model = if (source.startsWith("http")) source else File(source),
            contentDescription = null, // decorative: the name is always next to it
            contentScale = ContentScale.Crop,
            modifier = shaped,
        )
    }
}

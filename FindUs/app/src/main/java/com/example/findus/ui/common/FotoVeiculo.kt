package com.example.findus.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
fun FotoVeiculo(fotoBase64: String?, modifier: Modifier = Modifier) {
    if (fotoBase64 == null) return
    val miniatura = remember(fotoBase64) {
        runCatching {
            val bytes = Base64.decode(fotoBase64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
        }.getOrNull()
    } ?: return

    Image(
        bitmap = miniatura,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clipToBounds()
    )
}

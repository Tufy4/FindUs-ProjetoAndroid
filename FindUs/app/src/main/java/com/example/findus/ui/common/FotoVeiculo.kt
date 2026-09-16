package com.example.findus.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun FotoVeiculo(fotoBase64: String?, modifier: Modifier = Modifier) {
    if (fotoBase64 == null) return
    val miniatura = remember(fotoBase64) {
        runCatching {
            val bytes = Base64.decode(fotoBase64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    } ?: return

    AndroidView(
        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { it.setImageBitmap(miniatura) },
        modifier = modifier
    )
}

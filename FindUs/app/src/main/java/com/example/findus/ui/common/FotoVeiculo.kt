package com.example.findus.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

private const val LADO_MAXIMO = 320

private fun carregarMiniatura(context: Context, uri: Uri): Bitmap? = runCatching {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, medidas)
    }

    val maiorLado = maxOf(medidas.outWidth, medidas.outHeight)
    var amostra = 1
    while (maiorLado / amostra > LADO_MAXIMO) amostra *= 2

    val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opcoes)
    }
}.getOrNull()

@Composable
fun FotoVeiculo(fotoUri: String?, modifier: Modifier = Modifier) {
    if (fotoUri == null) return
    val context = LocalContext.current
    val miniatura = remember(fotoUri) { carregarMiniatura(context, Uri.parse(fotoUri)) } ?: return

    AndroidView(
        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { it.setImageBitmap(miniatura) },
        modifier = modifier
    )
}

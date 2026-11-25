package com.example.findus.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.findus.camera.arquivoDaFoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Foto do veículo lida do arquivo local (decodificada fora da thread principal). */
@Composable
fun FotoVeiculo(fotoArquivo: String?, modifier: Modifier = Modifier) {
    if (fotoArquivo == null) return
    val context = LocalContext.current
    val miniatura by produceState<ImageBitmap?>(initialValue = null, fotoArquivo) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                BitmapFactory.decodeFile(arquivoDaFoto(context, fotoArquivo).absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
    }
    val imagem = miniatura ?: return

    Image(
        bitmap = imagem,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clipToBounds()
    )
}

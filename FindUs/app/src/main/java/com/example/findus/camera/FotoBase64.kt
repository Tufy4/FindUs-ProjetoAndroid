package com.example.findus.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object FotoBase64 {
    private const val LADO_MAXIMO = 640
    private const val TAMANHO_MAXIMO = 700_000

    fun paraBase64(context: Context, uri: Uri): String? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, medidas)
        }

        val maiorLado = maxOf(medidas.outWidth, medidas.outHeight)
        var amostra = 1
        while (maiorLado / amostra > LADO_MAXIMO) amostra *= 2

        val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opcoes)
        } ?: return null

        val saida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, saida)
        val texto = Base64.encodeToString(saida.toByteArray(), Base64.NO_WRAP)
        return if (texto.length > TAMANHO_MAXIMO) null else texto
    }
}

package com.example.findus.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

private const val PASTA_FOTOS = "fotos_veiculos"

/** Arquivo local de uma foto de veículo a partir do nome guardado no Room. */
fun arquivoDaFoto(context: Context, nome: String): File =
    File(File(context.filesDir, PASTA_FOTOS), nome)

/**
 * Fotos dos veículos no sistema de arquivos do aparelho, como pede o manual
 * (dados estruturados no SQLite, fotos no filesystem local). O Room guarda só o
 * nome do arquivo. O Firebase Storage não é usado (plano pago); para a foto aparecer
 * nos outros aparelhos ela vai em Base64 apenas no documento do Firestore.
 *
 * O nome do arquivo é o hash do conteúdo: a mesma foto vinda do Firestore cai no
 * mesmo arquivo e não é regravada a cada sincronização.
 */
class FotoArmazenamento(context: Context) {
    private val appContext = context.applicationContext
    private val pasta = File(appContext.filesDir, PASTA_FOTOS).apply { mkdirs() }

    /**
     * Reduz a foto capturada pela câmera (lado máximo de 640 px, já na orientação certa),
     * grava em JPEG e apaga o arquivo original em tamanho cheio. Devolve o nome do arquivo.
     */
    fun salvarCaptura(uri: Uri): String? {
        val resolver = appContext.contentResolver
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }

        val maiorLado = maxOf(medidas.outWidth, medidas.outHeight)
        if (maiorLado <= 0) return null
        var amostra = 1
        while (maiorLado / amostra > LADO_MAXIMO) amostra *= 2

        val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
        val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcoes) } ?: return null
        val rotacao = resolver.openInputStream(uri)?.use { grausDaOrientacao(ExifInterface(it)) } ?: 0f
        val bitmap = if (rotacao == 0f) {
            original
        } else {
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, Matrix().apply { postRotate(rotacao) }, true)
        }

        val saida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida)
        val nome = salvarBytes(saida.toByteArray())
        runCatching { resolver.delete(uri, null, null) }
        return nome
    }

    /** Grava a foto recebida do Firestore e devolve o nome do arquivo local. */
    fun salvarDoRemoto(base64: String): String? {
        val bytes = runCatching { Base64.decode(base64, Base64.NO_WRAP) }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null
        return salvarBytes(bytes)
    }

    /** Conteúdo da foto em Base64 para o documento do Firestore, ou null se não houver ou for grande demais. */
    fun lerBase64(nome: String?): String? {
        val arquivo = nome?.let { File(pasta, it) }?.takeIf { it.exists() } ?: return null
        val texto = Base64.encodeToString(arquivo.readBytes(), Base64.NO_WRAP)
        return texto.takeIf { it.length <= TAMANHO_MAXIMO_BASE64 }
    }

    private fun salvarBytes(bytes: ByteArray): String {
        val nome = "foto_${hash(bytes)}.jpg"
        val destino = File(pasta, nome)
        if (!destino.exists()) destino.writeBytes(bytes)
        return nome
    }

    private fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun grausDaOrientacao(exif: ExifInterface): Float =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

    private companion object {
        const val LADO_MAXIMO = 640
        const val QUALIDADE_JPEG = 60
        // Limite de 1 MiB por documento do Firestore, com folga para os outros campos.
        const val TAMANHO_MAXIMO_BASE64 = 700_000
    }
}

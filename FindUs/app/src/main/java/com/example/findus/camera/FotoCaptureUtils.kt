package com.example.findus.camera

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Cria o arquivo/URI (via FileProvider) usado pelo intent nativo de captura de foto. */
object FotoCaptureUtils {
    fun criarUriParaFoto(context: Context): android.net.Uri {
        val pasta = File(context.getExternalFilesDir(null), "fotos_veiculos").apply { mkdirs() }
        val nomeArquivo = "veiculo_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".jpg"
        val arquivo = File(pasta, nomeArquivo)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
    }
}

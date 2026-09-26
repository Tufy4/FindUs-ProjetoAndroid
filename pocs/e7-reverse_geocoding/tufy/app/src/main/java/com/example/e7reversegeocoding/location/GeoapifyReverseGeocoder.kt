package com.example.e7reversegeocoding.location

import com.example.e7reversegeocoding.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reverse geocoding pela API HTTP da Geoapify, a mesma chave usada nos tiles e nas rotas.
 * Doc: https://apidocs.geoapify.com/docs/geocoding/reverse-geocoding/
 */
class GeoapifyReverseGeocoder(
    private val apiKey: String = BuildConfig.GEOAPIFY_API_KEY
) : ProvedorEndereco {

    override suspend fun buscar(latitude: Double, longitude: Double): Endereco? {
        if (apiKey.isBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    "https://api.geoapify.com/v1/geocode/reverse" +
                        "?lat=$latitude&lon=$longitude&lang=pt&limit=1&format=json&apiKey=$apiKey"
                )
                val conexao = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                }
                try {
                    if (conexao.responseCode != HttpURLConnection.HTTP_OK) {
                        null
                    } else {
                        parseRespostaGeoapify(conexao.inputStream.bufferedReader().use { it.readText() })
                    }
                } finally {
                    conexao.disconnect()
                }
            }.getOrNull()
        }
    }
}

/** Converte o JSON (format=json) da Geoapify em [Endereco]. Separado para teste unitário. */
internal fun parseRespostaGeoapify(corpo: String): Endereco? {
    val resultado = JSONObject(corpo).optJSONArray("results")?.optJSONObject(0) ?: return null
    val formatado = resultado.texto("formatted") ?: return null
    return Endereco(
        logradouro = resultado.texto("street"),
        numero = resultado.texto("housenumber"),
        bairro = resultado.texto("suburb") ?: resultado.texto("district") ?: resultado.texto("quarter"),
        cidade = resultado.texto("city") ?: resultado.texto("town") ?: resultado.texto("village")
            ?: resultado.texto("municipality"),
        estado = resultado.texto("state_code") ?: resultado.texto("state"),
        cep = resultado.texto("postcode"),
        formatado = formatado,
        fonte = FonteEndereco.GEOAPIFY
    )
}

private fun JSONObject.texto(chave: String): String? =
    if (isNull(chave)) null else optString(chave).takeIf { it.isNotBlank() }

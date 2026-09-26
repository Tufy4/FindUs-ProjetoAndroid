package com.example.findus.location

import com.example.findus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ReverseGeocoder {
    suspend fun endereco(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                "https://api.geoapify.com/v1/geocode/reverse" +
                    "?lat=$latitude&lon=$longitude&lang=pt&limit=1&format=json&apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
            )
            val conexao = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 5_000
            }
            try {
                if (conexao.responseCode != HttpURLConnection.HTTP_OK) {
                    null
                } else {
                    val corpo = conexao.inputStream.bufferedReader().use { it.readText() }
                    JSONObject(corpo).optJSONArray("results")?.optJSONObject(0)?.optString("formatted")
                        ?.takeIf { it.isNotBlank() }
                }
            } finally {
                conexao.disconnect()
            }
        }.getOrNull()
    }
}

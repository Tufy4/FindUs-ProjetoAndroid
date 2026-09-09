package com.example.findus.location

import com.example.findus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class RotaService {
    suspend fun rota(origem: Coordenada, destino: Coordenada): List<Coordenada> = withContext(Dispatchers.IO) {
        val pontos = runCatching {
            val url = "https://api.geoapify.com/v1/routing" +
                "?waypoints=${origem.latitude},${origem.longitude}%7C${destino.latitude},${destino.longitude}" +
                "&mode=drive&apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
            val corpo = URL(url).readText()
            val coordenadas = JSONObject(corpo)
                .getJSONArray("features")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")

            val lista = mutableListOf<Coordenada>()
            for (i in 0 until coordenadas.length()) {
                val segmento = coordenadas.getJSONArray(i)
                for (j in 0 until segmento.length()) {
                    val par = segmento.getJSONArray(j)
                    lista.add(Coordenada(par.getDouble(1), par.getDouble(0)))
                }
            }
            lista
        }.getOrNull()

        if (pontos.isNullOrEmpty()) listOf(origem, destino) else pontos
    }
}

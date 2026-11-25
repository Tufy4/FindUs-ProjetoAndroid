package com.example.findus.location

import com.example.findus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RotaService {
    /** Só a geometria da rota (telas de rota e simulação do controlador). */
    suspend fun rota(origem: Coordenada, destino: Coordenada): List<Coordenada> =
        rotaDetalhada(origem, destino).pontos

    /**
     * Rota com distância, tempo estimado e instruções em português. Sem chave da Geoapify
     * ou sem internet, devolve uma linha reta entre origem e destino.
     */
    suspend fun rotaDetalhada(origem: Coordenada, destino: Coordenada): RotaDetalhada = withContext(Dispatchers.IO) {
        runCatching { buscarNaApi(origem, destino) }.getOrNull() ?: rotaEmLinhaReta(origem, destino)
    }

    private fun buscarNaApi(origem: Coordenada, destino: Coordenada): RotaDetalhada? {
        if (BuildConfig.GEOAPIFY_API_KEY.isBlank()) return null
        val url = URL(
            "https://api.geoapify.com/v1/routing" +
                "?waypoints=${origem.latitude},${origem.longitude}%7C${destino.latitude},${destino.longitude}" +
                "&mode=drive&lang=pt-BR&apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
        )
        // Sem timeout explícito a chamada pode ficar presa indefinidamente em rede ruim.
        val conexao = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
        }
        try {
            if (conexao.responseCode != HttpURLConnection.HTTP_OK) return null
            val corpo = conexao.inputStream.bufferedReader().use { it.readText() }
            return interpretarRespostaRota(corpo)
        } finally {
            conexao.disconnect()
        }
    }
}

/**
 * Lê a resposta GeoJSON da Geoapify. A geometria é um MultiLineString (uma linha por perna)
 * com pares [lon, lat]; os índices dos passos são relativos à linha da própria perna.
 */
internal fun interpretarRespostaRota(corpo: String): RotaDetalhada? {
    val feature = JSONObject(corpo).optJSONArray("features")?.optJSONObject(0) ?: return null
    val linhas = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return null

    val pontos = mutableListOf<Coordenada>()
    val inicioDaPerna = mutableListOf<Int>()
    for (i in 0 until linhas.length()) {
        val linha = linhas.getJSONArray(i)
        inicioDaPerna.add(pontos.size)
        for (j in 0 until linha.length()) {
            val par = linha.getJSONArray(j)
            pontos.add(Coordenada(par.getDouble(1), par.getDouble(0)))
        }
    }
    if (pontos.size < 2) return null

    val propriedades = feature.optJSONObject("properties")
    val passos = mutableListOf<PassoRota>()
    val pernas = propriedades?.optJSONArray("legs")
    if (pernas != null) {
        for (p in 0 until pernas.length()) {
            val deslocamento = inicioDaPerna.getOrNull(p) ?: break
            val etapas = pernas.optJSONObject(p)?.optJSONArray("steps") ?: continue
            for (s in 0 until etapas.length()) {
                val etapa = etapas.optJSONObject(s) ?: continue
                passos.add(
                    PassoRota(
                        indiceInicio = (deslocamento + etapa.optInt("from_index", 0)).coerceIn(0, pontos.lastIndex),
                        indiceFim = (deslocamento + etapa.optInt("to_index", 0)).coerceIn(0, pontos.lastIndex),
                        distanciaMetros = etapa.optDouble("distance", 0.0),
                        tempoSegundos = etapa.optDouble("time", 0.0),
                        instrucao = etapa.optJSONObject("instruction")?.optString("text").orEmpty().ifBlank { "Siga em frente" }
                    )
                )
            }
        }
    }

    val distanciaCalculada = pontos.zipWithNext { a, b -> distanciaMetros(a, b) }.sum()
    val distancia = propriedades?.optDouble("distance", distanciaCalculada) ?: distanciaCalculada
    val tempoPadrao = distancia / (VELOCIDADE_MEDIA_PADRAO_KMH / 3.6)
    val tempo = propriedades?.optDouble("time", tempoPadrao) ?: tempoPadrao
    return RotaDetalhada(pontos, distancia, tempo, passos, calculadaPelaApi = true)
}

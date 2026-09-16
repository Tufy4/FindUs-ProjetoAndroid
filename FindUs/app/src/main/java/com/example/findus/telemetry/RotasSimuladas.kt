package com.example.findus.telemetry

import com.example.findus.data.enum.EstadoPortas
import com.example.findus.location.Coordenada
import com.example.findus.location.RotaService
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Monta o trajeto do simulador de telemetria a partir da rota real devolvida
 * pelo RotaService, reamostrada em um número fixo de passos.
 */
object RotasSimuladas {
    val destinoPadrao = Coordenada(-21.806911237893907, -48.17465712954804)

    private const val PASSOS = 30

    suspend fun trajeto(
        rotaService: RotaService,
        origem: Coordenada,
        destino: Coordenada = destinoPadrao
    ): List<PontoRota> {
        val pontos = rotaService.rota(origem, destino)
        return (0..PASSOS).map { indice ->
            val posicao = indice.toDouble() * (pontos.size - 1) / PASSOS
            val anterior = pontos[floor(posicao).toInt()]
            val proximo = pontos[ceil(posicao).toInt()]
            val fracao = posicao - floor(posicao)
            val latitude = anterior.latitude + (proximo.latitude - anterior.latitude) * fracao
            val longitude = anterior.longitude + (proximo.longitude - anterior.longitude) * fracao
            val velocidade = when (indice) {
                0, PASSOS -> 0.0
                1, PASSOS - 1 -> 20.0
                else -> 45.0 + (indice % 3) * 8.0
            }
            val estadoPortas = if (indice == 0 || indice == PASSOS) EstadoPortas.ABERTA else EstadoPortas.FECHADA
            PontoRota(latitude, longitude, velocidade, estadoPortas, motorLigado = indice != 0)
        }
    }
}

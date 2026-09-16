package com.example.findus.telemetry

import com.example.findus.data.enum.EstadoPortas

/**
 * Trajetos mock (Centro de Distribuição -> ponto de entrega) usados pelo
 * simulador de telemetria. Cada veículo recebe um pequeno deslocamento para
 * não sobrepor marcadores no mapa.
 */
object RotasSimuladas {
    val centroDistribuicao = 23.2237 to 45.8937 // São José dos Campos (lat/lon positivos, sinal aplicado abaixo)

    fun trajetoPadrao(veiculoId: String): List<PontoRota> {
        val offset = (veiculoId.hashCode() % 5) * 0.004
        val origemLat = -23.2237 - offset
        val origemLon = -45.8937 - offset
        val destinoLat = -23.1791 - offset
        val destinoLon = -45.8641 - offset

        val passos = 10
        return (0..passos).map { indice ->
            val fracao = indice.toDouble() / passos
            val lat = origemLat + (destinoLat - origemLat) * fracao
            val lon = origemLon + (destinoLon - origemLon) * fracao
            val velocidade = when (indice) {
                0, passos -> 0.0
                1, passos - 1 -> 20.0
                else -> 45.0 + (indice % 3) * 8.0
            }
            val estadoPortas = if (indice == 0 || indice == passos) EstadoPortas.ABERTA else EstadoPortas.FECHADA
            val motorLigado = indice != 0
            PontoRota(lat, lon, velocidade, estadoPortas, motorLigado)
        }
    }
}

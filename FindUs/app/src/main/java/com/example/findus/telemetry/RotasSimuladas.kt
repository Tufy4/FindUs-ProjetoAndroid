package com.example.findus.telemetry

import com.example.findus.data.enum.EstadoPortas
import com.example.findus.location.Coordenada
import com.example.findus.location.RotaService

/**
 * Monta o trajeto do simulador de telemetria do controlador a partir da rota real
 * devolvida pelo RotaService, reamostrada em um número fixo de passos.
 */
object RotasSimuladas {
    val destinoPadrao = Coordenada(-21.806911237893907, -48.17465712954804)

    private const val PASSOS = 30

    suspend fun trajeto(
        rotaService: RotaService,
        origem: Coordenada,
        destino: Coordenada = destinoPadrao
    ): List<PontoRota> {
        val percurso = PercursoSimulado(rotaService.rotaDetalhada(origem, destino))
        return (0..PASSOS).map { indice ->
            // Pontos igualmente espaçados pela distância ao longo da rota. Antes a reamostragem
            // era pelo índice dos vértices, que se concentram nas curvas: o veículo andava
            // poucos metros nas curvas e "saltava" centenas de metros nas retas.
            val posicao = percurso.posicaoEm(percurso.distanciaTotal * indice / PASSOS)
            val velocidade = when (indice) {
                0, PASSOS -> 0.0
                1, PASSOS - 1 -> 20.0
                else -> 45.0 + (indice % 3) * 8.0
            }
            val estadoPortas = if (indice == 0 || indice == PASSOS) EstadoPortas.ABERTA else EstadoPortas.FECHADA
            PontoRota(posicao.latitude, posicao.longitude, velocidade, estadoPortas, motorLigado = indice != 0 && indice != PASSOS)
        }
    }
}

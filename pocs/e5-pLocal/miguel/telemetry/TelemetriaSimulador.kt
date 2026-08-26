package com.example.findus.telemetry

import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import kotlinx.coroutines.delay

/**
 * Percorre um trajeto pré-definido gravando um registro de telemetria por
 * ponto no Room, simulando o rastreamento em tempo real de um veículo.
 */
class TelemetriaSimulador(
    private val veiculoId: Long,
    private val trajeto: List<PontoRota>,
    private val veiculoRepository: VeiculoRepository,
    private val telemetriaRepository: TelemetriaRepository,
    private val intervaloMs: Long = 3000L
) {
    suspend fun executar() {
        veiculoRepository.atualizarStatus(veiculoId, StatusOperacionalVeiculo.EM_ROTA)
        for (ponto in trajeto) {
            telemetriaRepository.registrar(
                RegistroTelemetriaEntity(
                    veiculoId = veiculoId,
                    latitude = ponto.latitude,
                    longitude = ponto.longitude,
                    velocidade = ponto.velocidade,
                    estadoPortas = ponto.estadoPortas,
                    motorLigado = ponto.motorLigado
                )
            )
            delay(intervaloMs)
        }
        veiculoRepository.atualizarStatus(veiculoId, StatusOperacionalVeiculo.DISPONIVEL)
    }
}

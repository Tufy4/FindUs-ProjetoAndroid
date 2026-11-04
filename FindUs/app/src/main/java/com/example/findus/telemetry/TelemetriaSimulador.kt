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
    private val veiculoId: String,
    private val trajeto: List<PontoRota>,
    private val veiculoRepository: VeiculoRepository,
    private val telemetriaRepository: TelemetriaRepository,
    private val intervaloMs: Long = 2000L,
    // Quando a simulação faz parte de uma entrega, o veículo continua "em rota"
    // até o motorista concluir a entrega, mesmo depois de chegar ao destino.
    private val liberarVeiculoAoFinal: Boolean = true
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
        if (liberarVeiculoAoFinal) {
            veiculoRepository.atualizarStatus(veiculoId, StatusOperacionalVeiculo.DISPONIVEL)
        }
    }
}

package com.example.findus.telemetry

import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Controla o ciclo de vida dos simuladores de telemetria de cada veículo,
 * permitindo iniciar/parar a simulação a partir do painel do Controlador.
 */
class TelemetriaSimuladorManager(
    private val veiculoRepository: VeiculoRepository,
    private val telemetriaRepository: TelemetriaRepository,
    private val escopo: CoroutineScope
) {
    private val jobsAtivos = mutableMapOf<String, Job>()

    fun estaAtivo(veiculoId: String): Boolean = jobsAtivos[veiculoId]?.isActive == true

    fun iniciar(veiculoId: String, trajeto: List<PontoRota> = RotasSimuladas.trajetoPadrao(veiculoId)) {
        if (estaAtivo(veiculoId)) return
        jobsAtivos[veiculoId] = escopo.launch {
            TelemetriaSimulador(veiculoId, trajeto, veiculoRepository, telemetriaRepository).executar()
        }
    }

    fun parar(veiculoId: String) {
        jobsAtivos[veiculoId]?.cancel()
        jobsAtivos.remove(veiculoId)
    }

    fun pararTodos() {
        jobsAtivos.values.forEach { it.cancel() }
        jobsAtivos.clear()
    }
}

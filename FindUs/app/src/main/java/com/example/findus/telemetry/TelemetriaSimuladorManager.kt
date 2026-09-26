package com.example.findus.telemetry

import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.location.Coordenada
import com.example.findus.location.GeofenceMonitor
import com.example.findus.location.LocationHelper
import com.example.findus.location.RotaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Controla o ciclo de vida dos simuladores de telemetria de cada veículo,
 * permitindo iniciar/parar a simulação a partir do painel do Controlador.
 */
class TelemetriaSimuladorManager(
    private val veiculoRepository: VeiculoRepository,
    private val telemetriaRepository: TelemetriaRepository,
    private val locationHelper: LocationHelper,
    private val rotaService: RotaService,
    private val geofenceMonitor: GeofenceMonitor,
    private val escopo: CoroutineScope
) {
    private val jobsAtivos = mutableMapOf<String, Job>()

    private val _trajetos = MutableStateFlow<Map<String, List<Coordenada>>>(emptyMap())
    val trajetos: StateFlow<Map<String, List<Coordenada>>> = _trajetos

    fun estaAtivo(veiculoId: String): Boolean = jobsAtivos[veiculoId]?.isActive == true

    fun iniciar(veiculoId: String) {
        if (estaAtivo(veiculoId)) return
        jobsAtivos[veiculoId] = escopo.launch {
            val origem = locationHelper.obterLocalizacaoOuPadrao()
            geofenceMonitor.definirCentro(origem)
            val trajeto = RotasSimuladas.trajeto(rotaService, origem)
            _trajetos.value += veiculoId to trajeto.map { Coordenada(it.latitude, it.longitude) }
            try {
                TelemetriaSimulador(veiculoId, trajeto, veiculoRepository, telemetriaRepository).executar()
            } finally {
                _trajetos.value -= veiculoId
            }
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

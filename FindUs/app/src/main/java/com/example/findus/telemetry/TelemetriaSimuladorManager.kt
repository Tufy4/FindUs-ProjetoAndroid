package com.example.findus.telemetry

import com.example.findus.data.enum.StatusEntrega
import com.example.findus.data.repository.CentroDistribuicaoRepository
import com.example.findus.data.repository.EntregaRepository
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.location.Coordenada
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
 *
 * Se o veículo tem uma entrega em rota (ou pendente), a simulação vai do centro de
 * distribuição da entrega até o ponto de entrega, e uma entrega pendente passa a
 * "em rota". Sem entrega, mantém o comportamento antigo: da posição atual até o
 * destino padrão.
 */
class TelemetriaSimuladorManager(
    private val veiculoRepository: VeiculoRepository,
    private val telemetriaRepository: TelemetriaRepository,
    private val entregaRepository: EntregaRepository,
    private val centroRepository: CentroDistribuicaoRepository,
    private val locationHelper: LocationHelper,
    private val rotaService: RotaService,
    private val escopo: CoroutineScope
) {
    private val jobsAtivos = mutableMapOf<String, Job>()

    private val _trajetos = MutableStateFlow<Map<String, List<Coordenada>>>(emptyMap())
    val trajetos: StateFlow<Map<String, List<Coordenada>>> = _trajetos

    fun estaAtivo(veiculoId: String): Boolean = jobsAtivos[veiculoId]?.isActive == true

    fun iniciar(veiculoId: String) {
        if (estaAtivo(veiculoId)) return
        jobsAtivos[veiculoId] = escopo.launch {
            val entrega = entregaRepository.entregaAtivaDoVeiculo(veiculoId)
            val centro = entrega?.let { centroRepository.buscarPorId(it.centroDistribuicaoId) }

            val viaEntrega = entrega != null && centro != null
            val origem: Coordenada
            val destino: Coordenada
            if (entrega != null && centro != null) {
                origem = Coordenada(centro.latitude, centro.longitude)
                destino = Coordenada(entrega.latitudeDestino, entrega.longitudeDestino)
                if (entrega.status == StatusEntrega.PENDENTE) entregaRepository.iniciar(entrega)
            } else {
                origem = locationHelper.obterLocalizacaoOuPadrao()
                destino = RotasSimuladas.destinoPadrao
            }

            val trajeto = RotasSimuladas.trajeto(rotaService, origem, destino)
            _trajetos.value += veiculoId to trajeto.map { Coordenada(it.latitude, it.longitude) }
            try {
                TelemetriaSimulador(
                    veiculoId = veiculoId,
                    trajeto = trajeto,
                    veiculoRepository = veiculoRepository,
                    telemetriaRepository = telemetriaRepository,
                    liberarVeiculoAoFinal = !viaEntrega
                ).executar()
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

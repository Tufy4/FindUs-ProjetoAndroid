package com.example.findus.telemetry

import com.example.findus.data.enum.StatusEntrega
import com.example.findus.data.repository.CentroDistribuicaoRepository
import com.example.findus.data.repository.EntregaRepository
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.location.COORDENADA_PADRAO
import com.example.findus.location.Coordenada
import com.example.findus.location.LocationHelper
import com.example.findus.location.OrigemPerimetro
import com.example.findus.location.PerimetroMonitorado
import com.example.findus.location.RotaService
import com.example.findus.location.distanciaMetros
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Acima disso, a posição do aparelho é considerada fora da região da simulação. */
private const val DISTANCIA_MAXIMA_DA_REGIAO_METROS = 200_000.0

/**
 * Controla o ciclo de vida dos simuladores de telemetria de cada veículo,
 * permitindo iniciar/parar a simulação a partir do painel do Controlador.
 *
 * De onde a simulação sai:
 * - veículo com entrega em rota (ou pendente): do centro de distribuição da entrega até o
 *   ponto de entrega; uma entrega pendente passa a "em rota";
 * - sem entrega: do primeiro centro de distribuição cadastrado até o destino padrão;
 * - sem entrega e sem centro cadastrado: da posição do aparelho, com um perímetro
 *   temporário em volta do ponto de partida (comportamento da versão anterior).
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

    private val _perimetrosDaSimulacao = MutableStateFlow<Map<String, PerimetroMonitorado>>(emptyMap())

    /** Perímetros temporários de ponto de partida, por veículo, enquanto a simulação roda. */
    val perimetrosDaSimulacao: StateFlow<Map<String, PerimetroMonitorado>> = _perimetrosDaSimulacao

    fun estaAtivo(veiculoId: String): Boolean = jobsAtivos[veiculoId]?.isActive == true

    fun iniciar(veiculoId: String) {
        if (estaAtivo(veiculoId)) return
        jobsAtivos[veiculoId] = escopo.launch {
            val entrega = entregaRepository.entregaAtivaDoVeiculo(veiculoId)
            val centroDaEntrega = entrega?.let { centroRepository.buscarPorId(it.centroDistribuicaoId) }
            val viaEntrega = entrega != null && centroDaEntrega != null

            val origem: Coordenada
            val destino: Coordenada
            if (entrega != null && centroDaEntrega != null) {
                origem = Coordenada(centroDaEntrega.latitude, centroDaEntrega.longitude)
                destino = Coordenada(entrega.latitudeDestino, entrega.longitudeDestino)
                if (entrega.status == StatusEntrega.PENDENTE) entregaRepository.iniciar(entrega)
            } else {
                val primeiroCentro = centroRepository.observarTodos().first().firstOrNull()
                origem = primeiroCentro?.let { Coordenada(it.latitude, it.longitude) } ?: origemDoAparelho()
                destino = RotasSimuladas.destinoPadrao
                if (primeiroCentro == null) {
                    _perimetrosDaSimulacao.value += veiculoId to PerimetroMonitorado(
                        chave = "partida:$veiculoId",
                        nome = "Ponto de partida da simulação",
                        origem = OrigemPerimetro.CENTRO_DISTRIBUICAO,
                        centro = origem,
                        raioMetros = 1000.0,
                        veiculoIds = setOf(veiculoId)
                    )
                }
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
                _perimetrosDaSimulacao.value -= veiculoId
            }
        }
    }

    /**
     * Posição do aparelho como ponto de partida. O emulador do Android informa por padrão
     * Mountain View (EUA); como não há rota de lá até o destino padrão, nesse caso a
     * simulação sai da coordenada padrão da região.
     */
    private suspend fun origemDoAparelho(): Coordenada {
        val posicao = locationHelper.obterLocalizacaoOuPadrao()
        val foraDaRegiao = distanciaMetros(posicao, COORDENADA_PADRAO) > DISTANCIA_MAXIMA_DA_REGIAO_METROS
        return if (foraDaRegiao) COORDENADA_PADRAO else posicao
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

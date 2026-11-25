package com.example.findus.ui.motorista.simulacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.enum.EstadoPortas
import com.example.findus.data.enum.StatusEntrega
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.repository.CentroDistribuicaoRepository
import com.example.findus.data.repository.EntregaRepository
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.domain.RegrasNegocio
import com.example.findus.location.Coordenada
import com.example.findus.location.GeofenceNotificador
import com.example.findus.location.LocationHelper
import com.example.findus.location.RotaService
import com.example.findus.telemetry.EstadoPercurso
import com.example.findus.telemetry.Manobra
import com.example.findus.telemetry.PercursoSimulado
import com.example.findus.telemetry.TelemetriaSimuladorManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class FaseSimulacao { CARREGANDO, PRONTA, EM_ANDAMENTO, PAUSADA, CHEGOU, ERRO }

/** Raio do perímetro do centro de distribuição, desenhado no mapa da simulação. */
data class PerimetroDaSimulacao(val centro: Coordenada, val raioMetros: Double)

data class SimulacaoUiState(
    val fase: FaseSimulacao = FaseSimulacao.CARREGANDO,
    val erro: String? = null,
    val titulo: String = "Simulação do percurso",
    val origemDescricao: String = "",
    val destinoDescricao: String = "",
    val placa: String? = null,
    val geraTelemetria: Boolean = false,
    val rotaPelaApi: Boolean = true,
    val rota: List<Coordenada> = emptyList(),
    val percorrida: List<Coordenada> = emptyList(),
    val restante: List<Coordenada> = emptyList(),
    val origem: Coordenada? = null,
    val destino: Coordenada? = null,
    val perimetroOrigem: PerimetroDaSimulacao? = null,
    val perimetroDestino: PerimetroDaSimulacao? = null,
    val posicao: Coordenada? = null,
    val rumoGraus: Float = 0f,
    val velocidadeKmh: Int = 0,
    val distanciaTotalMetros: Double = 0.0,
    val distanciaRestanteMetros: Double = 0.0,
    val tempoRestanteSegundos: Double = 0.0,
    val progresso: Float = 0f,
    val instrucaoAtual: String? = null,
    val proximaManobra: Manobra? = null,
    val motorLigado: Boolean = false,
    val portas: EstadoPortas = EstadoPortas.FECHADA,
    val fatorVelocidade: Int = 1,
    val ehEntrega: Boolean = false,
    val entregaConcluida: Boolean = false,
    val avisoGeofence: String? = null
)

/**
 * Simulação do percurso do ponto de vista do motorista: o veículo anda pela rota real
 * (Geoapify) com velocidade, frenagem nas manobras e instruções passo a passo, e cada
 * posição gera um registro de telemetria do veículo. Assim o controlador acompanha o
 * movimento no mapa da frota e o geofencing dispara a saída do centro e a chegada ao
 * ponto de entrega, como no rastreamento real.
 *
 * Aberta por uma entrega ([entregaId]): sai do centro de distribuição e vai até o ponto
 * de entrega; uma entrega pendente passa a "em rota" ao iniciar. Aberta pela tela de rota
 * livre ([destinoLivre]): sai da posição atual do aparelho.
 */
class SimulacaoPercursoViewModel(
    private val entregaId: String?,
    private val destinoLivre: Coordenada?,
    private val entregaRepository: EntregaRepository,
    private val centroRepository: CentroDistribuicaoRepository,
    private val veiculoRepository: VeiculoRepository,
    private val motoristaRepository: MotoristaRepository,
    private val telemetriaRepository: TelemetriaRepository,
    private val rotaService: RotaService,
    private val locationHelper: LocationHelper,
    private val telemetriaSimuladorManager: TelemetriaSimuladorManager,
    private val geofenceNotificador: GeofenceNotificador,
    private val escopoApp: CoroutineScope
) : ViewModel() {
    private val _estado = MutableStateFlow(SimulacaoUiState())
    val estado: StateFlow<SimulacaoUiState> = _estado.asStateFlow()

    private var percurso: PercursoSimulado? = null
    private var progressoAtual = EstadoPercurso()
    private var veiculoId: String? = null
    private var execucao: Job? = null
    private var limpezaAviso: Job? = null
    private var iniciando = false

    init {
        viewModelScope.launch { carregar() }
        viewModelScope.launch {
            geofenceNotificador.eventos.collect { evento ->
                if (evento.veiculoId != veiculoId) return@collect
                _estado.update { it.copy(avisoGeofence = evento.mensagem()) }
                limpezaAviso?.cancel()
                limpezaAviso = viewModelScope.launch {
                    delay(DURACAO_AVISO_MS)
                    _estado.update { it.copy(avisoGeofence = null) }
                }
            }
        }
    }

    private suspend fun carregar() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val motorista = motoristaRepository.observarTodos().first().find { it.uid == uid }

        val origem: Coordenada
        val destino: Coordenada
        val titulo: String
        val origemDescricao: String
        val destinoDescricao: String
        var perimetroOrigem: PerimetroDaSimulacao? = null
        var perimetroDestino: PerimetroDaSimulacao? = null

        if (entregaId != null) {
            val entrega = entregaRepository.buscarPorId(entregaId)
            val problema = when {
                entrega == null || entrega.deletado -> "Entrega não encontrada."
                motorista != null && entrega.motoristaId != motorista.id -> "Esta entrega não está atribuída a você."
                !RegrasNegocio.entregaAtiva(entrega) -> "Esta entrega já foi encerrada."
                else -> null
            }
            if (problema != null || entrega == null) {
                falhar(problema ?: "Entrega não encontrada.")
                return
            }
            val centro = centroRepository.buscarPorId(entrega.centroDistribuicaoId)
            origem = centro?.let { Coordenada(it.latitude, it.longitude) } ?: locationHelper.obterLocalizacaoOuPadrao()
            destino = Coordenada(entrega.latitudeDestino, entrega.longitudeDestino)
            veiculoId = entrega.veiculoId
            titulo = "Entrega: ${entrega.descricao}"
            origemDescricao = centro?.nome?.let { "Centro $it" } ?: "Sua posição (centro não encontrado)"
            destinoDescricao = entrega.enderecoDestino.ifBlank { "Ponto de entrega" }
            perimetroOrigem = centro?.let { PerimetroDaSimulacao(origem, it.raioMetros) }
            perimetroDestino = PerimetroDaSimulacao(destino, RegrasNegocio.RAIO_PONTO_ENTREGA_METROS)
        } else {
            val alvo = destinoLivre
            if (alvo == null) {
                falhar("Escolha um destino no mapa antes de simular.")
                return
            }
            origem = locationHelper.obterLocalizacaoOuPadrao()
            destino = alvo
            veiculoId = motorista?.veiculoId
            titulo = "Rota até o ponto escolhido"
            origemDescricao = "Sua posição"
            destinoDescricao = "Destino escolhido no mapa"
        }

        val placa = veiculoId?.let { veiculoRepository.buscarPorId(it) }?.placa
        val rota = rotaService.rotaDetalhada(origem, destino)
        val novoPercurso = PercursoSimulado(rota)
        percurso = novoPercurso
        progressoAtual = EstadoPercurso()

        _estado.update {
            it.copy(
                fase = FaseSimulacao.PRONTA,
                titulo = titulo,
                origemDescricao = origemDescricao,
                destinoDescricao = destinoDescricao,
                placa = placa,
                geraTelemetria = veiculoId != null,
                rotaPelaApi = rota.calculadaPelaApi,
                rota = novoPercurso.pontos,
                origem = origem,
                destino = destino,
                perimetroOrigem = perimetroOrigem,
                perimetroDestino = perimetroDestino,
                distanciaTotalMetros = novoPercurso.distanciaTotal,
                ehEntrega = entregaId != null
            )
        }
        publicar(progressoAtual, motorLigado = false, portas = EstadoPortas.FECHADA)
    }

    private fun falhar(mensagem: String) {
        _estado.update { it.copy(fase = FaseSimulacao.ERRO, erro = mensagem) }
    }

    /** Inicia (ou retoma, se pausada) a simulação. */
    fun iniciarOuContinuar() {
        when (_estado.value.fase) {
            FaseSimulacao.PAUSADA -> {
                _estado.update { it.copy(fase = FaseSimulacao.EM_ANDAMENTO, erro = null) }
                return
            }
            FaseSimulacao.PRONTA -> if (iniciando) return
            else -> return
        }
        iniciando = true
        viewModelScope.launch {
            try {
                if (!prepararEntrega()) return@launch
                // Evita dois simuladores escrevendo a telemetria do mesmo veículo neste aparelho.
                veiculoId?.let { telemetriaSimuladorManager.parar(it) }
                _estado.update { it.copy(fase = FaseSimulacao.EM_ANDAMENTO, erro = null) }
                executar()
            } finally {
                iniciando = false
            }
        }
    }

    /** Uma entrega pendente passa a "em rota", respeitando as regras de negócio. */
    private suspend fun prepararEntrega(): Boolean {
        val id = entregaId ?: return true
        val entrega = entregaRepository.buscarPorId(id)
        if (entrega == null) {
            falhar("Entrega não encontrada.")
            return false
        }
        val doMotorista = entregaRepository.observarTodos().first().filter { it.motoristaId == entrega.motoristaId }
        val problema = RegrasNegocio.validarSimulacaoEntrega(entrega, doMotorista)
        if (problema != null) {
            _estado.update { it.copy(erro = problema) }
            return false
        }
        if (entrega.status == StatusEntrega.PENDENTE) entregaRepository.iniciar(entrega)
        return true
    }

    private fun executar() {
        val rota = percurso ?: return
        execucao?.cancel()
        execucao = viewModelScope.launch {
            var ultimoRegistro = 0L
            while (isActive) {
                delay(TICK_MS)
                val atual = _estado.value
                if (atual.fase != FaseSimulacao.EM_ANDAMENTO) continue

                progressoAtual = rota.avancar(progressoAtual, TICK_MS / 1000.0 * atual.fatorVelocidade)
                if (progressoAtual.chegou) {
                    // Na chegada: parado, motor desligado e portas abertas para a descarga.
                    publicar(progressoAtual, motorLigado = false, portas = EstadoPortas.ABERTA)
                    registrarTelemetria(progressoAtual, motorLigado = false, portas = EstadoPortas.ABERTA)
                    _estado.update { it.copy(fase = FaseSimulacao.CHEGOU) }
                    break
                }
                publicar(progressoAtual, motorLigado = true, portas = EstadoPortas.FECHADA)
                val agora = System.currentTimeMillis()
                if (agora - ultimoRegistro >= INTERVALO_TELEMETRIA_MS) {
                    registrarTelemetria(progressoAtual, motorLigado = true, portas = EstadoPortas.FECHADA)
                    ultimoRegistro = agora
                }
            }
        }
    }

    /** Pausar é parar o veículo: a telemetria passa a mostrar "parado" para o controlador. */
    fun pausar() {
        if (_estado.value.fase != FaseSimulacao.EM_ANDAMENTO) return
        progressoAtual = progressoAtual.copy(velocidadeKmh = 0.0)
        _estado.update { it.copy(fase = FaseSimulacao.PAUSADA) }
        publicar(progressoAtual, motorLigado = true, portas = EstadoPortas.FECHADA)
        viewModelScope.launch { registrarTelemetria(progressoAtual, motorLigado = true, portas = EstadoPortas.FECHADA) }
    }

    fun definirFatorVelocidade(fator: Int) {
        _estado.update { it.copy(fatorVelocidade = fator) }
    }

    fun concluirEntrega() {
        val id = entregaId ?: return
        if (_estado.value.fase != FaseSimulacao.CHEGOU) return
        viewModelScope.launch {
            val entrega = entregaRepository.buscarPorId(id) ?: return@launch
            val problema = RegrasNegocio.validarConclusao(entrega)
            if (problema != null) {
                _estado.update { it.copy(erro = problema) }
                return@launch
            }
            entregaRepository.concluir(entrega)
            _estado.update { it.copy(entregaConcluida = true, erro = null) }
        }
    }

    private fun publicar(progresso: EstadoPercurso, motorLigado: Boolean, portas: EstadoPortas) {
        val rota = percurso ?: return
        val d = progresso.distanciaPercorrida
        val (percorrida, restante) = rota.dividirEm(d)
        _estado.update {
            it.copy(
                posicao = rota.posicaoEm(d),
                rumoGraus = rota.rumoEm(d).toFloat(),
                velocidadeKmh = progresso.velocidadeKmh.roundToInt(),
                distanciaRestanteMetros = rota.distanciaTotal - d,
                tempoRestanteSegundos = rota.tempoRestanteSegundos(d),
                progresso = rota.progresso(d),
                instrucaoAtual = rota.instrucaoAtual(d),
                proximaManobra = rota.proximaManobra(d),
                percorrida = percorrida,
                restante = restante,
                motorLigado = motorLigado,
                portas = portas
            )
        }
    }

    private suspend fun registrarTelemetria(progresso: EstadoPercurso, motorLigado: Boolean, portas: EstadoPortas) {
        val id = veiculoId ?: return
        val rota = percurso ?: return
        val posicao = rota.posicaoEm(progresso.distanciaPercorrida)
        telemetriaRepository.registrar(
            RegistroTelemetriaEntity(
                veiculoId = id,
                latitude = posicao.latitude,
                longitude = posicao.longitude,
                velocidade = (progresso.velocidadeKmh * 10).roundToInt() / 10.0,
                estadoPortas = portas,
                motorLigado = motorLigado
            )
        )
    }

    override fun onCleared() {
        // Saindo da tela no meio do percurso o veículo fica parado onde estava. Sem este registro
        // final, o controlador continuaria vendo "em trânsito" com a última velocidade gravada.
        val fase = _estado.value.fase
        if (fase == FaseSimulacao.EM_ANDAMENTO || fase == FaseSimulacao.PAUSADA) {
            val parado = progressoAtual.copy(velocidadeKmh = 0.0)
            escopoApp.launch { registrarTelemetria(parado, motorLigado = false, portas = EstadoPortas.FECHADA) }
        }
        super.onCleared()
    }

    private companion object {
        const val TICK_MS = 500L
        const val INTERVALO_TELEMETRIA_MS = 2_000L
        const val DURACAO_AVISO_MS = 6_000L
    }
}

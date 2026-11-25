package com.example.findus.ui.motorista.simulacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.data.enum.rotulo
import com.example.findus.location.Coordenada
import com.example.findus.ui.common.MensagemErro
import com.example.findus.ui.map.CirculoMapa
import com.example.findus.ui.map.LinhaMapa
import com.example.findus.ui.map.MapaOsm
import com.example.findus.ui.map.MarcadorMapa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val COR_ORIGEM = "#1565C0"
private const val COR_DESTINO = "#EF6C00"
private const val COR_VEICULO = "#2E7D32"
private const val COR_PERCORRIDA = "#9E9E9E"
private const val COR_RESTANTE = "#1976D2"
private val FATORES_VELOCIDADE = listOf(1, 2, 5, 10)
private val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")

/**
 * Simulação do percurso na visão do motorista: o mapa acompanha o veículo com a direção
 * de deslocamento para cima, como um app de navegação, mostrando a próxima manobra,
 * velocidade, distância e tempo restantes. Veja [SimulacaoPercursoViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulacaoPercursoScreen(entregaId: String?, destinoLivre: Coordenada?, onVoltar: () -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: SimulacaoPercursoViewModel = viewModel(factory = viewModelFactory {
        initializer {
            val c = app.container
            SimulacaoPercursoViewModel(
                entregaId = entregaId,
                destinoLivre = destinoLivre,
                entregaRepository = c.entregaRepository,
                centroRepository = c.centroRepository,
                veiculoRepository = c.veiculoRepository,
                motoristaRepository = c.motoristaRepository,
                telemetriaRepository = c.telemetriaRepository,
                rotaService = c.rotaService,
                locationHelper = c.locationHelper,
                telemetriaSimuladorManager = c.telemetriaSimuladorManager,
                geofenceNotificador = c.geofenceNotificador,
                escopoApp = c.escopoApp
            )
        }
    })
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // Como em um app de navegação, a tela não apaga enquanto o percurso está aberto.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (estado.fase) {
                FaseSimulacao.CARREGANDO -> Mensagem(carregando = true, texto = "Calculando a rota…")
                FaseSimulacao.ERRO -> Mensagem(carregando = false, texto = estado.erro ?: "Não foi possível abrir a simulação.", onVoltar = onVoltar)
                else -> ConteudoSimulacao(
                    estado = estado,
                    onIniciar = viewModel::iniciarOuContinuar,
                    onPausar = viewModel::pausar,
                    onFator = viewModel::definirFatorVelocidade,
                    onConcluir = viewModel::concluirEntrega,
                    onVoltar = onVoltar
                )
            }
        }
    }
}

@Composable
private fun BoxScope.Mensagem(carregando: Boolean, texto: String, onVoltar: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (carregando) CircularProgressIndicator()
        Text(texto, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        onVoltar?.let { Button(onClick = it) { Text("Voltar") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.ConteudoSimulacao(
    estado: SimulacaoUiState,
    onIniciar: () -> Unit,
    onPausar: () -> Unit,
    onFator: (Int) -> Unit,
    onConcluir: () -> Unit,
    onVoltar: () -> Unit
) {
    var seguindo by rememberSaveable { mutableStateOf(true) }
    var chaveVisaoGeral by rememberSaveable { mutableStateOf(0) }

    val emMovimento = estado.fase == FaseSimulacao.EM_ANDAMENTO || estado.fase == FaseSimulacao.PAUSADA
    // Antes de iniciar e depois de chegar, o mapa mostra a rota inteira com o norte para cima.
    val cameraSegue = seguindo && emMovimento
    val posicao = estado.posicao

    val marcadores = buildList {
        estado.origem?.let { add(MarcadorMapa(posicao = it, titulo = "Saída: ${estado.origemDescricao}", corHex = COR_ORIGEM)) }
        estado.destino?.let { add(MarcadorMapa(posicao = it, titulo = estado.destinoDescricao, corHex = COR_DESTINO)) }
        posicao?.let {
            add(
                MarcadorMapa(
                    posicao = it,
                    titulo = estado.placa ?: "Veículo",
                    corHex = COR_VEICULO,
                    // Seguindo, o mapa gira e a frente do veículo fica sempre para cima.
                    setaGrausNaTela = if (cameraSegue) 0f else estado.rumoGraus
                )
            )
        }
    }
    val circulos = listOfNotNull(
        estado.perimetroOrigem?.let { CirculoMapa(it.centro, it.raioMetros, COR_ORIGEM) },
        estado.perimetroDestino?.let { CirculoMapa(it.centro, it.raioMetros, COR_DESTINO) }
    )

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        MapaOsm(
            marcadores = marcadores,
            modifier = Modifier.fillMaxSize(),
            circulos = circulos,
            linhasEstilizadas = listOf(
                LinhaMapa(estado.percorrida, COR_PERCORRIDA, 10f),
                LinhaMapa(estado.restante, COR_RESTANTE, 12f)
            ),
            seguir = if (cameraSegue) posicao else null,
            zoomAoSeguir = 17.0,
            orientacaoMapa = if (cameraSegue) -estado.rumoGraus else 0f,
            enquadrar = estado.rota,
            chaveEnquadramento = if (cameraSegue) null else chaveVisaoGeral
        )
    }

    Column(
        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PainelInstrucao(estado)
        estado.avisoGeofence?.let { aviso ->
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(aviso, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }
        }
        if (!estado.rotaPelaApi) {
            Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Rota aproximada em linha reta: a Geoapify não respondeu (sem chave ou sem internet).",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }

    Surface(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Indicador(valor = "${estado.velocidadeKmh} km/h", rotulo = "Velocidade")
                Indicador(valor = formatarDistancia(estado.distanciaRestanteMetros), rotulo = "Restante")
                Indicador(
                    valor = formatarTempo(estado.tempoRestanteSegundos),
                    rotulo = if (estado.fase == FaseSimulacao.CHEGOU) "Chegou" else "Chegada ${horaPrevista(estado.tempoRestanteSegundos)}"
                )
            }
            LinearProgressIndicator(progress = { estado.progresso }, modifier = Modifier.fillMaxWidth())
            Text(
                listOf(
                    estado.placa ?: "Sem veículo vinculado",
                    "Motor ${if (estado.motorLigado) "ligado" else "desligado"}",
                    "Portas ${estado.portas.rotulo()}"
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall
            )
            if (!estado.geraTelemetria) {
                Text(
                    "Sem veículo vinculado: o percurso não gera telemetria para o controlador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MensagemErro(estado.erro)

            if (estado.fase != FaseSimulacao.CHEGOU) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ritmo", style = MaterialTheme.typography.labelMedium)
                    FATORES_VELOCIDADE.forEach { fator ->
                        FilterChip(
                            selected = estado.fatorVelocidade == fator,
                            onClick = { onFator(fator) },
                            label = { Text("${fator}x") }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (estado.fase) {
                    FaseSimulacao.PRONTA -> Button(onClick = onIniciar, modifier = Modifier.weight(1f)) {
                        Text(if (estado.ehEntrega) "Iniciar percurso" else "Iniciar simulação")
                    }
                    FaseSimulacao.EM_ANDAMENTO -> Button(onClick = onPausar, modifier = Modifier.weight(1f)) { Text("Pausar") }
                    FaseSimulacao.PAUSADA -> Button(onClick = onIniciar, modifier = Modifier.weight(1f)) { Text("Continuar") }
                    FaseSimulacao.CHEGOU ->
                        if (estado.ehEntrega && !estado.entregaConcluida) {
                            Button(onClick = onConcluir, modifier = Modifier.weight(1f)) { Text("Concluir entrega") }
                        } else {
                            Button(onClick = onVoltar, modifier = Modifier.weight(1f)) { Text("Voltar") }
                        }
                    else -> Unit
                }
                if (emMovimento) {
                    OutlinedButton(onClick = {
                        if (seguindo) chaveVisaoGeral++
                        seguindo = !seguindo
                    }) { Text(if (seguindo) "Visão geral" else "Seguir veículo") }
                }
            }
            if (estado.entregaConcluida) {
                Text(
                    "Entrega concluída. O veículo voltou a ficar disponível.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PainelInstrucao(estado: SimulacaoUiState) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val manobra = estado.proximaManobra
            when {
                estado.fase == FaseSimulacao.CHEGOU -> {
                    Text("Você chegou ao destino", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(estado.destinoDescricao, style = MaterialTheme.typography.bodyMedium)
                }
                manobra != null -> {
                    Text("Em ${formatarDistancia(manobra.distanciaMetros)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(manobra.instrucao, style = MaterialTheme.typography.titleMedium)
                    estado.instrucaoAtual?.let { Text("Agora: $it", style = MaterialTheme.typography.bodySmall) }
                }
                else -> Text(estado.instrucaoAtual ?: "Siga a rota", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun Indicador(valor: String, rotulo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatarDistancia(metros: Double): String = when {
    metros < 100 -> "${metros.roundToInt()} m"
    metros < 1000 -> "${(metros / 10).roundToInt() * 10} m"
    else -> String.format(LOCALE_BR, "%.1f km", metros / 1000)
}

private fun formatarTempo(segundos: Double): String {
    val minutos = (segundos / 60).roundToInt()
    return when {
        segundos <= 0.0 -> "0 min"
        minutos < 1 -> "< 1 min"
        minutos < 60 -> "$minutos min"
        else -> "${minutos / 60} h ${minutos % 60} min"
    }
}

private fun horaPrevista(segundosRestantes: Double): String =
    SimpleDateFormat("HH:mm", LOCALE_BR).format(Date(System.currentTimeMillis() + (segundosRestantes * 1000).toLong()))

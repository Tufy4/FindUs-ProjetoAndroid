package com.example.findus.ui.controlador.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.data.enum.EstadoPortas
import com.example.findus.data.enum.rotulo
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.location.Coordenada
import com.example.findus.location.OrigemPerimetro
import com.example.findus.ui.common.FotoVeiculo
import com.example.findus.ui.map.CirculoMapa
import com.example.findus.ui.map.MapaOsm
import com.example.findus.ui.map.MarcadorMapa
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaControladorScreen(onVoltar: () -> Unit, onVerRota: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: ControladorViewModel = viewModel(factory = viewModelFactory {
        initializer {
            ControladorViewModel(
                veiculoRepository = app.container.veiculoRepository,
                telemetriaRepository = app.container.telemetriaRepository,
                entregaRepository = app.container.entregaRepository,
                telemetriaSimuladorManager = app.container.telemetriaSimuladorManager,
                geofenceNotificador = app.container.geofenceNotificador,
                reverseGeocoder = app.container.reverseGeocoder,
                perimetros = app.container.perimetrosMonitorados
            )
        }
    })

    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    val telemetrias by viewModel.ultimasTelemetrias.collectAsStateWithLifecycle()
    val enderecoSelecionado by viewModel.enderecoSelecionado.collectAsStateWithLifecycle()
    val trajetos by viewModel.trajetos.collectAsStateWithLifecycle()
    val entregasAtivas by viewModel.entregasAtivas.collectAsStateWithLifecycle()
    val perimetros by viewModel.perimetros.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    var veiculoSelecionado by remember { mutableStateOf<VeiculoEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventosGeofence.collect { evento ->
            escopo.launch { snackbarHostState.showSnackbar(evento.mensagem()) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel do Controlador") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Centros de distribuição (azul) e pontos de entrega ativos (laranja) por baixo dos veículos.
            val marcadoresLocais = perimetros
                .filter { it.origem != OrigemPerimetro.PERSONALIZADO }
                .map { perimetro ->
                    MarcadorMapa(
                        posicao = perimetro.centro,
                        titulo = perimetro.nome,
                        corHex = if (perimetro.origem == OrigemPerimetro.CENTRO_DISTRIBUICAO) COR_CENTRO else COR_ENTREGA
                    )
                }
            val circulos = perimetros.map { perimetro ->
                CirculoMapa(
                    centro = perimetro.centro,
                    raioMetros = perimetro.raioMetros,
                    corHex = when (perimetro.origem) {
                        OrigemPerimetro.CENTRO_DISTRIBUICAO -> COR_CENTRO
                        OrigemPerimetro.PONTO_ENTREGA -> COR_ENTREGA
                        OrigemPerimetro.PERSONALIZADO -> COR_PERIMETRO
                    }
                )
            }
            val marcadoresVeiculos = telemetrias.map { registro ->
                val veiculo = veiculos.find { it.id == registro.veiculoId }
                val emTransito = registro.velocidade > 0.0
                MarcadorMapa(
                    posicao = Coordenada(registro.latitude, registro.longitude),
                    titulo = "${veiculo?.placa ?: "Veículo ${registro.veiculoId}"} · ${if (emTransito) "em trânsito" else "parado"}",
                    descricao = "${registro.velocidade} km/h · ${registro.estadoPortas}",
                    corHex = if (emTransito) COR_EM_TRANSITO else COR_PARADO,
                    onClick = {
                        veiculoSelecionado = veiculo
                        viewModel.consultarEndereco(registro.latitude, registro.longitude)
                    }
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                MapaOsm(
                    marcadores = marcadoresLocais + marcadoresVeiculos,
                    modifier = Modifier.fillMaxSize(),
                    linhas = trajetos.values.toList(),
                    // Centraliza primeiro na região de operação (centros e entregas), não em
                    // posições antigas de veículos, que no emulador podem estar nos EUA.
                    centro = marcadoresLocais.firstOrNull()?.posicao ?: marcadoresVeiculos.firstOrNull()?.posicao,
                    zoom = 12.0,
                    circulos = circulos
                )
            }
            LegendaMapa()

            Divider()

            Text(
                "Frota e telemetria",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(12.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(veiculos, key = { it.id }) { veiculo ->
                    val telemetria = telemetrias.find { it.veiculoId == veiculo.id }
                    LinhaTelemetriaVeiculo(
                        veiculo = veiculo,
                        telemetria = telemetria,
                        entrega = entregasAtivas.find { it.veiculoId == veiculo.id },
                        simulacaoAtiva = trajetos.containsKey(veiculo.id),
                        onIniciarSimulacao = { viewModel.iniciarSimulacao(veiculo.id) },
                        onPararSimulacao = { viewModel.pararSimulacao(veiculo.id) },
                        onVerRota = { onVerRota(veiculo.id) }
                    )
                }
            }
        }
    }

    veiculoSelecionado?.let { veiculo ->
        AlertDialog(
            onDismissRequest = { veiculoSelecionado = null },
            title = { Text(veiculo.placa) },
            text = { Text(enderecoSelecionado ?: "Buscando endereço…") },
            confirmButton = {
                TextButton(onClick = { veiculoSelecionado = null }) { Text("Fechar") }
            }
        )
    }
}

@Composable
private fun LinhaTelemetriaVeiculo(
    veiculo: VeiculoEntity,
    telemetria: RegistroTelemetriaEntity?,
    entrega: EntregaEntity?,
    simulacaoAtiva: Boolean,
    onIniciarSimulacao: () -> Unit,
    onPararSimulacao: () -> Unit,
    onVerRota: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FotoVeiculo(veiculo.fotoBase64, Modifier.size(56.dp))
                Text("${veiculo.placa} · ${veiculo.modelo}", style = MaterialTheme.typography.titleMedium)
            }
            if (telemetria != null) {
                val emTransito = telemetria.velocidade > 0.0
                Text(
                    if (emTransito) "Em trânsito" else "Parado",
                    style = MaterialTheme.typography.titleSmall,
                    color = corCompose(if (emTransito) COR_EM_TRANSITO else COR_PARADO)
                )
                Text(
                    "Velocidade: ${telemetria.velocidade} km/h · Portas: ${if (telemetria.estadoPortas == EstadoPortas.ABERTA) "abertas" else "fechadas"} · Motor: ${if (telemetria.motorLigado) "ligado" else "desligado"}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("Sem telemetria recente", style = MaterialTheme.typography.bodySmall)
            }
            Text("Status: ${veiculo.status}", style = MaterialTheme.typography.bodySmall)
            Text(
                if (entrega != null) "Entrega: ${entrega.descricao} (${entrega.status.rotulo()})" else "Sem entrega atribuída",
                style = MaterialTheme.typography.bodySmall
            )

            Column {
                Button(onClick = if (simulacaoAtiva) onPararSimulacao else onIniciarSimulacao) {
                    Text(if (simulacaoAtiva) "Parar simulação" else "Iniciar simulação")
                }
                TextButton(onClick = onVerRota) { Text("Ver rota até este veículo") }
            }
        }
    }
}

private const val COR_EM_TRANSITO = "#2E7D32"
private const val COR_PARADO = "#C62828"
private const val COR_CENTRO = "#1565C0"
private const val COR_ENTREGA = "#EF6C00"
private const val COR_PERIMETRO = "#6A1B9A"

private fun corCompose(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

@Composable
private fun LegendaMapa() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemLegenda(COR_EM_TRANSITO, "Em trânsito")
        ItemLegenda(COR_PARADO, "Parado")
        ItemLegenda(COR_CENTRO, "Centro")
        ItemLegenda(COR_ENTREGA, "Entrega")
        ItemLegenda(COR_PERIMETRO, "Perímetro")
    }
}

@Composable
private fun ItemLegenda(corHex: String, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).background(corCompose(corHex), CircleShape))
        Text(texto, style = MaterialTheme.typography.labelSmall)
    }
}

package com.example.findus.ui.controlador.mapa

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.data.enum.EstadoPortas
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.location.Coordenada
import com.example.findus.ui.common.FotoVeiculo
import com.example.findus.location.TipoEventoGeofence
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
                telemetriaSimuladorManager = app.container.telemetriaSimuladorManager,
                geofenceNotificador = app.container.geofenceNotificador,
                reverseGeocoder = app.container.reverseGeocoder
            )
        }
    })

    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    val telemetrias by viewModel.ultimasTelemetrias.collectAsStateWithLifecycle()
    val enderecoSelecionado by viewModel.enderecoSelecionado.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    var veiculoSelecionado by remember { mutableStateOf<VeiculoEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventosGeofence.collect { evento ->
            val veiculo = veiculos.find { it.id == evento.veiculoId }
            val acao = if (evento.tipo == TipoEventoGeofence.ENTROU) "entrou no" else "saiu do"
            escopo.launch {
                snackbarHostState.showSnackbar("Veículo ${veiculo?.placa ?: evento.veiculoId} $acao perímetro monitorado")
            }
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
            val marcadores = telemetrias.map { registro ->
                val veiculo = veiculos.find { it.id == registro.veiculoId }
                MarcadorMapa(
                    posicao = Coordenada(registro.latitude, registro.longitude),
                    titulo = veiculo?.placa ?: "Veículo ${registro.veiculoId}",
                    descricao = "${registro.velocidade} km/h · ${registro.estadoPortas}",
                    corHex = if (registro.velocidade > 0.0) "#2E7D32" else "#C62828",
                    onClick = {
                        veiculoSelecionado = veiculo
                        viewModel.consultarEndereco(registro.latitude, registro.longitude)
                    }
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                MapaOsm(
                    marcadores = marcadores,
                    modifier = Modifier.fillMaxSize(),
                    centro = marcadores.firstOrNull()?.posicao,
                    zoom = 12.0
                )
            }

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
                        simulacaoAtiva = viewModel.simulacaoAtiva(veiculo.id),
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
                Text(
                    "Velocidade: ${telemetria.velocidade} km/h · Portas: ${if (telemetria.estadoPortas == EstadoPortas.ABERTA) "abertas" else "fechadas"} · Motor: ${if (telemetria.motorLigado) "ligado" else "desligado"}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("Sem telemetria recente", style = MaterialTheme.typography.bodySmall)
            }
            Text("Status: ${veiculo.status}", style = MaterialTheme.typography.bodySmall)

            Column {
                Button(onClick = if (simulacaoAtiva) onPararSimulacao else onIniciarSimulacao) {
                    Text(if (simulacaoAtiva) "Parar simulação" else "Iniciar simulação")
                }
                TextButton(onClick = onVerRota) { Text("Ver rota até este veículo") }
            }
        }
    }
}

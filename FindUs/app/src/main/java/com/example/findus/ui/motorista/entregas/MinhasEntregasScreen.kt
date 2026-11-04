package com.example.findus.ui.motorista.entregas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.data.enum.StatusEntrega
import com.example.findus.data.enum.rotulo
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.location.Coordenada
import com.example.findus.ui.common.MensagemErro
import com.example.findus.ui.common.descreverLocal
import com.example.findus.ui.common.formatarDataHora

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinhasEntregasScreen(onVoltar: () -> Unit, onVerRota: (Coordenada) -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: MinhasEntregasViewModel = viewModel(factory = viewModelFactory {
        initializer {
            MinhasEntregasViewModel(
                app.container.entregaRepository,
                app.container.motoristaRepository,
                app.container.centroRepository,
                app.container.veiculoRepository
            )
        }
    })
    val motorista by viewModel.motoristaAtual.collectAsStateWithLifecycle()
    val entregas by viewModel.entregas.collectAsStateWithLifecycle()
    val centros by viewModel.centros.collectAsStateWithLifecycle()
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    val erro by viewModel.erro.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas entregas") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val eu = motorista
            if (eu == null) {
                Text(
                    "Sua conta ainda não está vinculada a um cadastro de motorista. " +
                        "Peça ao controlador para cadastrar você em \"Cadastro de motoristas\".",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                ConteudoMinhasEntregas(
                    nomeMotorista = eu.nome,
                    veiculoDescricao = veiculos.find { it.id == eu.veiculoId }?.let { "${it.placa} · ${it.modelo}" },
                    erro = erro,
                    entregas = entregas,
                    nomeDoCentro = { id -> centros.find { it.id == id }?.nome },
                    onIniciar = { viewModel.iniciar(it) },
                    onConcluir = { viewModel.concluir(it) },
                    onVerRota = onVerRota
                )
            }
        }
    }
}

@Composable
private fun ConteudoMinhasEntregas(
    nomeMotorista: String,
    veiculoDescricao: String?,
    erro: String?,
    entregas: List<EntregaEntity>,
    nomeDoCentro: (String) -> String?,
    onIniciar: (EntregaEntity) -> Unit,
    onConcluir: (EntregaEntity) -> Unit,
    onVerRota: (Coordenada) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Olá, $nomeMotorista. Veículo: ${veiculoDescricao ?: "nenhum vinculado"}",
            style = MaterialTheme.typography.bodyMedium
        )
        MensagemErro(erro)

        if (entregas.isEmpty()) {
            Text("Nenhuma entrega atribuída a você.", style = MaterialTheme.typography.bodyMedium)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entregas, key = { it.id }) { entrega ->
                val destino = Coordenada(entrega.latitudeDestino, entrega.longitudeDestino)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${entrega.descricao} · ${entrega.status.rotulo()}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Saída: ${nomeDoCentro(entrega.centroDistribuicaoId) ?: "centro não encontrado"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Destino: ${descreverLocal(entrega.enderecoDestino, entrega.latitudeDestino, entrega.longitudeDestino)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        entrega.concluidaEm?.let {
                            Text("Entregue em ${formatarDataHora(it)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (entrega.status) {
                                StatusEntrega.PENDENTE -> {
                                    Button(onClick = { onIniciar(entrega) }) { Text("Iniciar") }
                                    OutlinedButton(onClick = { onVerRota(destino) }) { Text("Ver rota") }
                                }
                                StatusEntrega.EM_ROTA -> {
                                    Button(onClick = { onConcluir(entrega) }) { Text("Concluir") }
                                    OutlinedButton(onClick = { onVerRota(destino) }) { Text("Ver rota") }
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.findus.ui.controlador.entregas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.data.enum.rotulo
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.domain.RegrasNegocio
import com.example.findus.ui.common.MensagemErro
import com.example.findus.ui.common.SeletorLista
import com.example.findus.ui.common.SeletorLocalMapa
import com.example.findus.ui.common.TelaCadastro
import com.example.findus.ui.common.descreverLocal
import com.example.findus.ui.common.formatarDataHora

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntregasScreen(onVoltar: () -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: EntregasViewModel = viewModel(factory = viewModelFactory {
        initializer {
            EntregasViewModel(
                app.container.entregaRepository,
                app.container.centroRepository,
                app.container.motoristaRepository,
                app.container.veiculoRepository,
                app.container.reverseGeocoder
            )
        }
    })
    val entregas by viewModel.entregas.collectAsStateWithLifecycle()
    val centros by viewModel.centros.collectAsStateWithLifecycle()
    val motoristas by viewModel.motoristas.collectAsStateWithLifecycle()
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    val erro by viewModel.erro.collectAsStateWithLifecycle()
    val coordenada by viewModel.local.coordenada.collectAsStateWithLifecycle()
    val endereco by viewModel.local.endereco.collectAsStateWithLifecycle()

    var formularioAberto by remember { mutableStateOf(false) }
    var emEdicao by remember { mutableStateOf<EntregaEntity?>(null) }
    var descricao by remember { mutableStateOf("") }
    var centro by remember { mutableStateOf<CentroDistribuicaoEntity?>(null) }
    var motorista by remember { mutableStateOf<MotoristaEntity?>(null) }
    var entregaParaCancelar by remember { mutableStateOf<EntregaEntity?>(null) }

    fun abrir(entrega: EntregaEntity?) {
        emEdicao = entrega
        descricao = entrega?.descricao.orEmpty()
        centro = entrega?.let { e -> centros.find { it.id == e.centroDistribuicaoId } }
        motorista = entrega?.let { e -> motoristas.find { it.id == e.motoristaId } }
        viewModel.abrir(entrega)
        formularioAberto = true
    }

    fun fechar() {
        formularioAberto = false
        emEdicao = null
    }

    fun placaDoMotorista(m: MotoristaEntity): String? = veiculos.find { it.id == m.veiculoId }?.placa

    TelaCadastro(
        titulo = "Entregas",
        formularioAberto = formularioAberto,
        textoBotaoNovo = "Nova entrega",
        onVoltar = onVoltar,
        onNovo = { abrir(null) },
        onFecharFormulario = { fechar() },
        formulario = {
            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição (ex.: pedido, cliente)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SeletorLista(
                label = "Centro de distribuição de origem",
                opcoes = centros,
                selecionado = centro,
                textoDe = { it.nome },
                onSelecionar = { centro = it },
                modifier = Modifier.fillMaxWidth()
            )
            SeletorLista(
                label = "Motorista",
                opcoes = motoristas,
                selecionado = motorista,
                textoDe = { m -> "${m.nome} · ${placaDoMotorista(m) ?: "sem veículo"}" },
                onSelecionar = { motorista = it },
                modifier = Modifier.fillMaxWidth()
            )
            val veiculoDoMotorista = motorista?.let { m -> veiculos.find { it.id == m.veiculoId } }
            Text(
                when {
                    motorista == null -> "O veículo da entrega é o veículo vinculado ao motorista."
                    veiculoDoMotorista == null -> "Este motorista não tem veículo vinculado."
                    else -> "Veículo: ${veiculoDoMotorista.placa} · ${veiculoDoMotorista.modelo} (${veiculoDoMotorista.status})"
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text("Ponto de entrega", style = MaterialTheme.typography.titleSmall)
            SeletorLocalMapa(
                coordenada = coordenada,
                endereco = endereco,
                onSelecionar = { viewModel.local.selecionar(it) },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            MensagemErro(erro)
            Button(
                onClick = { viewModel.salvar(emEdicao, descricao, centro, motorista, onSucesso = { fechar() }) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (emEdicao == null) "Atribuir entrega" else "Salvar alterações") }
        },
        lista = {
            if (entregas.isEmpty()) {
                item {
                    Text(
                        "Nenhuma entrega cadastrada. Cadastre antes um centro de distribuição e um motorista com veículo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(entregas, key = { it.id }) { entrega ->
                val nomeMotorista = motoristas.find { it.id == entrega.motoristaId }?.nome ?: "motorista removido"
                val placa = veiculos.find { it.id == entrega.veiculoId }?.placa ?: "veículo removido"
                val nomeCentro = centros.find { it.id == entrega.centroDistribuicaoId }?.nome ?: "centro removido"
                Card(
                    onClick = { if (RegrasNegocio.podeEditarEntrega(entrega)) abrir(entrega) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${entrega.descricao} · ${entrega.status.rotulo()}", style = MaterialTheme.typography.titleMedium)
                        Text("$nomeMotorista · $placa", style = MaterialTheme.typography.bodySmall)
                        Text("Saída: $nomeCentro", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Destino: ${descreverLocal(entrega.enderecoDestino, entrega.latitudeDestino, entrega.longitudeDestino)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            listOfNotNull(
                                "Criada ${formatarDataHora(entrega.criadaEm)}",
                                entrega.iniciadaEm?.let { "saiu ${formatarDataHora(it)}" },
                                entrega.concluidaEm?.let { "encerrada ${formatarDataHora(it)}" }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (RegrasNegocio.podeCancelarEntrega(entrega)) {
                            TextButton(onClick = { entregaParaCancelar = entrega }) { Text("Cancelar entrega") }
                        }
                    }
                }
            }
        }
    )

    entregaParaCancelar?.let { entrega ->
        AlertDialog(
            onDismissRequest = { entregaParaCancelar = null },
            title = { Text("Cancelar entrega?") },
            text = { Text("A entrega \"${entrega.descricao}\" será cancelada e o motorista deixará de vê-la.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelar(entrega)
                    entregaParaCancelar = null
                }) { Text("Cancelar entrega") }
            },
            dismissButton = {
                TextButton(onClick = { entregaParaCancelar = null }) { Text("Voltar") }
            }
        )
    }
}

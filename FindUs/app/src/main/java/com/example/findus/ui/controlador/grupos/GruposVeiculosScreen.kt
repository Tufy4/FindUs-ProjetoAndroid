package com.example.findus.ui.controlador.grupos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.findus.data.local.entity.GrupoVeiculoEntity
import com.example.findus.ui.common.MensagemErro
import com.example.findus.ui.common.TelaCadastro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruposVeiculosScreen(onVoltar: () -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: GruposVeiculosViewModel = viewModel(factory = viewModelFactory {
        initializer {
            GruposVeiculosViewModel(
                app.container.grupoRepository,
                app.container.veiculoRepository,
                app.container.perimetroRepository
            )
        }
    })
    val grupos by viewModel.grupos.collectAsStateWithLifecycle()
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    val erro by viewModel.erro.collectAsStateWithLifecycle()

    var formularioAberto by remember { mutableStateOf(false) }
    var emEdicao by remember { mutableStateOf<GrupoVeiculoEntity?>(null) }
    var nome by remember { mutableStateOf("") }
    var selecionados by remember { mutableStateOf(setOf<String>()) }

    fun abrir(grupo: GrupoVeiculoEntity?) {
        emEdicao = grupo
        nome = grupo?.nome.orEmpty()
        selecionados = grupo?.veiculoIds?.toSet() ?: emptySet()
        viewModel.limparErro()
        formularioAberto = true
    }

    fun fechar() {
        formularioAberto = false
        emEdicao = null
    }

    TelaCadastro(
        titulo = "Grupos de veículos",
        formularioAberto = formularioAberto,
        textoBotaoNovo = "Novo grupo",
        onVoltar = onVoltar,
        onNovo = { abrir(null) },
        onFecharFormulario = { fechar() },
        formulario = {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome do grupo (ex.: Zona Norte, Refrigerados)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Veículos do grupo", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (veiculos.isEmpty()) {
                    item { Text("Nenhum veículo cadastrado na frota.", style = MaterialTheme.typography.bodyMedium) }
                }
                items(veiculos, key = { it.id }) { veiculo ->
                    val marcado = veiculo.id in selecionados
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = marcado,
                            onCheckedChange = { selecionados = if (it) selecionados + veiculo.id else selecionados - veiculo.id }
                        )
                        Text("${veiculo.placa} · ${veiculo.modelo} (${veiculo.tipo})")
                    }
                }
            }
            MensagemErro(erro)
            Button(
                onClick = { viewModel.salvar(emEdicao, nome, selecionados, onSucesso = { fechar() }) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (emEdicao == null) "Cadastrar grupo" else "Salvar alterações") }
            emEdicao?.let { grupo ->
                OutlinedButton(
                    onClick = { viewModel.remover(grupo, onSucesso = { fechar() }) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remover grupo") }
            }
        },
        lista = {
            if (grupos.isEmpty()) {
                item {
                    Text(
                        "Nenhum grupo cadastrado. Use grupos para monitorar vários veículos com um mesmo perímetro.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(grupos, key = { it.id }) { grupo ->
                val placas = grupo.veiculoIds.mapNotNull { id -> veiculos.find { it.id == id }?.placa }
                Card(
                    onClick = { abrir(grupo) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(grupo.nome, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${placas.size} veículo(s): ${placas.joinToString(", ").ifBlank { "nenhum" }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}

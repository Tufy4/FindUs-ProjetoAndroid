package com.example.findus.ui.controlador.centros

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import com.example.findus.ui.common.MensagemErro
import com.example.findus.ui.common.SeletorLocalMapa
import com.example.findus.ui.common.TelaCadastro
import com.example.findus.ui.common.descreverLocal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentrosDistribuicaoScreen(onVoltar: () -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: CentrosDistribuicaoViewModel = viewModel(factory = viewModelFactory {
        initializer {
            CentrosDistribuicaoViewModel(
                app.container.centroRepository,
                app.container.entregaRepository,
                app.container.reverseGeocoder
            )
        }
    })
    val centros by viewModel.centros.collectAsStateWithLifecycle()
    val erro by viewModel.erro.collectAsStateWithLifecycle()
    val coordenada by viewModel.local.coordenada.collectAsStateWithLifecycle()
    val endereco by viewModel.local.endereco.collectAsStateWithLifecycle()

    var formularioAberto by remember { mutableStateOf(false) }
    var emEdicao by remember { mutableStateOf<CentroDistribuicaoEntity?>(null) }
    var nome by remember { mutableStateOf("") }
    var raio by remember { mutableStateOf("500") }

    fun abrir(centro: CentroDistribuicaoEntity?) {
        emEdicao = centro
        nome = centro?.nome.orEmpty()
        raio = (centro?.raioMetros ?: 500.0).toInt().toString()
        viewModel.abrir(centro)
        formularioAberto = true
    }

    fun fechar() {
        formularioAberto = false
        emEdicao = null
    }

    TelaCadastro(
        titulo = "Centros de distribuição",
        formularioAberto = formularioAberto,
        textoBotaoNovo = "Novo centro de distribuição",
        onVoltar = onVoltar,
        onNovo = { abrir(null) },
        onFecharFormulario = { fechar() },
        formulario = {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = raio,
                onValueChange = { raio = it },
                label = { Text("Raio do perímetro (metros)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            SeletorLocalMapa(
                coordenada = coordenada,
                endereco = endereco,
                onSelecionar = { viewModel.local.selecionar(it) },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            MensagemErro(erro)
            Button(
                onClick = { viewModel.salvar(emEdicao, nome, raio, onSucesso = { fechar() }) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (emEdicao == null) "Cadastrar centro" else "Salvar alterações") }
            emEdicao?.let { centro ->
                OutlinedButton(
                    onClick = { viewModel.remover(centro, onSucesso = { fechar() }) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remover centro") }
            }
        },
        lista = {
            if (centros.isEmpty()) {
                item {
                    Text(
                        "Nenhum centro cadastrado. Os centros são a origem das entregas e também perímetros do geofencing.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(centros, key = { it.id }) { centro ->
                Card(
                    onClick = { abrir(centro) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(centro.nome, style = MaterialTheme.typography.titleMedium)
                        Text(
                            descreverLocal(centro.endereco, centro.latitude, centro.longitude),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Perímetro de ${centro.raioMetros.toInt()} m", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    )
}

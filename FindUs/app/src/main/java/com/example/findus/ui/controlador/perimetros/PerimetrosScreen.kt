package com.example.findus.ui.controlador.perimetros

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.data.enum.TipoAlvoPerimetro
import com.example.findus.data.enum.rotulo
import com.example.findus.data.local.entity.PerimetroEntity
import com.example.findus.ui.common.MensagemErro
import com.example.findus.ui.common.SeletorLista
import com.example.findus.ui.common.SeletorLocalMapa
import com.example.findus.ui.common.TelaCadastro
import com.example.findus.ui.common.descreverLocal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerimetrosScreen(onVoltar: () -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: PerimetrosViewModel = viewModel(factory = viewModelFactory {
        initializer {
            PerimetrosViewModel(
                app.container.perimetroRepository,
                app.container.veiculoRepository,
                app.container.grupoRepository,
                app.container.reverseGeocoder
            )
        }
    })
    val perimetros by viewModel.perimetros.collectAsStateWithLifecycle()
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    val grupos by viewModel.grupos.collectAsStateWithLifecycle()
    val erro by viewModel.erro.collectAsStateWithLifecycle()
    val coordenada by viewModel.local.coordenada.collectAsStateWithLifecycle()
    val endereco by viewModel.local.endereco.collectAsStateWithLifecycle()

    var formularioAberto by remember { mutableStateOf(false) }
    var emEdicao by remember { mutableStateOf<PerimetroEntity?>(null) }
    var nome by remember { mutableStateOf("") }
    var raio by remember { mutableStateOf("500") }
    var tipoAlvo by remember { mutableStateOf(TipoAlvoPerimetro.TODOS) }
    var alvoId by remember { mutableStateOf<String?>(null) }
    var ativo by remember { mutableStateOf(true) }

    fun abrir(perimetro: PerimetroEntity?) {
        emEdicao = perimetro
        nome = perimetro?.nome.orEmpty()
        raio = (perimetro?.raioMetros ?: 500.0).toInt().toString()
        tipoAlvo = perimetro?.tipoAlvo ?: TipoAlvoPerimetro.TODOS
        alvoId = perimetro?.alvoId
        ativo = perimetro?.ativo ?: true
        viewModel.abrir(perimetro)
        formularioAberto = true
    }

    fun fechar() {
        formularioAberto = false
        emEdicao = null
    }

    fun descreverAlvo(perimetro: PerimetroEntity): String = when (perimetro.tipoAlvo) {
        TipoAlvoPerimetro.TODOS -> "Todos os veículos"
        TipoAlvoPerimetro.VEICULO ->
            "Veículo ${veiculos.find { it.id == perimetro.alvoId }?.placa ?: "removido"}"
        TipoAlvoPerimetro.GRUPO ->
            "Grupo ${grupos.find { it.id == perimetro.alvoId }?.nome ?: "removido"}"
    }

    TelaCadastro(
        titulo = "Perímetros (geofencing)",
        formularioAberto = formularioAberto,
        textoBotaoNovo = "Novo perímetro",
        onVoltar = onVoltar,
        onNovo = { abrir(null) },
        onFecharFormulario = { fechar() },
        formulario = {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome (ex.: Cliente Centro, Área restrita)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = raio,
                onValueChange = { raio = it },
                label = { Text("Raio (metros)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            SeletorLista(
                label = "Monitorar",
                opcoes = TipoAlvoPerimetro.entries.toList(),
                selecionado = tipoAlvo,
                textoDe = { it.rotulo() },
                onSelecionar = {
                    if (it != tipoAlvo) alvoId = null
                    tipoAlvo = it
                },
                modifier = Modifier.fillMaxWidth()
            )
            when (tipoAlvo) {
                TipoAlvoPerimetro.VEICULO -> SeletorLista(
                    label = "Veículo",
                    opcoes = veiculos,
                    selecionado = veiculos.find { it.id == alvoId },
                    textoDe = { "${it.placa} · ${it.modelo}" },
                    onSelecionar = { alvoId = it.id },
                    modifier = Modifier.fillMaxWidth()
                )
                TipoAlvoPerimetro.GRUPO -> SeletorLista(
                    label = "Grupo",
                    opcoes = grupos,
                    selecionado = grupos.find { it.id == alvoId },
                    textoDe = { "${it.nome} (${it.veiculoIds.size} veículos)" },
                    onSelecionar = { alvoId = it.id },
                    modifier = Modifier.fillMaxWidth()
                )
                TipoAlvoPerimetro.TODOS -> Unit
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = ativo, onCheckedChange = { ativo = it })
                Text("Perímetro ativo")
            }
            SeletorLocalMapa(
                coordenada = coordenada,
                endereco = endereco,
                onSelecionar = { viewModel.local.selecionar(it) },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            MensagemErro(erro)
            Button(
                onClick = {
                    viewModel.salvar(emEdicao, nome, raio, tipoAlvo, alvoId, ativo, onSucesso = { fechar() })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (emEdicao == null) "Cadastrar perímetro" else "Salvar alterações") }
            emEdicao?.let { perimetro ->
                OutlinedButton(
                    onClick = { viewModel.remover(perimetro, onSucesso = { fechar() }) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remover perímetro") }
            }
        },
        lista = {
            item {
                Text(
                    "Os centros de distribuição e os pontos de entrega ativos já são monitorados automaticamente. " +
                        "Aqui você cadastra perímetros extras, para um veículo específico ou para um grupo.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(perimetros, key = { it.id }) { perimetro ->
                Card(
                    onClick = { abrir(perimetro) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            if (perimetro.ativo) perimetro.nome else "${perimetro.nome} (inativo)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${descreverAlvo(perimetro)} · raio de ${perimetro.raioMetros.toInt()} m",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            descreverLocal(perimetro.endereco, perimetro.latitude, perimetro.longitude),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}

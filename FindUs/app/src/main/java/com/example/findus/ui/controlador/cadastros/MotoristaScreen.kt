package com.example.findus.ui.controlador.cadastros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoristaScreen(onVoltar: () -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: MotoristaViewModel = viewModel(factory = viewModelFactory {
        initializer { MotoristaViewModel(app.container.motoristaRepository, app.container.veiculoRepository) }
    })
    val motoristas by viewModel.motoristas.collectAsStateWithLifecycle()
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()

    var editandoId by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var cnh by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var veiculoSelecionado by remember { mutableStateOf<VeiculoEntity?>(null) }
    var expandido by remember { mutableStateOf(false) }

    fun limparFormulario() {
        editandoId = ""; nome = ""; cnh = ""; telefone = ""; veiculoSelecionado = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastro de motoristas") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cnh, onValueChange = { cnh = it }, label = { Text("CNH") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
                    OutlinedTextField(
                        value = veiculoSelecionado?.let { "${it.placa} · ${it.modelo}" } ?: "Nenhum veículo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Veículo vinculado") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                        DropdownMenuItem(text = { Text("Nenhum veículo") }, onClick = { veiculoSelecionado = null; expandido = false })
                        veiculos.forEach { veiculo ->
                            DropdownMenuItem(
                                text = { Text("${veiculo.placa} · ${veiculo.modelo}") },
                                onClick = { veiculoSelecionado = veiculo; expandido = false }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.salvar(
                            MotoristaEntity(
                                id = editandoId.ifEmpty { UUID.randomUUID().toString() },
                                nome = nome,
                                cnh = cnh,
                                telefone = telefone,
                                veiculoId = veiculoSelecionado?.id
                            )
                        )
                        limparFormulario()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (editandoId.isEmpty()) "Cadastrar motorista" else "Salvar alterações") }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Motoristas cadastrados", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(motoristas, key = { it.id }) { motorista ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            editandoId = motorista.id
                            nome = motorista.nome
                            cnh = motorista.cnh
                            telefone = motorista.telefone
                            veiculoSelecionado = veiculos.find { it.id == motorista.veiculoId }
                        }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(motorista.nome, style = MaterialTheme.typography.titleMedium)
                            val placaVeiculo = veiculos.find { it.id == motorista.veiculoId }?.placa
                            Text("CNH ${motorista.cnh} · ${placaVeiculo ?: "sem veículo"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.data.local.entity.VeiculoEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoristaScreen(onVoltar: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FindUsApplication
    val viewModel: MotoristaViewModel = viewModel(factory = viewModelFactory {
        initializer {
            MotoristaViewModel(app.container.motoristaRepository, app.container.veiculoRepository, app.container.usuarioRepository)
        }
    })
    val motoristas by viewModel.motoristas.collectAsStateWithLifecycle()
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    val estado by viewModel.estado.collectAsState()

    var editandoId by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var cnh by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var veiculoSelecionado by remember { mutableStateOf<VeiculoEntity?>(null) }
    var expandido by remember { mutableStateOf(false) }

    val editando = editandoId.isNotEmpty()

    fun limparFormulario() {
        editandoId = ""; nome = ""; cnh = ""; telefone = ""; email = ""; senha = ""; veiculoSelecionado = null
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

                if (!editando) {
                    Text(
                        "Acesso do motorista ao app",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail de login") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it },
                        label = { Text("Senha de login") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        if (editando) {
                            val motoristaExistente = motoristas.find { it.id == editandoId } ?: return@Button
                            viewModel.atualizar(
                                motoristaExistente.copy(
                                    nome = nome,
                                    cnh = cnh,
                                    telefone = telefone,
                                    veiculoId = veiculoSelecionado?.id
                                )
                            )
                            limparFormulario()
                        } else {
                            viewModel.cadastrarNovo(
                                context = context,
                                nome = nome,
                                cnh = cnh,
                                telefone = telefone,
                                veiculoId = veiculoSelecionado?.id,
                                email = email,
                                senha = senha,
                                onSucesso = ::limparFormulario
                            )
                        }
                    },
                    enabled = !estado.carregando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (estado.carregando) {
                        CircularProgressIndicator(modifier = Modifier.padding(2.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(if (editando) "Salvar alterações" else "Cadastrar motorista")
                    }
                }

                estado.erro?.let { mensagem ->
                    Text(mensagem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
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

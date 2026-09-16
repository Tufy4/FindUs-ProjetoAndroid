package com.example.findus.ui.motorista.cadastros

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.findus.data.enum.TipoNegociante
import com.example.findus.data.local.entity.NegocianteEntity
import com.example.findus.ui.common.EnumDropdown
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegocianteScreen(onVoltar: () -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: NegocianteViewModel = viewModel(factory = viewModelFactory {
        initializer { NegocianteViewModel(app.container.negocianteRepository) }
    })
    val negociantes by viewModel.negociantes.collectAsStateWithLifecycle()

    var editandoId by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var documento by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoNegociante.CLIENTE) }
    var endereco by remember { mutableStateOf("") }

    fun limparFormulario() {
        editandoId = ""; nome = ""; documento = ""; tipo = TipoNegociante.CLIENTE; endereco = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Negociantes") },
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
                OutlinedTextField(value = documento, onValueChange = { documento = it }, label = { Text("Documento (CPF/CNPJ)") }, modifier = Modifier.fillMaxWidth())
                EnumDropdown("Tipo", TipoNegociante.entries.toTypedArray(), tipo, { tipo = it }, Modifier.fillMaxWidth())
                OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        viewModel.salvar(
                            NegocianteEntity(
                                id = editandoId.ifEmpty { UUID.randomUUID().toString() },
                                nome = nome,
                                documento = documento,
                                tipo = tipo,
                                endereco = endereco
                            )
                        )
                        limparFormulario()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (editandoId.isEmpty()) "Cadastrar negociante" else "Salvar alterações") }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Negociantes cadastrados", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(negociantes, key = { it.id }) { negociante ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            editandoId = negociante.id
                            nome = negociante.nome
                            documento = negociante.documento
                            tipo = negociante.tipo
                            endereco = negociante.endereco
                        }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(negociante.nome, style = MaterialTheme.typography.titleMedium)
                            Text("${negociante.tipo} · ${negociante.documento}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

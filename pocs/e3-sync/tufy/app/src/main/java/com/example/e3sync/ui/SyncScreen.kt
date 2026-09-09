package com.example.e3sync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SyncScreen(
    modifier: Modifier = Modifier,
    viewModel: SyncViewModel = viewModel()
) {
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    val pendentes by viewModel.pendentes.collectAsStateWithLifecycle()
    val offline by viewModel.offlineSimulado.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()

    var placa by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var motorista by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Room + Firestore", style = MaterialTheme.typography.headlineSmall)
        Text(
            "O SQLite e a fonte de verdade da tela. O Firestore recebe as alteracoes depois.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Simular offline")
                    Switch(checked = offline, onCheckedChange = viewModel::alternarOffline)
                }
                Text("Registros locais: ${veiculos.size}")
                Text("Aguardando envio: $pendentes")
                Spacer(Modifier.height(4.dp))
                Text(log, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = placa,
            onValueChange = { placa = it },
            label = { Text("Placa") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = modelo,
            onValueChange = { modelo = it },
            label = { Text("Modelo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = motorista,
            onValueChange = { motorista = it },
            label = { Text("Motorista") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.salvar(placa, modelo, motorista)
                    placa = ""; modelo = ""; motorista = ""
                },
                modifier = Modifier.weight(1f)
            ) { Text("Salvar") }

            OutlinedButton(
                onClick = viewModel::sincronizar,
                modifier = Modifier.weight(1f)
            ) { Text("Sincronizar") }
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(veiculos, key = { it.id }) { veiculo ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(veiculo.placa, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOf(veiculo.modelo, veiculo.motorista)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" | "),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (veiculo.pendenteSync) {
                                AssistChip(onClick = {}, label = { Text("pendente") })
                            }
                        }
                        OutlinedButton(onClick = { viewModel.remover(veiculo.id) }) {
                            Text("Excluir")
                        }
                    }
                }
            }
        }
    }
}

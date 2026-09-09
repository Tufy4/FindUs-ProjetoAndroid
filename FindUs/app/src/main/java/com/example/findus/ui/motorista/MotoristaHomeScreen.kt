package com.example.findus.ui.motorista

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoristaHomeScreen(
    onVoltar: () -> Unit,
    onCadastroVeiculo: () -> Unit,
    onProdutos: () -> Unit,
    onNegociantes: () -> Unit,
    onAvaliacoes: () -> Unit,
    onRotaEntrega: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil Motorista") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onCadastroVeiculo, modifier = Modifier.fillMaxWidth()) { Text("Cadastro de veículo") }
            Button(onClick = onProdutos, modifier = Modifier.fillMaxWidth()) { Text("Produtos") }
            Button(onClick = onNegociantes, modifier = Modifier.fillMaxWidth()) { Text("Negociantes") }
            Button(onClick = onAvaliacoes, modifier = Modifier.fillMaxWidth()) { Text("Avaliações de produto") }
            Button(onClick = onRotaEntrega, modifier = Modifier.fillMaxWidth()) { Text("Rota até a entrega") }
        }
    }
}

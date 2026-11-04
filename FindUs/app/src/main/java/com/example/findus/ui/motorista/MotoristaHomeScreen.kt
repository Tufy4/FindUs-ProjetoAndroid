package com.example.findus.ui.motorista

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoristaHomeScreen(
    onVoltar: () -> Unit,
    onCadastroVeiculo: () -> Unit,
    onRotaEntrega: () -> Unit,
    onMinhasEntregas: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil Motorista") },
                actions = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ExitToApp, contentDescription = "Sair") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CartaoNavegacao(
                icone = Icons.Filled.ShoppingCart,
                titulo = "Minhas entregas",
                descricao = "Veja as entregas atribuídas a você, inicie, conclua e trace a rota",
                onClick = onMinhasEntregas
            )
            CartaoNavegacao(
                icone = Icons.Filled.Build,
                titulo = "Cadastro de veículo",
                descricao = "Informe dados e foto do veículo que você dirige",
                onClick = onCadastroVeiculo
            )
            CartaoNavegacao(
                icone = Icons.Filled.LocationOn,
                titulo = "Rota até um ponto no mapa",
                descricao = "Toque em qualquer ponto do mapa para traçar a rota a partir da sua posição",
                onClick = onRotaEntrega
            )
        }
    }
}

@Composable
private fun CartaoNavegacao(icone: ImageVector, titulo: String, descricao: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(descricao, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

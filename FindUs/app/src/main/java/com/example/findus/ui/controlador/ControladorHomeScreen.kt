package com.example.findus.ui.controlador

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
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
fun ControladorHomeScreen(
    onVoltar: () -> Unit,
    onMapaTelemetria: () -> Unit,
    onCadastroMotoristas: () -> Unit,
    onEntregas: () -> Unit,
    onCentros: () -> Unit,
    onPerimetros: () -> Unit,
    onGrupos: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil Controlador") },
                actions = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ExitToApp, contentDescription = "Sair") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CartaoNavegacao(
                icone = Icons.Filled.ShoppingCart,
                titulo = "Entregas",
                descricao = "Crie entregas e atribua a um motorista e veículo",
                onClick = onEntregas
            )
            CartaoNavegacao(
                icone = Icons.Filled.LocationOn,
                titulo = "Mapa e telemetria da frota",
                descricao = "Acompanhe veículos, rotas e geofencing em tempo real",
                onClick = onMapaTelemetria
            )
            CartaoNavegacao(
                icone = Icons.Filled.AccountBox,
                titulo = "Cadastro de motoristas",
                descricao = "Gerencie os motoristas vinculados à frota",
                onClick = onCadastroMotoristas
            )
            CartaoNavegacao(
                icone = Icons.Filled.Home,
                titulo = "Centros de distribuição",
                descricao = "Locais de saída das entregas, também monitorados por geofencing",
                onClick = onCentros
            )
            CartaoNavegacao(
                icone = Icons.Filled.Place,
                titulo = "Perímetros (geofencing)",
                descricao = "Avisos de entrada e saída para um veículo ou um grupo",
                onClick = onPerimetros
            )
            CartaoNavegacao(
                icone = Icons.Filled.Menu,
                titulo = "Grupos de veículos",
                descricao = "Agrupe veículos para monitorá-los juntos",
                onClick = onGrupos
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

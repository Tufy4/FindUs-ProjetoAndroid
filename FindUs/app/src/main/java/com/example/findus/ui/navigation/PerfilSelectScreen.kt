package com.example.findus.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PerfilSelectScreen(onEscolherMotorista: () -> Unit, onEscolherControlador: () -> Unit, onSair: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FindUs", style = MaterialTheme.typography.headlineMedium)
        Text("Selecione o perfil de acesso", style = MaterialTheme.typography.bodyMedium)

        androidx.compose.foundation.layout.Spacer(Modifier.padding(16.dp))

        Button(onClick = onEscolherMotorista, modifier = Modifier.fillMaxWidth()) { Text("Motorista") }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
        Button(onClick = onEscolherControlador, modifier = Modifier.fillMaxWidth()) { Text("Controlador") }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
        TextButton(onClick = onSair, modifier = Modifier.fillMaxWidth()) { Text("Sair") }
    }
}

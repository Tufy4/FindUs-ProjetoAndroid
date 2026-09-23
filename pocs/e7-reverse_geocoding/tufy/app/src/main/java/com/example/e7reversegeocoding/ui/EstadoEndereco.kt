package com.example.e7reversegeocoding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.e7reversegeocoding.location.Endereco

/** Estado de uma consulta de reverse geocoding exibida na UI. */
sealed interface EstadoEndereco {
    data object Ocioso : EstadoEndereco
    data object Carregando : EstadoEndereco
    data class Encontrado(val endereco: Endereco) : EstadoEndereco
    data class Indisponivel(val motivo: String) : EstadoEndereco
}

fun Endereco?.paraEstado(): EstadoEndereco =
    this?.let { EstadoEndereco.Encontrado(it) }
        ?: EstadoEndereco.Indisponivel("Endereço não encontrado para este ponto.")

@Composable
fun DetalheEndereco(estado: EstadoEndereco, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        when (estado) {
            EstadoEndereco.Ocioso -> Unit
            EstadoEndereco.Carregando -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Buscando endereço…", style = MaterialTheme.typography.bodyMedium)
            }
            is EstadoEndereco.Indisponivel -> Text(estado.motivo, style = MaterialTheme.typography.bodyMedium)
            is EstadoEndereco.Encontrado -> {
                val endereco = estado.endereco
                Text(endereco.linhaCurta, style = MaterialTheme.typography.titleSmall)
                listOfNotNull(endereco.cidade, endereco.estado).joinToString(" / ")
                    .takeIf { it.isNotBlank() }
                    ?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                endereco.cep?.let { Text("CEP $it", style = MaterialTheme.typography.bodySmall) }
                Text(
                    "Fonte: ${endereco.fonte.rotulo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

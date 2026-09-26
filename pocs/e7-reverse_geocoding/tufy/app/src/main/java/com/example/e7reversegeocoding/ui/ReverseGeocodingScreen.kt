package com.example.e7reversegeocoding.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverseGeocodingScreen(viewModel: ReverseGeocodingViewModel = viewModel()) {
    val selecionado by viewModel.selecionado.collectAsStateWithLifecycle()
    val centralizarEm by viewModel.centralizarEm.collectAsStateWithLifecycle()
    val atual by viewModel.atual.collectAsStateWithLifecycle()
    val historico by viewModel.historico.collectAsStateWithLifecycle()
    val forcarNativo by viewModel.forcarNativo.collectAsStateWithLifecycle()

    // Negar a permissão também cai aqui: a consulta devolve "não foi possível obter sua localização".
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.consultarLocalizacaoAtual()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("PoC E7: reverse geocoding") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                MapaSelecao(
                    selecionado = selecionado,
                    centralizarEm = centralizarEm,
                    onToque = viewModel::consultar,
                    modifier = Modifier.fillMaxSize()
                )
                if (selecionado == null) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                        tonalElevation = 4.dp,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "Toque no mapa para consultar o endereço",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!viewModel.chaveConfigurada) {
                    item {
                        Text(
                            "GEOAPIFY_API_KEY não configurada: o mapa fica cinza e só o Geocoder do Android responde.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = forcarNativo, onCheckedChange = viewModel::alternarForcarNativo)
                        Spacer(Modifier.width(8.dp))
                        Text("Forçar Geocoder do Android (simula a API fora do ar)", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { pedirPermissao.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                            Text("Minha localização")
                        }
                        OutlinedButton(onClick = viewModel::consultarDeNovo, enabled = selecionado != null) {
                            Text("Consultar de novo")
                        }
                    }
                }
                atual?.let { consulta -> item { CartaoConsulta(consulta) } }
                if (historico.isNotEmpty()) {
                    item {
                        Text("Histórico", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(historico) { consulta -> LinhaHistorico(consulta) }
                }
            }
        }
    }
}

@Composable
private fun CartaoConsulta(consulta: Consulta) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            consulta.coordenada?.let {
                Text(it.formatada(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DetalheEndereco(consulta.estado)
            consulta.resumo()?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LinhaHistorico(consulta: Consulta) {
    val titulo = when (val estado = consulta.estado) {
        is EstadoEndereco.Encontrado -> estado.endereco.linhaCurta
        is EstadoEndereco.Indisponivel -> estado.motivo
        else -> "…"
    }
    val fonte = (consulta.estado as? EstadoEndereco.Encontrado)?.endereco?.fonte?.rotulo ?: "sem resultado"
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(fonte, consulta.resumo()).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
    }
}

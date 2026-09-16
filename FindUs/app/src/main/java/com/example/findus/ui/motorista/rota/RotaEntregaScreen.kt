package com.example.findus.ui.motorista.rota

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.ui.map.MapaOsm
import com.example.findus.ui.map.MarcadorMapa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotaEntregaScreen(onVoltar: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FindUsApplication
    val viewModel: RotaEntregaViewModel = viewModel(factory = viewModelFactory {
        initializer { RotaEntregaViewModel(app.container.locationHelper, app.container.rotaService) }
    })
    val posicaoAtual by viewModel.posicaoAtual.collectAsStateWithLifecycle()
    val destino by viewModel.destino.collectAsStateWithLifecycle()
    val rota by viewModel.rota.collectAsStateWithLifecycle()

    val lancadorPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.atualizarLocalizacaoAtual()
    }
    LaunchedEffect(Unit) { lancadorPermissao.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rota até a entrega") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val marcadores = mutableListOf<MarcadorMapa>()
            destino?.let { marcadores.add(MarcadorMapa(posicao = it, titulo = "Ponto de entrega", corHex = "#C62828")) }
            posicaoAtual?.let { marcadores.add(MarcadorMapa(posicao = it, titulo = "Sua posição")) }

            MapaOsm(
                marcadores = marcadores,
                modifier = Modifier.fillMaxSize(),
                linha = rota,
                centro = posicaoAtual,
                onToqueMapa = { viewModel.definirDestino(it) }
            )

            if (destino == null) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    tonalElevation = 4.dp
                ) {
                    Box(Modifier.padding(12.dp)) {
                        Text("Toque no mapa para escolher o ponto de entrega", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

package com.example.findus

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.findus.data.enum.TipoUsuario
import com.example.findus.ui.auth.LoginScreen
import com.example.findus.ui.navigation.FindUsNavHost
import com.example.findus.ui.theme.FindUsTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private sealed interface SessaoUiState {
    data object Carregando : SessaoUiState
    data object Deslogado : SessaoUiState
    data class Logado(val tipo: TipoUsuario?) : SessaoUiState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindUsTheme(darkTheme = true, dynamicColor = false) {
                val pedirNotificacao = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val app = application as FindUsApplication
                val escopo = rememberCoroutineScope()
                var sessao by remember { mutableStateOf<SessaoUiState>(SessaoUiState.Carregando) }

                LaunchedEffect(Unit) {
                    val usuario = FirebaseAuth.getInstance().currentUser
                    sessao = if (usuario == null) {
                        SessaoUiState.Deslogado
                    } else {
                        SessaoUiState.Logado(app.container.usuarioRepository.buscarTipo(usuario.uid))
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val estado = sessao) {
                        is SessaoUiState.Carregando -> TelaCarregando()
                        is SessaoUiState.Deslogado -> LoginScreen(
                            onAutenticado = { tipo -> sessao = SessaoUiState.Logado(tipo) }
                        )
                        is SessaoUiState.Logado -> FindUsNavHost(
                            tipoUsuario = estado.tipo,
                            onSelecionarTipo = { tipoEscolhido ->
                                sessao = SessaoUiState.Logado(tipoEscolhido)
                                val uid = FirebaseAuth.getInstance().currentUser?.uid
                                if (uid != null) {
                                    escopo.launch { app.container.usuarioRepository.salvarTipo(uid, tipoEscolhido) }
                                }
                            },
                            onSair = {
                                FirebaseAuth.getInstance().signOut()
                                sessao = SessaoUiState.Deslogado
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelaCarregando() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(
            "Carregando...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

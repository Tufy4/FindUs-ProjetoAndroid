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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
    data class Erro(val mensagem: String) : SessaoUiState
    data class Logado(val tipo: TipoUsuario?) : SessaoUiState
}

/**
 * A sessão sobrevive à rotação da tela. Antes ficava em remember: ao girar o aparelho
 * o app voltava para "Carregando", consultava o Firestore de novo e a navegação era
 * recriada, levando o usuário de volta para a tela inicial do perfil.
 */
private val SessaoSaver = Saver<SessaoUiState, String>(
    save = { estado ->
        when (estado) {
            is SessaoUiState.Logado -> "L:" + estado.tipo?.name.orEmpty()
            SessaoUiState.Deslogado -> "D"
            else -> "C"
        }
    },
    restore = { texto ->
        when {
            texto == "D" -> SessaoUiState.Deslogado
            texto.startsWith("L:") -> SessaoUiState.Logado(
                TipoUsuario.entries.find { it.name == texto.removePrefix("L:") }
            )
            else -> SessaoUiState.Carregando
        }
    }
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindUsTheme(darkTheme = true, dynamicColor = false) {
                val pedirNotificacao = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }
                var jaPediuNotificacao by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33 && !jaPediuNotificacao) {
                        jaPediuNotificacao = true
                        pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val app = application as FindUsApplication
                val escopo = rememberCoroutineScope()
                var sessao by rememberSaveable(stateSaver = SessaoSaver) {
                    mutableStateOf<SessaoUiState>(SessaoUiState.Carregando)
                }
                var tentativa by rememberSaveable { mutableStateOf(0) }

                LaunchedEffect(tentativa) {
                    if (sessao !is SessaoUiState.Carregando) return@LaunchedEffect
                    val usuario = FirebaseAuth.getInstance().currentUser
                    sessao = if (usuario == null) {
                        SessaoUiState.Deslogado
                    } else {
                        // Sem internet e sem perfil salvo no aparelho, buscarTipo lança exceção:
                        // antes isso derrubava o app na abertura.
                        runCatching { app.container.usuarioRepository.buscarTipo(usuario.uid) }.fold(
                            onSuccess = { SessaoUiState.Logado(it) },
                            onFailure = { SessaoUiState.Erro("Não foi possível carregar seu perfil. Verifique a conexão.") }
                        )
                    }
                }

                LaunchedEffect(sessao) {
                    app.container.definirPerfilDaSessao((sessao as? SessaoUiState.Logado)?.tipo)
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val estado = sessao) {
                        is SessaoUiState.Carregando -> TelaCarregando()
                        is SessaoUiState.Erro -> TelaErroSessao(
                            mensagem = estado.mensagem,
                            onTentarNovamente = {
                                sessao = SessaoUiState.Carregando
                                tentativa++
                            },
                            onSair = {
                                FirebaseAuth.getInstance().signOut()
                                sessao = SessaoUiState.Deslogado
                            }
                        )
                        is SessaoUiState.Deslogado -> LoginScreen(
                            onAutenticado = { tipo -> sessao = SessaoUiState.Logado(tipo) }
                        )
                        is SessaoUiState.Logado -> FindUsNavHost(
                            tipoUsuario = estado.tipo,
                            onSelecionarTipo = { tipoEscolhido ->
                                sessao = SessaoUiState.Logado(tipoEscolhido)
                                val uid = FirebaseAuth.getInstance().currentUser?.uid
                                if (uid != null) {
                                    escopo.launch {
                                        runCatching { app.container.usuarioRepository.salvarTipo(uid, tipoEscolhido) }
                                    }
                                }
                            },
                            onSair = {
                                // As simulações rodam no escopo do app; sem isso continuavam
                                // gravando telemetria depois do logout.
                                app.container.telemetriaSimuladorManager.pararTodos()
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

@Composable
private fun TelaErroSessao(mensagem: String, onTentarNovamente: () -> Unit, onSair: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(mensagem, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Button(onClick = onTentarNovamente, modifier = Modifier.padding(top = 16.dp)) { Text("Tentar novamente") }
        TextButton(onClick = onSair) { Text("Sair") }
    }
}

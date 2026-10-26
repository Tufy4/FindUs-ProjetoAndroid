package com.example.findus.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.enum.TipoUsuario
import com.example.findus.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val carregando: Boolean = false,
    val erro: String? = null
)

class AuthViewModel(private val usuarioRepository: UsuarioRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _estado = MutableStateFlow(AuthUiState())
    val estado: StateFlow<AuthUiState> = _estado.asStateFlow()

    fun entrar(email: String, senha: String, onSucesso: (TipoUsuario?) -> Unit) {
        if (email.isBlank() || senha.isBlank()) {
            _estado.update { it.copy(erro = "Preencha e-mail e senha") }
            return
        }
        _estado.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, senha).await()
                val uid = auth.currentUser?.uid.orEmpty()
                val tipo = usuarioRepository.buscarTipo(uid)
                _estado.update { it.copy(carregando = false) }
                onSucesso(tipo)
            } catch (e: Exception) {
                _estado.update { it.copy(carregando = false, erro = "Não foi possível entrar: ${e.message}") }
            }
        }
    }

    fun cadastrar(email: String, senha: String, tipo: TipoUsuario, onSucesso: (TipoUsuario) -> Unit) {
        if (email.isBlank() || senha.isBlank()) {
            _estado.update { it.copy(erro = "Preencha e-mail e senha") }
            return
        }
        _estado.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, senha).await()
                val uid = auth.currentUser?.uid.orEmpty()
                usuarioRepository.salvarTipo(uid, tipo)
                _estado.update { it.copy(carregando = false) }
                onSucesso(tipo)
            } catch (e: Exception) {
                _estado.update { it.copy(carregando = false, erro = "Não foi possível cadastrar: ${e.message}") }
            }
        }
    }

    fun limparErro() = _estado.update { it.copy(erro = null) }
}

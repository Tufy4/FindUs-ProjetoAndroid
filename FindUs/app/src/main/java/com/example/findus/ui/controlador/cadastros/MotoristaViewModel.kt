package com.example.findus.ui.controlador.cadastros

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.enum.TipoUsuario
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.EntregaRepository
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.UsuarioRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.domain.RegrasNegocio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MotoristaUiState(
    val carregando: Boolean = false,
    val erro: String? = null
)

class MotoristaViewModel(
    private val motoristaRepository: MotoristaRepository,
    veiculoRepository: VeiculoRepository,
    private val usuarioRepository: UsuarioRepository,
    entregaRepository: EntregaRepository
) : ViewModel() {
    val motoristas: StateFlow<List<MotoristaEntity>> = motoristaRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val veiculos: StateFlow<List<VeiculoEntity>> = veiculoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val entregas: StateFlow<List<EntregaEntity>> = entregaRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _estado = MutableStateFlow(MotoristaUiState())
    val estado: StateFlow<MotoristaUiState> = _estado.asStateFlow()

    /** Cadastra um novo motorista e cria a conta de login dele (apenas o controlador pode fazer isso). */
    fun cadastrarNovo(
        context: Context,
        nome: String,
        cnh: String,
        telefone: String,
        veiculoId: String?,
        email: String,
        senha: String,
        onSucesso: () -> Unit
    ) {
        val invalido = RegrasNegocio.validarMotorista(nome, cnh)
            ?: if (email.isBlank() || senha.isBlank()) "Informe e-mail e senha de acesso do motorista" else null
        if (invalido != null) {
            _estado.update { it.copy(erro = invalido) }
            return
        }
        _estado.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            try {
                val uid = usuarioRepository.criarContaMotorista(context, email, senha)
                usuarioRepository.salvarTipo(uid, TipoUsuario.MOTORISTA)
                motoristaRepository.salvar(
                    MotoristaEntity(uid = uid, nome = nome.trim(), cnh = cnh.trim(), telefone = telefone.trim(), veiculoId = veiculoId)
                )
                _estado.update { it.copy(carregando = false) }
                onSucesso()
            } catch (e: Exception) {
                _estado.update { it.copy(carregando = false, erro = "Não foi possível cadastrar o motorista: ${e.message}") }
            }
        }
    }

    /** Devolve false (e mostra o erro) quando a alteração não pode ser salva. */
    fun atualizar(motorista: MotoristaEntity): Boolean {
        val anterior = motoristas.value.find { it.id == motorista.id } ?: motorista
        val invalido = RegrasNegocio.validarMotorista(motorista.nome, motorista.cnh)
            ?: RegrasNegocio.validarTrocaVeiculo(anterior, motorista.veiculoId, entregas.value)
        _estado.update { it.copy(erro = invalido) }
        if (invalido != null) return false
        viewModelScope.launch { motoristaRepository.salvar(motorista) }
        return true
    }

    fun remover(motorista: MotoristaEntity) = viewModelScope.launch { motoristaRepository.remover(motorista) }

    fun limparErro() = _estado.update { it.copy(erro = null) }
}

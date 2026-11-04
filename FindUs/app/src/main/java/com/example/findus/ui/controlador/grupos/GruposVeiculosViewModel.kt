package com.example.findus.ui.controlador.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.GrupoVeiculoEntity
import com.example.findus.data.local.entity.PerimetroEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.GrupoVeiculoRepository
import com.example.findus.data.repository.PerimetroRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.domain.RegrasNegocio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GruposVeiculosViewModel(
    private val grupoRepository: GrupoVeiculoRepository,
    veiculoRepository: VeiculoRepository,
    perimetroRepository: PerimetroRepository
) : ViewModel() {
    val grupos: StateFlow<List<GrupoVeiculoEntity>> = grupoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val veiculos: StateFlow<List<VeiculoEntity>> = veiculoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val perimetros: StateFlow<List<PerimetroEntity>> = perimetroRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    fun limparErro() {
        _erro.value = null
    }

    fun salvar(existente: GrupoVeiculoEntity?, nome: String, veiculoIds: Set<String>, onSucesso: () -> Unit) {
        // Ignora veículos que foram removidos da frota depois de entrar no grupo.
        val idsValidos = veiculoIds.filter { id -> veiculos.value.any { it.id == id } }
        val mensagem = RegrasNegocio.validarGrupo(nome, idsValidos)
        if (mensagem != null) {
            _erro.value = mensagem
            return
        }
        val grupo = (existente ?: GrupoVeiculoEntity()).copy(nome = nome.trim(), veiculoIds = idsValidos)
        _erro.value = null
        viewModelScope.launch {
            grupoRepository.salvar(grupo)
            onSucesso()
        }
    }

    fun remover(grupo: GrupoVeiculoEntity, onSucesso: () -> Unit) {
        val mensagem = RegrasNegocio.validarRemocaoGrupo(grupo, perimetros.value)
        if (mensagem != null) {
            _erro.value = mensagem
            return
        }
        viewModelScope.launch {
            grupoRepository.remover(grupo)
            onSucesso()
        }
    }
}

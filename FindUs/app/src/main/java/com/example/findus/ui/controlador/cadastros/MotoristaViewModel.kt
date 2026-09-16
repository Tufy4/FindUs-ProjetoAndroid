package com.example.findus.ui.controlador.cadastros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.VeiculoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MotoristaViewModel(
    private val motoristaRepository: MotoristaRepository,
    veiculoRepository: VeiculoRepository
) : ViewModel() {
    val motoristas: StateFlow<List<MotoristaEntity>> = motoristaRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val veiculos: StateFlow<List<VeiculoEntity>> = veiculoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(motorista: MotoristaEntity) = viewModelScope.launch {
        motoristaRepository.salvar(motorista)
    }

    fun remover(motorista: MotoristaEntity) = viewModelScope.launch { motoristaRepository.remover(motorista) }
}

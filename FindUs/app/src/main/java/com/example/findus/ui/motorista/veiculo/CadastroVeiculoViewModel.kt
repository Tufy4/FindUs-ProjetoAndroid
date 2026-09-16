package com.example.findus.ui.motorista.veiculo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.VeiculoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CadastroVeiculoViewModel(private val repository: VeiculoRepository) : ViewModel() {
    val veiculos: StateFlow<List<VeiculoEntity>> = repository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(veiculo: VeiculoEntity) = viewModelScope.launch {
        repository.salvar(veiculo)
    }

    fun remover(veiculo: VeiculoEntity) = viewModelScope.launch { repository.remover(veiculo) }
}

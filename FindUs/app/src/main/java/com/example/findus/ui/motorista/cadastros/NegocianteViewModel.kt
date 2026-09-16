package com.example.findus.ui.motorista.cadastros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.NegocianteEntity
import com.example.findus.data.repository.NegocianteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NegocianteViewModel(private val repository: NegocianteRepository) : ViewModel() {
    val negociantes: StateFlow<List<NegocianteEntity>> = repository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(negociante: NegocianteEntity) = viewModelScope.launch {
        repository.salvar(negociante)
    }

    fun remover(negociante: NegocianteEntity) = viewModelScope.launch { repository.remover(negociante) }
}

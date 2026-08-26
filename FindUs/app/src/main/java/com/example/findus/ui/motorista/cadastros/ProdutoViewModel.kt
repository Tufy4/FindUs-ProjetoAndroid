package com.example.findus.ui.motorista.cadastros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.ProdutoEntity
import com.example.findus.data.repository.ProdutoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProdutoViewModel(private val repository: ProdutoRepository) : ViewModel() {
    val produtos: StateFlow<List<ProdutoEntity>> = repository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(produto: ProdutoEntity) = viewModelScope.launch {
        if (produto.id == 0L) repository.salvar(produto) else repository.atualizar(produto)
    }

    fun remover(produto: ProdutoEntity) = viewModelScope.launch { repository.remover(produto) }
}

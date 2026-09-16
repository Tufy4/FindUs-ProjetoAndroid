package com.example.findus.ui.motorista.cadastros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.AvaliacaoProdutoEntity
import com.example.findus.data.local.entity.ProdutoEntity
import com.example.findus.data.repository.AvaliacaoProdutoRepository
import com.example.findus.data.repository.ProdutoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AvaliacaoProdutoViewModel(
    private val avaliacaoRepository: AvaliacaoProdutoRepository,
    produtoRepository: ProdutoRepository
) : ViewModel() {
    val avaliacoes: StateFlow<List<AvaliacaoProdutoEntity>> = avaliacaoRepository.observarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val produtos: StateFlow<List<ProdutoEntity>> = produtoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(avaliacao: AvaliacaoProdutoEntity) = viewModelScope.launch {
        avaliacaoRepository.salvar(avaliacao)
    }

    fun remover(avaliacao: AvaliacaoProdutoEntity) = viewModelScope.launch { avaliacaoRepository.remover(avaliacao) }
}

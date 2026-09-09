package com.example.findus.data.repository

import com.example.findus.data.local.dao.AvaliacaoProdutoDao
import com.example.findus.data.local.entity.AvaliacaoProdutoEntity
import kotlinx.coroutines.flow.Flow

class AvaliacaoProdutoRepository(private val dao: AvaliacaoProdutoDao) {
    fun observarTodas(): Flow<List<AvaliacaoProdutoEntity>> = dao.observarTodas()

    suspend fun salvar(avaliacao: AvaliacaoProdutoEntity): Long = dao.inserir(avaliacao)

    suspend fun atualizar(avaliacao: AvaliacaoProdutoEntity) = dao.atualizar(avaliacao)

    suspend fun remover(avaliacao: AvaliacaoProdutoEntity) = dao.remover(avaliacao)
}

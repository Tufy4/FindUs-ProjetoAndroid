package com.example.findus.data.repository

import com.example.findus.data.local.dao.ProdutoDao
import com.example.findus.data.local.entity.ProdutoEntity
import kotlinx.coroutines.flow.Flow

class ProdutoRepository(private val dao: ProdutoDao) {
    fun observarTodos(): Flow<List<ProdutoEntity>> = dao.observarTodos()

    suspend fun salvar(produto: ProdutoEntity): Long = dao.inserir(produto)

    suspend fun atualizar(produto: ProdutoEntity) = dao.atualizar(produto)

    suspend fun remover(produto: ProdutoEntity) = dao.remover(produto)
}

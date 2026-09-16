package com.example.findus.data.repository

import com.example.findus.data.local.dao.ProdutoDao
import com.example.findus.data.local.entity.ProdutoEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.flow.Flow

class ProdutoRepository(
    private val dao: ProdutoDao,
    private val remoto: FirestoreColecao<ProdutoEntity>
) {
    fun observarTodos(): Flow<List<ProdutoEntity>> = dao.observarTodos()

    suspend fun salvar(produto: ProdutoEntity) {
        dao.inserir(produto)
        remoto.enviar(produto.id, produto)
    }

    suspend fun remover(produto: ProdutoEntity) = salvar(produto.copy(deletado = true))
}

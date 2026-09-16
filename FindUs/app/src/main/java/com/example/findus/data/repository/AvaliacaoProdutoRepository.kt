package com.example.findus.data.repository

import com.example.findus.data.local.dao.AvaliacaoProdutoDao
import com.example.findus.data.local.entity.AvaliacaoProdutoEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.flow.Flow

class AvaliacaoProdutoRepository(
    private val dao: AvaliacaoProdutoDao,
    private val remoto: FirestoreColecao<AvaliacaoProdutoEntity>
) {
    fun observarTodas(): Flow<List<AvaliacaoProdutoEntity>> = dao.observarTodas()

    suspend fun salvar(avaliacao: AvaliacaoProdutoEntity) {
        dao.inserir(avaliacao)
        remoto.enviar(avaliacao.id, avaliacao)
    }

    suspend fun remover(avaliacao: AvaliacaoProdutoEntity) = salvar(avaliacao.copy(deletado = true))
}

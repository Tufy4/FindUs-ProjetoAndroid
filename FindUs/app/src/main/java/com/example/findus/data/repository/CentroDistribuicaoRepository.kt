package com.example.findus.data.repository

import com.example.findus.data.local.dao.CentroDistribuicaoDao
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.flow.Flow

class CentroDistribuicaoRepository(
    private val dao: CentroDistribuicaoDao,
    private val remoto: FirestoreColecao<CentroDistribuicaoEntity>
) {
    fun observarTodos(): Flow<List<CentroDistribuicaoEntity>> = dao.observarTodos()

    suspend fun buscarPorId(id: String): CentroDistribuicaoEntity? = dao.buscarPorId(id)

    suspend fun salvar(centro: CentroDistribuicaoEntity) {
        dao.inserir(centro)
        remoto.enviar(centro.id, centro)
    }

    suspend fun remover(centro: CentroDistribuicaoEntity) = salvar(centro.copy(deletado = true))
}

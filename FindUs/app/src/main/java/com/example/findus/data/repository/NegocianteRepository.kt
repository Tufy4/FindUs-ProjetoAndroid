package com.example.findus.data.repository

import com.example.findus.data.local.dao.NegocianteDao
import com.example.findus.data.local.entity.NegocianteEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.flow.Flow

class NegocianteRepository(
    private val dao: NegocianteDao,
    private val remoto: FirestoreColecao<NegocianteEntity>
) {
    fun observarTodos(): Flow<List<NegocianteEntity>> = dao.observarTodos()

    suspend fun salvar(negociante: NegocianteEntity) {
        dao.inserir(negociante)
        remoto.enviar(negociante.id, negociante)
    }

    suspend fun remover(negociante: NegocianteEntity) = salvar(negociante.copy(deletado = true))
}

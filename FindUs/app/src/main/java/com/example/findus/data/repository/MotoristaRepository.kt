package com.example.findus.data.repository

import com.example.findus.data.local.dao.MotoristaDao
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.flow.Flow

class MotoristaRepository(
    private val dao: MotoristaDao,
    private val remoto: FirestoreColecao<MotoristaEntity>
) {
    fun observarTodos(): Flow<List<MotoristaEntity>> = dao.observarTodos()

    suspend fun salvar(motorista: MotoristaEntity) {
        dao.inserir(motorista)
        remoto.enviar(motorista.id, motorista)
    }

    suspend fun remover(motorista: MotoristaEntity) = salvar(motorista.copy(deletado = true))
}

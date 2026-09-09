package com.example.findus.data.repository

import com.example.findus.data.local.dao.MotoristaDao
import com.example.findus.data.local.entity.MotoristaEntity
import kotlinx.coroutines.flow.Flow

class MotoristaRepository(private val dao: MotoristaDao) {
    fun observarTodos(): Flow<List<MotoristaEntity>> = dao.observarTodos()

    suspend fun salvar(motorista: MotoristaEntity): Long = dao.inserir(motorista)

    suspend fun atualizar(motorista: MotoristaEntity) = dao.atualizar(motorista)

    suspend fun remover(motorista: MotoristaEntity) = dao.remover(motorista)
}

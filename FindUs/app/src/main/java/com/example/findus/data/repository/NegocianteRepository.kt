package com.example.findus.data.repository

import com.example.findus.data.local.dao.NegocianteDao
import com.example.findus.data.local.entity.NegocianteEntity
import kotlinx.coroutines.flow.Flow

class NegocianteRepository(private val dao: NegocianteDao) {
    fun observarTodos(): Flow<List<NegocianteEntity>> = dao.observarTodos()

    suspend fun salvar(negociante: NegocianteEntity): Long = dao.inserir(negociante)

    suspend fun atualizar(negociante: NegocianteEntity) = dao.atualizar(negociante)

    suspend fun remover(negociante: NegocianteEntity) = dao.remover(negociante)
}

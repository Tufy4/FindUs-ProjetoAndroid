package com.example.findus.data.repository

import com.example.findus.data.local.dao.PerimetroDao
import com.example.findus.data.local.entity.PerimetroEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.flow.Flow

class PerimetroRepository(
    private val dao: PerimetroDao,
    private val remoto: FirestoreColecao<PerimetroEntity>
) {
    fun observarTodos(): Flow<List<PerimetroEntity>> = dao.observarTodos()

    suspend fun salvar(perimetro: PerimetroEntity) {
        dao.inserir(perimetro)
        remoto.enviar(perimetro.id, perimetro)
    }

    suspend fun remover(perimetro: PerimetroEntity) = salvar(perimetro.copy(deletado = true))
}

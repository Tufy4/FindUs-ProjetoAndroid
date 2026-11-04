package com.example.findus.data.repository

import com.example.findus.data.local.dao.GrupoVeiculoDao
import com.example.findus.data.local.entity.GrupoVeiculoEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.flow.Flow

class GrupoVeiculoRepository(
    private val dao: GrupoVeiculoDao,
    private val remoto: FirestoreColecao<GrupoVeiculoEntity>
) {
    fun observarTodos(): Flow<List<GrupoVeiculoEntity>> = dao.observarTodos()

    suspend fun salvar(grupo: GrupoVeiculoEntity) {
        dao.inserir(grupo)
        remoto.enviar(grupo.id, grupo)
    }

    suspend fun remover(grupo: GrupoVeiculoEntity) = salvar(grupo.copy(deletado = true))
}

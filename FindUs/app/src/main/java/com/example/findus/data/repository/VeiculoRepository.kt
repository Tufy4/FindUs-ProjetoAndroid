package com.example.findus.data.repository

import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.local.dao.VeiculoDao
import com.example.findus.data.local.entity.VeiculoEntity
import kotlinx.coroutines.flow.Flow

class VeiculoRepository(private val dao: VeiculoDao) {
    fun observarTodos(): Flow<List<VeiculoEntity>> = dao.observarTodos()

    fun observarPorId(id: Long): Flow<VeiculoEntity?> = dao.observarPorId(id)

    suspend fun buscarPorId(id: Long): VeiculoEntity? = dao.buscarPorId(id)

    suspend fun salvar(veiculo: VeiculoEntity): Long = dao.inserir(veiculo)

    suspend fun atualizar(veiculo: VeiculoEntity) = dao.atualizar(veiculo)

    suspend fun remover(veiculo: VeiculoEntity) = dao.remover(veiculo)

    suspend fun atualizarStatus(veiculoId: Long, status: StatusOperacionalVeiculo) =
        dao.atualizarStatus(veiculoId, status)
}

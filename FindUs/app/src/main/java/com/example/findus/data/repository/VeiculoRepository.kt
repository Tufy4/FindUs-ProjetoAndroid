package com.example.findus.data.repository

import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.local.dao.VeiculoDao
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.sync.VeiculoSync
import kotlinx.coroutines.flow.Flow

class VeiculoRepository(
    private val dao: VeiculoDao,
    private val sync: VeiculoSync? = null
) {
    fun observarTodos(): Flow<List<VeiculoEntity>> = dao.observarTodos()

    fun observarPorId(id: Long): Flow<VeiculoEntity?> = dao.observarPorId(id)

    suspend fun buscarPorId(id: Long): VeiculoEntity? = dao.buscarPorId(id)

    suspend fun salvar(veiculo: VeiculoEntity): Long {
        val id = dao.inserir(veiculo.copy(pendenteSync = true, atualizadoEm = System.currentTimeMillis()))
        sync?.agendarPush()
        return id
    }

    suspend fun atualizar(veiculo: VeiculoEntity) {
        dao.atualizar(veiculo.copy(pendenteSync = true, atualizadoEm = System.currentTimeMillis()))
        sync?.agendarPush()
    }

    suspend fun remover(veiculo: VeiculoEntity) {
        dao.atualizar(
            veiculo.copy(
                deletado = true,
                pendenteSync = true,
                atualizadoEm = System.currentTimeMillis()
            )
        )
        sync?.agendarPush()
    }

    suspend fun atualizarStatus(veiculoId: Long, status: StatusOperacionalVeiculo) =
        dao.atualizarStatus(veiculoId, status)
}

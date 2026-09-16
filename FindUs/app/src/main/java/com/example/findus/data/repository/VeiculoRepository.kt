package com.example.findus.data.repository

import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.local.dao.VeiculoDao
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.flow.Flow

class VeiculoRepository(
    private val dao: VeiculoDao,
    private val remoto: FirestoreColecao<VeiculoEntity>
) {
    fun observarTodos(): Flow<List<VeiculoEntity>> = dao.observarTodos()

    suspend fun salvar(veiculo: VeiculoEntity) {
        dao.inserir(veiculo)
        remoto.enviar(veiculo.id, veiculo)
    }

    suspend fun remover(veiculo: VeiculoEntity) = salvar(veiculo.copy(deletado = true))

    suspend fun atualizarStatus(veiculoId: String, status: StatusOperacionalVeiculo) {
        dao.buscarPorId(veiculoId)?.let { salvar(it.copy(status = status)) }
    }
}

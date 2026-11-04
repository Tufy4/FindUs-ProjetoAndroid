package com.example.findus.data.repository

import com.example.findus.data.enum.StatusEntrega
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.local.dao.EntregaDao
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.flow.Flow

/**
 * Além de salvar, aplica as transições de status da entrega e mantém o status
 * operacional do veículo coerente com elas (em rota enquanto a entrega está em rota).
 */
class EntregaRepository(
    private val dao: EntregaDao,
    private val remoto: FirestoreColecao<EntregaEntity>,
    private val veiculoRepository: VeiculoRepository
) {
    fun observarTodos(): Flow<List<EntregaEntity>> = dao.observarTodos()

    suspend fun entregaAtivaDoVeiculo(veiculoId: String): EntregaEntity? = dao.buscarAtivaDoVeiculo(veiculoId)

    suspend fun salvar(entrega: EntregaEntity) {
        dao.inserir(entrega)
        remoto.enviar(entrega.id, entrega)
    }

    suspend fun iniciar(entrega: EntregaEntity) {
        salvar(entrega.copy(status = StatusEntrega.EM_ROTA, iniciadaEm = System.currentTimeMillis()))
        veiculoRepository.atualizarStatus(entrega.veiculoId, StatusOperacionalVeiculo.EM_ROTA)
    }

    suspend fun concluir(entrega: EntregaEntity) {
        salvar(entrega.copy(status = StatusEntrega.ENTREGUE, concluidaEm = System.currentTimeMillis()))
        veiculoRepository.atualizarStatus(entrega.veiculoId, StatusOperacionalVeiculo.DISPONIVEL)
    }

    suspend fun cancelar(entrega: EntregaEntity) {
        salvar(entrega.copy(status = StatusEntrega.CANCELADA, concluidaEm = System.currentTimeMillis()))
        if (entrega.status == StatusEntrega.EM_ROTA) {
            veiculoRepository.atualizarStatus(entrega.veiculoId, StatusOperacionalVeiculo.DISPONIVEL)
        }
    }
}

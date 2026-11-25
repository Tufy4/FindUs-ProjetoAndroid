package com.example.findus.data.repository

import com.example.findus.data.local.dao.RegistroTelemetriaDao
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.flow.Flow

class TelemetriaRepository(
    private val dao: RegistroTelemetriaDao,
    private val remoto: FirestoreColecao<RegistroTelemetriaEntity>
) {
    suspend fun registrar(registro: RegistroTelemetriaEntity) {
        dao.inserir(registro)
        remoto.enviar(registro.id, registro)
    }

    fun observarUltimoDoVeiculo(veiculoId: String): Flow<RegistroTelemetriaEntity?> =
        dao.observarUltimoDoVeiculo(veiculoId)

    fun observarUltimoPorVeiculo(): Flow<List<RegistroTelemetriaEntity>> =
        dao.observarUltimoPorVeiculo()

    /** Mantém só as últimas [horas] de histórico local (o último registro de cada veículo nunca é apagado). */
    suspend fun limparHistoricoLocal(horas: Int = 24): Int =
        dao.apagarHistoricoAnteriorA(System.currentTimeMillis() - horas * 3_600_000L)
}

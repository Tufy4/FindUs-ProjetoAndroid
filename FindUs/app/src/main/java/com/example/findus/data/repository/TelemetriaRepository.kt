package com.example.findus.data.repository

import com.example.findus.data.local.dao.RegistroTelemetriaDao
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import kotlinx.coroutines.flow.Flow

class TelemetriaRepository(private val dao: RegistroTelemetriaDao) {
    suspend fun registrar(registro: RegistroTelemetriaEntity): Long = dao.inserir(registro)

    fun observarUltimoDoVeiculo(veiculoId: Long): Flow<RegistroTelemetriaEntity?> =
        dao.observarUltimoDoVeiculo(veiculoId)

    fun observarUltimoPorVeiculo(): Flow<List<RegistroTelemetriaEntity>> =
        dao.observarUltimoPorVeiculo()

    fun observarHistoricoDoVeiculo(veiculoId: Long): Flow<List<RegistroTelemetriaEntity>> =
        dao.observarHistoricoDoVeiculo(veiculoId)
}

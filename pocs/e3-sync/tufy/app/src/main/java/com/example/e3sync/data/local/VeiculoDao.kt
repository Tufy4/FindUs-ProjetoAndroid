package com.example.e3sync.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VeiculoDao {

    @Query("SELECT * FROM veiculos WHERE deletado = 0 ORDER BY placa")
    fun observarAtivos(): Flow<List<VeiculoEntity>>

    @Query("SELECT COUNT(*) FROM veiculos WHERE pendenteSync = 1")
    fun observarQuantidadePendente(): Flow<Int>

    @Query("SELECT * FROM veiculos WHERE pendenteSync = 1")
    suspend fun listarPendentes(): List<VeiculoEntity>

    @Query("SELECT * FROM veiculos WHERE id = :id")
    suspend fun buscarPorId(id: String): VeiculoEntity?

    @Upsert
    suspend fun salvar(veiculo: VeiculoEntity)

    @Query("UPDATE veiculos SET pendenteSync = 0 WHERE id = :id AND atualizadoEm = :atualizadoEm")
    suspend fun marcarComoSincronizado(id: String, atualizadoEm: Long): Int
}

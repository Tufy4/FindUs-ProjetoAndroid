package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroTelemetriaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(registro: RegistroTelemetriaEntity): Long

    @Query("SELECT * FROM registros_telemetria WHERE veiculoId = :veiculoId ORDER BY timestamp DESC LIMIT 1")
    fun observarUltimoDoVeiculo(veiculoId: Long): Flow<RegistroTelemetriaEntity?>

    @Query(
        """
        SELECT t.* FROM registros_telemetria t
        INNER JOIN (
            SELECT veiculoId, MAX(timestamp) AS maxTs
            FROM registros_telemetria
            GROUP BY veiculoId
        ) ultimo ON t.veiculoId = ultimo.veiculoId AND t.timestamp = ultimo.maxTs
        """
    )
    fun observarUltimoPorVeiculo(): Flow<List<RegistroTelemetriaEntity>>

    @Query("SELECT * FROM registros_telemetria WHERE veiculoId = :veiculoId ORDER BY timestamp ASC")
    fun observarHistoricoDoVeiculo(veiculoId: Long): Flow<List<RegistroTelemetriaEntity>>
}

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
    suspend fun inserir(registro: RegistroTelemetriaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(registros: List<RegistroTelemetriaEntity>)

    @Query("SELECT * FROM registros_telemetria WHERE veiculoId = :veiculoId ORDER BY timestamp DESC LIMIT 1")
    fun observarUltimoDoVeiculo(veiculoId: String): Flow<RegistroTelemetriaEntity?>

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

    /**
     * Apaga o histórico antigo, preservando sempre o último registro de cada veículo
     * (senão o veículo parado há muito tempo sumiria do mapa). Sem isso a tabela crescia
     * sem limite (um registro a cada 2 s por veículo simulado) e a consulta acima ficava lenta.
     */
    @Query(
        """
        DELETE FROM registros_telemetria
        WHERE timestamp < :limite AND id NOT IN (
            SELECT t.id FROM registros_telemetria t
            INNER JOIN (
                SELECT veiculoId, MAX(timestamp) AS maxTs
                FROM registros_telemetria
                GROUP BY veiculoId
            ) ultimo ON t.veiculoId = ultimo.veiculoId AND t.timestamp = ultimo.maxTs
        )
        """
    )
    suspend fun apagarHistoricoAnteriorA(limite: Long): Int
}

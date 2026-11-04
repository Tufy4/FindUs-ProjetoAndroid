package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.findus.data.local.entity.EntregaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntregaDao {
    @Query("SELECT * FROM entregas WHERE deletado = 0 ORDER BY criadaEm DESC")
    fun observarTodos(): Flow<List<EntregaEntity>>

    /** Entrega em rota do veículo ou, se não houver, a pendente mais antiga. */
    @Query(
        """
        SELECT * FROM entregas
        WHERE veiculoId = :veiculoId AND deletado = 0 AND status IN ('EM_ROTA', 'PENDENTE')
        ORDER BY CASE status WHEN 'EM_ROTA' THEN 0 ELSE 1 END, criadaEm ASC
        LIMIT 1
        """
    )
    suspend fun buscarAtivaDoVeiculo(veiculoId: String): EntregaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(entrega: EntregaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(entregas: List<EntregaEntity>)
}

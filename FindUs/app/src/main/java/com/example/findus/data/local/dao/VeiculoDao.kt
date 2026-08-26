package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.local.entity.VeiculoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VeiculoDao {
    @Query("SELECT * FROM veiculos ORDER BY placa ASC")
    fun observarTodos(): Flow<List<VeiculoEntity>>

    @Query("SELECT * FROM veiculos WHERE id = :id")
    fun observarPorId(id: Long): Flow<VeiculoEntity?>

    @Query("SELECT * FROM veiculos WHERE id = :id")
    suspend fun buscarPorId(id: Long): VeiculoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(veiculo: VeiculoEntity): Long

    @Update
    suspend fun atualizar(veiculo: VeiculoEntity)

    @Delete
    suspend fun remover(veiculo: VeiculoEntity)

    @Query("UPDATE veiculos SET status = :status WHERE id = :veiculoId")
    suspend fun atualizarStatus(veiculoId: Long, status: StatusOperacionalVeiculo)
}

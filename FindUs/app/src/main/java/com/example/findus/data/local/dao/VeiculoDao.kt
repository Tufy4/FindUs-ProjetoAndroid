package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.findus.data.local.entity.VeiculoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VeiculoDao {
    @Query("SELECT * FROM veiculos WHERE deletado = 0 ORDER BY placa ASC")
    fun observarTodos(): Flow<List<VeiculoEntity>>

    @Query("SELECT * FROM veiculos WHERE id = :id")
    suspend fun buscarPorId(id: String): VeiculoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(veiculo: VeiculoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(veiculos: List<VeiculoEntity>)
}

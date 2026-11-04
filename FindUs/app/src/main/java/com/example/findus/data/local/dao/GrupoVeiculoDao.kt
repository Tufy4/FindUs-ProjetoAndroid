package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.findus.data.local.entity.GrupoVeiculoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrupoVeiculoDao {
    @Query("SELECT * FROM grupos_veiculos WHERE deletado = 0 ORDER BY nome ASC")
    fun observarTodos(): Flow<List<GrupoVeiculoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(grupo: GrupoVeiculoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(grupos: List<GrupoVeiculoEntity>)
}

package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CentroDistribuicaoDao {
    @Query("SELECT * FROM centros_distribuicao WHERE deletado = 0 ORDER BY nome ASC")
    fun observarTodos(): Flow<List<CentroDistribuicaoEntity>>

    @Query("SELECT * FROM centros_distribuicao WHERE id = :id")
    suspend fun buscarPorId(id: String): CentroDistribuicaoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(centro: CentroDistribuicaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(centros: List<CentroDistribuicaoEntity>)
}

package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.findus.data.local.entity.PerimetroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerimetroDao {
    @Query("SELECT * FROM perimetros WHERE deletado = 0 ORDER BY nome ASC")
    fun observarTodos(): Flow<List<PerimetroEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(perimetro: PerimetroEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(perimetros: List<PerimetroEntity>)
}

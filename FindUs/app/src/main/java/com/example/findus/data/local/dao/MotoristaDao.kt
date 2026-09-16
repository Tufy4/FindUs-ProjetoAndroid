package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.findus.data.local.entity.MotoristaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotoristaDao {
    @Query("SELECT * FROM motoristas WHERE deletado = 0 ORDER BY nome ASC")
    fun observarTodos(): Flow<List<MotoristaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(motorista: MotoristaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(motoristas: List<MotoristaEntity>)
}

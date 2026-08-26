package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.findus.data.local.entity.MotoristaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotoristaDao {
    @Query("SELECT * FROM motoristas ORDER BY nome ASC")
    fun observarTodos(): Flow<List<MotoristaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(motorista: MotoristaEntity): Long

    @Update
    suspend fun atualizar(motorista: MotoristaEntity)

    @Delete
    suspend fun remover(motorista: MotoristaEntity)
}

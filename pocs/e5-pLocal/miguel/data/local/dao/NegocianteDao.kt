package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.findus.data.local.entity.NegocianteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NegocianteDao {
    @Query("SELECT * FROM negociantes ORDER BY nome ASC")
    fun observarTodos(): Flow<List<NegocianteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(negociante: NegocianteEntity): Long

    @Update
    suspend fun atualizar(negociante: NegocianteEntity)

    @Delete
    suspend fun remover(negociante: NegocianteEntity)
}

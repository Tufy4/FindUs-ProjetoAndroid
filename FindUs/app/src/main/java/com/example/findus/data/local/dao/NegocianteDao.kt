package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.findus.data.local.entity.NegocianteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NegocianteDao {
    @Query("SELECT * FROM negociantes WHERE deletado = 0 ORDER BY nome ASC")
    fun observarTodos(): Flow<List<NegocianteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(negociante: NegocianteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(negociantes: List<NegocianteEntity>)
}

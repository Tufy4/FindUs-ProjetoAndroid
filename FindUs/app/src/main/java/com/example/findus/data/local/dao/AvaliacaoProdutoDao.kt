package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.findus.data.local.entity.AvaliacaoProdutoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvaliacaoProdutoDao {
    @Query("SELECT * FROM avaliacoes_produto ORDER BY data DESC")
    fun observarTodas(): Flow<List<AvaliacaoProdutoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(avaliacao: AvaliacaoProdutoEntity): Long

    @Update
    suspend fun atualizar(avaliacao: AvaliacaoProdutoEntity)

    @Delete
    suspend fun remover(avaliacao: AvaliacaoProdutoEntity)
}

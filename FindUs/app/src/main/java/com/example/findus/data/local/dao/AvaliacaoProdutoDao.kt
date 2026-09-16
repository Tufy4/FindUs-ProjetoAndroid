package com.example.findus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.findus.data.local.entity.AvaliacaoProdutoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvaliacaoProdutoDao {
    @Query("SELECT * FROM avaliacoes_produto WHERE deletado = 0 ORDER BY data DESC")
    fun observarTodas(): Flow<List<AvaliacaoProdutoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(avaliacao: AvaliacaoProdutoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodas(avaliacoes: List<AvaliacaoProdutoEntity>)
}

package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "avaliacoes_produto",
    indices = [Index(value = ["produtoId"])]
)
data class AvaliacaoProdutoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val produtoId: String = "",
    val nota: Int = 0,
    val comentario: String? = null,
    val data: Long = System.currentTimeMillis(),
    val deletado: Boolean = false
)

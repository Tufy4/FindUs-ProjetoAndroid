package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "avaliacoes_produto",
    foreignKeys = [
        ForeignKey(
            entity = ProdutoEntity::class,
            parentColumns = ["id"],
            childColumns = ["produtoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["produtoId"])]
)
data class AvaliacaoProdutoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val produtoId: Long,
    val nota: Int,
    val comentario: String? = null,
    val data: Long = System.currentTimeMillis()
)

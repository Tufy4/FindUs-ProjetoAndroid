package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "produtos")
data class ProdutoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "",
    val descricao: String = "",
    val peso: Double = 0.0,
    val categoria: String = "",
    val deletado: Boolean = false
)

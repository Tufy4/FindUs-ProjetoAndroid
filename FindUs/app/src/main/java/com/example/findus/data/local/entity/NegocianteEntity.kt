package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.findus.data.enum.TipoNegociante
import java.util.UUID

@Entity(tableName = "negociantes")
data class NegocianteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "",
    val documento: String = "",
    val tipo: TipoNegociante = TipoNegociante.CLIENTE,
    val endereco: String = "",
    val deletado: Boolean = false
)

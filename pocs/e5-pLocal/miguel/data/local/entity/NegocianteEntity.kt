package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.findus.data.enum.TipoNegociante

@Entity(tableName = "negociantes")
data class NegocianteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val documento: String,
    val tipo: TipoNegociante,
    val endereco: String
)

package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Conjunto de veículos que pode ser alvo de um perímetro de geofencing. */
@Entity(tableName = "grupos_veiculos")
data class GrupoVeiculoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "",
    val veiculoIds: List<String> = emptyList(),
    val deletado: Boolean = false
)

package com.example.e3sync.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "veiculos")
data class VeiculoEntity(
    @PrimaryKey val id: String,
    val placa: String,
    val modelo: String,
    val motorista: String,
    val atualizadoEm: Long,
    val pendenteSync: Boolean = true,
    val deletado: Boolean = false
)

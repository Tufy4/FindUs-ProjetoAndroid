package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "motoristas",
    indices = [
        Index(value = ["veiculoId"]),
        Index(value = ["cnh"])
    ]
)
data class MotoristaEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    // uid da conta de login (Firebase Auth) criada para este motorista pelo controlador
    val uid: String = "",
    val nome: String = "",
    val cnh: String = "",
    val telefone: String = "",
    val veiculoId: String? = null,
    val deletado: Boolean = false
)

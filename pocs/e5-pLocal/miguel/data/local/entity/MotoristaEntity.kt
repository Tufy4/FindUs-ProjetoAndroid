package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "motoristas",
    foreignKeys = [
        ForeignKey(
            entity = VeiculoEntity::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["veiculoId"]),
        Index(value = ["cnh"], unique = true)
    ]
)
data class MotoristaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val cnh: String,
    val telefone: String,
    val veiculoId: Long? = null
)

package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.findus.data.enum.EstadoPortas

@Entity(
    tableName = "registros_telemetria",
    foreignKeys = [
        ForeignKey(
            entity = VeiculoEntity::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["veiculoId"]),
        Index(value = ["timestamp"])
    ]
)
data class RegistroTelemetriaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val veiculoId: Long,
    val latitude: Double,
    val longitude: Double,
    val velocidade: Double,
    val estadoPortas: EstadoPortas,
    val motorLigado: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

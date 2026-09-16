package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.findus.data.enum.EstadoPortas
import java.util.UUID

@Entity(
    tableName = "registros_telemetria",
    indices = [
        Index(value = ["veiculoId"]),
        Index(value = ["timestamp"])
    ]
)
data class RegistroTelemetriaEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val veiculoId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val velocidade: Double = 0.0,
    val estadoPortas: EstadoPortas = EstadoPortas.FECHADA,
    val motorLigado: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

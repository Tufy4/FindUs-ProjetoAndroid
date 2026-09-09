package com.example.findus.telemetry

import com.example.findus.data.enum.EstadoPortas

data class PontoRota(
    val latitude: Double,
    val longitude: Double,
    val velocidade: Double,
    val estadoPortas: EstadoPortas,
    val motorLigado: Boolean
)

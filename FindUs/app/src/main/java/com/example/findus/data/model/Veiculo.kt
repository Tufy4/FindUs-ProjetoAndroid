package com.example.findus.data.model

import com.example.findus.data.enum.StatusVeiculo

data class Veiculo(
    val id: String,
    val nome: String,
    val trajeto: List<PontoSimulado>
)

data class PontoSimulado(
    val latitutde: Double,
    val longitude: Double,
    val status: StatusVeiculo
)


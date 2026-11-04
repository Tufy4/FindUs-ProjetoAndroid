package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Local de onde os veículos saem para as entregas. O raio define o perímetro
 * usado pelo geofencing para avisar quando um veículo sai ou volta ao centro.
 */
@Entity(tableName = "centros_distribuicao")
data class CentroDistribuicaoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "",
    val endereco: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val raioMetros: Double = 500.0,
    val deletado: Boolean = false
)

package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.findus.data.enum.TipoAlvoPerimetro
import java.util.UUID

/**
 * Perímetro de geofencing cadastrado pelo controlador. [alvoId] é o id do veículo
 * (quando [tipoAlvo] é VEICULO) ou do grupo (quando é GRUPO); fica nulo para TODOS.
 */
@Entity(tableName = "perimetros")
data class PerimetroEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String = "",
    val endereco: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val raioMetros: Double = 500.0,
    val tipoAlvo: TipoAlvoPerimetro = TipoAlvoPerimetro.TODOS,
    val alvoId: String? = null,
    val ativo: Boolean = true,
    val deletado: Boolean = false
)

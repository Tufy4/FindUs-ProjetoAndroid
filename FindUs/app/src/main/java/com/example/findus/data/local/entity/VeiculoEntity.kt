package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.enum.TipoVeiculo
import java.util.UUID

@Entity(
    tableName = "veiculos",
    indices = [Index(value = ["placa"])]
)
data class VeiculoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val placa: String = "",
    val modelo: String = "",
    val tipo: TipoVeiculo = TipoVeiculo.CAMINHAO,
    val fotoBase64: String? = null,
    val status: StatusOperacionalVeiculo = StatusOperacionalVeiculo.DISPONIVEL,
    val deletado: Boolean = false
)

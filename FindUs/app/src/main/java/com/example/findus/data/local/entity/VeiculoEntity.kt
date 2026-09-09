package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.enum.TipoVeiculo
import java.util.UUID

@Entity(
    tableName = "veiculos",
    indices = [Index(value = ["placa"], unique = true)]
)
data class VeiculoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val placa: String,
    val modelo: String,
    val tipo: TipoVeiculo,
    val fotoUri: String? = null,
    val status: StatusOperacionalVeiculo = StatusOperacionalVeiculo.DISPONIVEL,
    val syncId: String = UUID.randomUUID().toString(),
    val atualizadoEm: Long = System.currentTimeMillis(),
    val pendenteSync: Boolean = true,
    val deletado: Boolean = false,
    val fotoSincronizada: Boolean = false
)

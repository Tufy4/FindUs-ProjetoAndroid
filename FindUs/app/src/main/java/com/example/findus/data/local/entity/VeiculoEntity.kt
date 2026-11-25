package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.enum.TipoVeiculo
import com.google.firebase.firestore.Exclude
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
    // Só no documento do Firestore (que não tem Storage). No Room fica sempre nulo:
    // a foto local é um arquivo, referenciado por [fotoArquivo]. Veja VeiculoRepository.
    val fotoBase64: String? = null,
    // Nome do arquivo em filesDir/fotos_veiculos. Não vai para o Firestore.
    @get:Exclude
    val fotoArquivo: String? = null,
    val status: StatusOperacionalVeiculo = StatusOperacionalVeiculo.DISPONIVEL,
    val deletado: Boolean = false
)

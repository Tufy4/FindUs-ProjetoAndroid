package com.example.findus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.findus.data.enum.StatusEntrega
import java.util.UUID

/**
 * Entrega atribuída pelo controlador a um motorista (e ao veículo vinculado a ele),
 * saindo de um centro de distribuição até um ponto de entrega.
 */
@Entity(
    tableName = "entregas",
    indices = [
        Index(value = ["motoristaId"]),
        Index(value = ["veiculoId"]),
        Index(value = ["status"])
    ]
)
data class EntregaEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val descricao: String = "",
    val centroDistribuicaoId: String = "",
    val enderecoDestino: String = "",
    val latitudeDestino: Double = 0.0,
    val longitudeDestino: Double = 0.0,
    val motoristaId: String = "",
    val veiculoId: String = "",
    val status: StatusEntrega = StatusEntrega.PENDENTE,
    val criadaEm: Long = System.currentTimeMillis(),
    val iniciadaEm: Long? = null,
    val concluidaEm: Long? = null,
    val deletado: Boolean = false
)

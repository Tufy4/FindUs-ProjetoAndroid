package com.example.findus.data.enum

enum class StatusEntrega { PENDENTE, EM_ROTA, ENTREGUE, CANCELADA }

fun StatusEntrega.rotulo(): String = when (this) {
    StatusEntrega.PENDENTE -> "Pendente"
    StatusEntrega.EM_ROTA -> "Em rota"
    StatusEntrega.ENTREGUE -> "Entregue"
    StatusEntrega.CANCELADA -> "Cancelada"
}

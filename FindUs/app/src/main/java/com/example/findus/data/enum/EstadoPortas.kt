package com.example.findus.data.enum

enum class EstadoPortas { ABERTA, FECHADA }

fun EstadoPortas.rotulo(): String = when (this) {
    EstadoPortas.ABERTA -> "abertas"
    EstadoPortas.FECHADA -> "fechadas"
}

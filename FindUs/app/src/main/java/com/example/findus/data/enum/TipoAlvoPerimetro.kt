package com.example.findus.data.enum

/** Quais veículos um perímetro de geofencing monitora. */
enum class TipoAlvoPerimetro { TODOS, VEICULO, GRUPO }

fun TipoAlvoPerimetro.rotulo(): String = when (this) {
    TipoAlvoPerimetro.TODOS -> "Todos os veículos"
    TipoAlvoPerimetro.VEICULO -> "Veículo específico"
    TipoAlvoPerimetro.GRUPO -> "Grupo de veículos"
}

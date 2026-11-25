package com.example.findus.data.enum

enum class StatusOperacionalVeiculo { DISPONIVEL, EM_ROTA, MANUTENCAO, INATIVO }

fun StatusOperacionalVeiculo.rotulo(): String = when (this) {
    StatusOperacionalVeiculo.DISPONIVEL -> "Disponível"
    StatusOperacionalVeiculo.EM_ROTA -> "Em rota"
    StatusOperacionalVeiculo.MANUTENCAO -> "Em manutenção"
    StatusOperacionalVeiculo.INATIVO -> "Inativo"
}

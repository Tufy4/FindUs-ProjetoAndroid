package com.example.findus.data.enum

enum class TipoVeiculo { CAMINHAO, FURGAO, VAN, MOTO }

fun TipoVeiculo.rotulo(): String = when (this) {
    TipoVeiculo.CAMINHAO -> "Caminhão"
    TipoVeiculo.FURGAO -> "Furgão"
    TipoVeiculo.VAN -> "Van"
    TipoVeiculo.MOTO -> "Moto"
}

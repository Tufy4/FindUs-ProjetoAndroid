package com.example.findus.location

data class Coordenada(val latitude: Double, val longitude: Double)

/** Região de operação usada quando não há posição do aparelho (Araraquara/SP). */
val COORDENADA_PADRAO = Coordenada(-21.77380144085756, -48.1841802686846)

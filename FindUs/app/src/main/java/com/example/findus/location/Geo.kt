package com.example.findus.location

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Distância em metros entre duas coordenadas (fórmula de Haversine). */
fun distanciaMetros(a: Coordenada, b: Coordenada): Double {
    val raioTerra = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val h = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).pow(2)
    return 2 * raioTerra * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

/** Rumo de [a] para [b], em graus no sentido horário a partir do norte (0..360). */
fun rumoGraus(a: Coordenada, b: Coordenada): Double {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
}

/** Ponto entre [a] e [b]; [fracao] 0 devolve [a] e 1 devolve [b]. Suficiente para trechos curtos de rota. */
fun interpolar(a: Coordenada, b: Coordenada, fracao: Double): Coordenada = Coordenada(
    a.latitude + (b.latitude - a.latitude) * fracao,
    a.longitude + (b.longitude - a.longitude) * fracao
)

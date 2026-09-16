package com.example.findus.location

import android.location.Location

enum class TipoEventoGeofence { ENTROU, SAIU }

data class EventoGeofence(val veiculoId: String, val tipo: TipoEventoGeofence, val distanciaMetros: Float)

/**
 * Simula geofencing calculando a distância do veículo até o centro do
 * perímetro e comparando com o raio configurado, sem depender da
 * Geofencing API do Play Services (mais simples de reproduzir em uma PoC).
 * O centro é o ponto de partida da simulação, definido em definirCentro.
 */
class GeofenceMonitor(private val raioMetros: Float = 1000f) {
    private val dentroDoPerimetro = mutableMapOf<String, Boolean>()
    private var centro: Coordenada? = null

    fun definirCentro(coordenada: Coordenada) {
        centro = coordenada
        dentroDoPerimetro.clear()
    }

    fun avaliar(veiculoId: String, latitude: Double, longitude: Double): EventoGeofence? {
        val centroAtual = centro ?: return null
        val resultado = FloatArray(1)
        Location.distanceBetween(centroAtual.latitude, centroAtual.longitude, latitude, longitude, resultado)
        val distancia = resultado[0]
        val estaDentroAgora = distancia <= raioMetros
        val estavaDentroAntes = dentroDoPerimetro[veiculoId]
        dentroDoPerimetro[veiculoId] = estaDentroAgora

        if (estavaDentroAntes == null || estavaDentroAntes == estaDentroAgora) return null
        val tipo = if (estaDentroAgora) TipoEventoGeofence.ENTROU else TipoEventoGeofence.SAIU
        return EventoGeofence(veiculoId, tipo, distancia)
    }
}

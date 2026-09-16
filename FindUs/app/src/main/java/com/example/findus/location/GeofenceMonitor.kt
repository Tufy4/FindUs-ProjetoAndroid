package com.example.findus.location

import android.location.Location

data class Geofence(val centroLat: Double, val centroLon: Double, val raioMetros: Float)

enum class TipoEventoGeofence { ENTROU, SAIU }

data class EventoGeofence(val veiculoId: String, val tipo: TipoEventoGeofence, val distanciaMetros: Float)

/**
 * Simula geofencing calculando a distância do veículo até o centro do
 * perímetro e comparando com o raio configurado, sem depender da
 * Geofencing API do Play Services (mais simples de reproduzir em uma PoC).
 */
class GeofenceMonitor(private val geofence: Geofence) {
    private val dentroDoPerimetro = mutableMapOf<String, Boolean>()

    fun avaliar(veiculoId: String, latitude: Double, longitude: Double): EventoGeofence? {
        val resultado = FloatArray(1)
        Location.distanceBetween(geofence.centroLat, geofence.centroLon, latitude, longitude, resultado)
        val distancia = resultado[0]
        val estaDentroAgora = distancia <= geofence.raioMetros
        val estavaDentroAntes = dentroDoPerimetro[veiculoId]
        dentroDoPerimetro[veiculoId] = estaDentroAgora

        if (estavaDentroAntes == null || estavaDentroAntes == estaDentroAgora) return null
        val tipo = if (estaDentroAgora) TipoEventoGeofence.ENTROU else TipoEventoGeofence.SAIU
        return EventoGeofence(veiculoId, tipo, distancia)
    }
}

package com.example.findus.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

data class Coordenada(val latitude: Double, val longitude: Double)

/** Wrapper enxuto sobre o FusedLocationProviderClient para obter a posição atual. */
class LocationHelper(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun obterLocalizacaoAtual(): Coordenada? {
        val localizacao = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            ?: client.lastLocation.await()
        return localizacao?.let { Coordenada(it.latitude, it.longitude) }
    }
}

package com.example.e7reversegeocoding.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

data class Coordenada(val latitude: Double, val longitude: Double)

/** Posição atual do aparelho via FusedLocationProvider. Devolve null sem permissão ou sem sinal. */
class LocalizacaoAtual(context: Context) {
    private val cliente = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun obter(): Coordenada? = runCatching {
        val local = cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            ?: cliente.lastLocation.await()
        local?.let { Coordenada(it.latitude, it.longitude) }
    }.getOrNull()
}

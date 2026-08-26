package com.example.findus.location

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Resolve coordenadas em um endereço legível (Reverse Geocoding). */
class ReverseGeocoder(context: Context) {
    private val geocoder = Geocoder(context, Locale("pt", "BR"))

    suspend fun endereco(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
        }.getOrNull()
    }
}

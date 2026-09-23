package com.example.e7reversegeocoding.location

import java.util.Locale

/**
 * Resolve coordenadas em endereço (Reverse Geocoding).
 *
 * Tenta o provedor principal (API Geoapify) e cai para o reserva (Geocoder do Android)
 * quando não há chave, rede ou resultado. Resultados ficam em cache por coordenada
 * arredondada em 4 casas (~11 m), o que evita repetir chamadas para o mesmo veículo parado.
 */
class ReverseGeocoder(
    private val principal: ProvedorEndereco,
    private val reserva: ProvedorEndereco? = null,
    private val tamanhoCache: Int = 200
) {
    private val cache = object : LinkedHashMap<String, Endereco>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Endereco>?): Boolean =
            size > tamanhoCache
    }

    suspend fun buscar(latitude: Double, longitude: Double): Endereco? {
        val chave = chaveCache(latitude, longitude)
        synchronized(cache) { cache[chave] }?.let { return it }

        val endereco = principal.buscar(latitude, longitude) ?: reserva?.buscar(latitude, longitude)
        if (endereco != null) synchronized(cache) { cache[chave] = endereco }
        return endereco
    }

    suspend fun buscar(coordenada: Coordenada): Endereco? = buscar(coordenada.latitude, coordenada.longitude)

    /** Só na PoC: permite à tela mostrar se a resposta veio do cache. */
    fun emCache(latitude: Double, longitude: Double): Boolean =
        synchronized(cache) { cache.containsKey(chaveCache(latitude, longitude)) }
}

internal fun chaveCache(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.4f,%.4f", latitude, longitude)

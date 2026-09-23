package com.example.e7reversegeocoding.location

enum class FonteEndereco(val rotulo: String) {
    GEOAPIFY("API Geoapify"),
    GEOCODER_ANDROID("Geocoder do Android")
}

/** Endereço estruturado resolvido a partir de uma coordenada. */
data class Endereco(
    val logradouro: String? = null,
    val numero: String? = null,
    val bairro: String? = null,
    val cidade: String? = null,
    val estado: String? = null,
    val cep: String? = null,
    val formatado: String,
    val fonte: FonteEndereco
) {
    /** "Rua X, 123 - Bairro", caindo para o texto completo quando faltam partes. */
    val linhaCurta: String
        get() {
            val rua = listOfNotNull(logradouro, numero).joinToString(", ")
            return listOf(rua, bairro.orEmpty())
                .filter { it.isNotBlank() }
                .joinToString(" - ")
                .ifBlank { formatado }
        }
}

/** Contrato comum aos provedores de reverse geocoding. */
interface ProvedorEndereco {
    suspend fun buscar(latitude: Double, longitude: Double): Endereco?
}

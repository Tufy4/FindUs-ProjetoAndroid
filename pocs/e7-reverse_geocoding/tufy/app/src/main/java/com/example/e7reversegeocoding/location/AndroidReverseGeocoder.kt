package com.example.e7reversegeocoding.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Reverse geocoding nativo (Geocoder do Android). Usado como reserva: não precisa de chave,
 * mas depende do backend do fabricante/Play Services e pode não existir em alguns emuladores.
 */
class AndroidReverseGeocoder(context: Context) : ProvedorEndereco {
    private val geocoder = Geocoder(context, Locale("pt", "BR"))

    override suspend fun buscar(latitude: Double, longitude: Double): Endereco? {
        if (!Geocoder.isPresent()) return null
        val endereco = runCatching { consultar(latitude, longitude) }.getOrNull() ?: return null
        return endereco.paraEndereco()
    }

    private suspend fun consultar(latitude: Double, longitude: Double): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuacao ->
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(enderecos: MutableList<Address>) {
                        continuacao.resume(enderecos.firstOrNull())
                    }

                    override fun onError(mensagem: String?) {
                        continuacao.resume(null)
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        }
}

private fun Address.paraEndereco(): Endereco? {
    val formatado = getAddressLine(0) ?: return null
    return Endereco(
        logradouro = thoroughfare,
        numero = subThoroughfare,
        bairro = subLocality,
        cidade = locality ?: subAdminArea,
        estado = adminArea,
        cep = postalCode,
        formatado = formatado,
        fonte = FonteEndereco.GEOCODER_ANDROID
    )
}

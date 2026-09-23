package com.example.e7reversegeocoding.location

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReverseGeocoderTest {

    private class ProvedorFalso(private val resposta: Endereco?) : ProvedorEndereco {
        var chamadas = 0
        override suspend fun buscar(latitude: Double, longitude: Double): Endereco? {
            chamadas++
            return resposta
        }
    }

    private val enderecoApi = Endereco(formatado = "Rua A, 1", fonte = FonteEndereco.GEOAPIFY)
    private val enderecoNativo = Endereco(formatado = "Rua A, 1", fonte = FonteEndereco.GEOCODER_ANDROID)

    @Test
    fun `usa o provedor principal quando ele responde`() = runBlocking {
        val reserva = ProvedorFalso(enderecoNativo)
        val geocoder = ReverseGeocoder(ProvedorFalso(enderecoApi), reserva)

        assertEquals(FonteEndereco.GEOAPIFY, geocoder.buscar(-21.77, -48.18)?.fonte)
        assertEquals(0, reserva.chamadas)
    }

    @Test
    fun `cai para a reserva quando o principal falha`() = runBlocking {
        val geocoder = ReverseGeocoder(ProvedorFalso(null), ProvedorFalso(enderecoNativo))

        assertEquals(FonteEndereco.GEOCODER_ANDROID, geocoder.buscar(-21.77, -48.18)?.fonte)
    }

    @Test
    fun `devolve null quando nenhum provedor encontra`() = runBlocking {
        val geocoder = ReverseGeocoder(ProvedorFalso(null), ProvedorFalso(null))

        assertNull(geocoder.buscar(-21.77, -48.18))
    }

    @Test
    fun `pontos a poucos metros reaproveitam o cache`() = runBlocking {
        val principal = ProvedorFalso(enderecoApi)
        val geocoder = ReverseGeocoder(principal)

        geocoder.buscar(-21.773801, -48.184180)
        geocoder.buscar(-21.773809, -48.184188)

        assertEquals(1, principal.chamadas)
    }

    @Test
    fun `emCache so fica verdadeiro depois de uma consulta com resultado`() = runBlocking {
        val geocoder = ReverseGeocoder(ProvedorFalso(enderecoApi))

        assertEquals(false, geocoder.emCache(-21.77, -48.18))
        geocoder.buscar(-21.77, -48.18)
        assertEquals(true, geocoder.emCache(-21.77, -48.18))
    }

    @Test
    fun `falha nao entra no cache`() = runBlocking {
        val principal = ProvedorFalso(null)
        val geocoder = ReverseGeocoder(principal)

        geocoder.buscar(-21.77, -48.18)
        geocoder.buscar(-21.77, -48.18)

        assertEquals(2, principal.chamadas)
    }

    @Test
    fun `chave de cache arredonda em 4 casas e ignora o locale`() {
        assertEquals("-21.7738,-48.1842", chaveCache(-21.77380144, -48.18418026))
    }
}

package com.example.e7reversegeocoding.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoapifyParserTest {

    @Test
    fun `extrai os campos de um resultado brasileiro`() {
        val json = """
            {"results":[{
              "street":"Avenida Bandeirantes","housenumber":"1500","suburb":"Vila Xavier",
              "city":"Araraquara","state":"São Paulo","state_code":"SP","postcode":"14810-000",
              "country_code":"br",
              "formatted":"Avenida Bandeirantes, 1500 - Vila Xavier, Araraquara - SP, 14810-000, Brasil"
            }]}
        """.trimIndent()

        val endereco = parseRespostaGeoapify(json)!!

        assertEquals("Avenida Bandeirantes", endereco.logradouro)
        assertEquals("1500", endereco.numero)
        assertEquals("Vila Xavier", endereco.bairro)
        assertEquals("Araraquara", endereco.cidade)
        assertEquals("SP", endereco.estado)
        assertEquals("14810-000", endereco.cep)
        assertEquals("Avenida Bandeirantes, 1500 - Vila Xavier", endereco.linhaCurta)
        assertEquals(FonteEndereco.GEOAPIFY, endereco.fonte)
    }

    @Test
    fun `campos ausentes viram null e linha curta usa o formatado`() {
        val json = """{"results":[{"city":"Araraquara","formatted":"Araraquara - SP, Brasil"}]}"""

        val endereco = parseRespostaGeoapify(json)!!

        assertNull(endereco.logradouro)
        assertNull(endereco.cep)
        assertEquals("Araraquara - SP, Brasil", endereco.linhaCurta)
    }

    @Test
    fun `sem resultados devolve null`() {
        assertNull(parseRespostaGeoapify("""{"results":[]}"""))
    }
}

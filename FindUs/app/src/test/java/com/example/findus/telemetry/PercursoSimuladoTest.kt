package com.example.findus.telemetry

import com.example.findus.location.Coordenada
import com.example.findus.location.PassoRota
import com.example.findus.location.RotaDetalhada
import com.example.findus.location.distanciaMetros
import com.example.findus.location.rotaEmLinhaReta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PercursoSimuladoTest {
    private val origem = Coordenada(-21.7738, -48.1842)

    // ~1,1 km para o norte e depois ~1 km para o leste, com uma manobra na esquina.
    private val esquina = Coordenada(-21.7638, -48.1842)
    private val destino = Coordenada(-21.7638, -48.1745)

    private fun rotaEmL(): RotaDetalhada {
        val pontos = listOf(origem, Coordenada(-21.7688, -48.1842), esquina, esquina, destino)
        val trecho1 = distanciaMetros(origem, esquina)
        val trecho2 = distanciaMetros(esquina, destino)
        return RotaDetalhada(
            pontos = pontos,
            distanciaMetros = trecho1 + trecho2,
            tempoSegundos = trecho1 / 12.5 + trecho2 / 10.0,
            passos = listOf(
                PassoRota(0, 2, trecho1, trecho1 / 12.5, "Siga para o norte na Rua A"),
                PassoRota(3, 4, trecho2, trecho2 / 10.0, "Vire à direita na Rua B"),
                PassoRota(4, 4, 0.0, 0.0, "Você chegou ao destino")
            ),
            calculadaPelaApi = true
        )
    }

    private fun simularAteChegar(percurso: PercursoSimulado, dt: Double = 0.5): List<EstadoPercurso> {
        val estados = mutableListOf(EstadoPercurso())
        while (!estados.last().chegou && estados.size < 100_000) {
            estados.add(percurso.avancar(estados.last(), dt))
        }
        return estados
    }

    @Test
    fun percorreRotaInteiraEChegaParado() {
        val percurso = PercursoSimulado(rotaEmL())
        val estados = simularAteChegar(percurso)
        val final = estados.last()
        assertTrue(final.chegou)
        assertEquals(percurso.distanciaTotal, final.distanciaPercorrida, 0.001)
        assertEquals(0.0, final.velocidadeKmh, 0.0)
        assertEquals(destino.latitude, percurso.posicaoEm(final.distanciaPercorrida).latitude, 1e-9)
        assertEquals(destino.longitude, percurso.posicaoEm(final.distanciaPercorrida).longitude, 1e-9)
    }

    @Test
    fun velocidadeInformadaEhCoerenteComODeslocamento() {
        val percurso = PercursoSimulado(rotaEmL())
        val dt = 0.5
        val estados = simularAteChegar(percurso, dt)
        estados.zipWithNext().filter { !it.second.chegou }.forEach { (antes, depois) ->
            val deslocamento = depois.distanciaPercorrida - antes.distanciaPercorrida
            assertEquals(depois.velocidadeKmh / 3.6 * dt, deslocamento, 1e-6)
            assertTrue(depois.velocidadeKmh <= PercursoSimulado.VELOCIDADE_MAXIMA_KMH + 1e-6)
        }
    }

    @Test
    fun reduzAntesDaManobra() {
        val percurso = PercursoSimulado(rotaEmL())
        val naEsquina = distanciaMetros(origem, esquina)
        assertTrue(percurso.velocidadeAlvoKmh(naEsquina - 5.0) < percurso.velocidadeAlvoKmh(naEsquina - 300.0))
    }

    @Test
    fun informaProximaManobraEDistancia() {
        val percurso = PercursoSimulado(rotaEmL())
        val naEsquina = distanciaMetros(origem, esquina)

        val longe = percurso.proximaManobra(100.0)
        assertNotNull(longe)
        assertEquals("Vire à direita na Rua B", longe!!.instrucao)
        assertEquals(naEsquina - 100.0, longe.distanciaMetros, 0.5)
        assertEquals("Siga para o norte na Rua A", percurso.instrucaoAtual(100.0))

        val depoisDaEsquina = percurso.proximaManobra(naEsquina + 50.0)
        assertEquals("Você chegou ao destino", depoisDaEsquina!!.instrucao)
        assertNull(percurso.proximaManobra(percurso.distanciaTotal))
    }

    @Test
    fun rumoAcompanhaADirecaoDaRota() {
        val percurso = PercursoSimulado(rotaEmL())
        val naEsquina = distanciaMetros(origem, esquina)
        assertEquals(0.0, percurso.rumoEm(100.0), 1.0)
        assertEquals(90.0, percurso.rumoEm(naEsquina + 100.0), 1.0)
        // Exatamente no ponto repetido (segmento de comprimento zero) não pode dar NaN.
        assertFalse(percurso.rumoEm(naEsquina).isNaN())
    }

    @Test
    fun tempoRestanteDiminuiEZeraNaChegada() {
        val percurso = PercursoSimulado(rotaEmL())
        val inicio = percurso.tempoRestanteSegundos(0.0)
        assertEquals(rotaEmL().tempoSegundos, inicio, 1.0)
        assertTrue(percurso.tempoRestanteSegundos(800.0) < inicio)
        assertEquals(0.0, percurso.tempoRestanteSegundos(percurso.distanciaTotal), 1e-6)
    }

    @Test
    fun linhaRetaFuncionaSemApi() {
        val rota = rotaEmLinhaReta(origem, destino)
        val percurso = PercursoSimulado(rota)
        assertFalse(rota.calculadaPelaApi)
        assertEquals(distanciaMetros(origem, destino), percurso.distanciaTotal, 0.001)
        assertEquals("Você chegou ao destino", percurso.proximaManobra(10.0)!!.instrucao)
        assertTrue(simularAteChegar(percurso).last().chegou)
    }

    @Test
    fun dividirSeparaTrechoPercorridoDoRestante() {
        val percurso = PercursoSimulado(rotaEmL())
        val (percorrida, restante) = percurso.dividirEm(700.0)
        assertEquals(percorrida.last(), restante.first())
        assertEquals(origem, percorrida.first())
        assertEquals(destino, restante.last())
    }
}

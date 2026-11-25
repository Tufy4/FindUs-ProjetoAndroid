package com.example.findus.telemetry

import com.example.findus.location.Coordenada
import com.example.findus.location.RotaDetalhada
import com.example.findus.location.VELOCIDADE_MEDIA_PADRAO_KMH
import com.example.findus.location.distanciaMetros
import com.example.findus.location.interpolar
import com.example.findus.location.rumoGraus
import kotlin.math.max
import kotlin.math.min

/** Onde o veículo está no percurso e a que velocidade. */
data class EstadoPercurso(
    val distanciaPercorrida: Double = 0.0,
    val velocidadeKmh: Double = 0.0,
    val tempoDecorridoSegundos: Double = 0.0,
    val chegou: Boolean = false
)

/** Próxima manobra que o motorista vai fazer e a quantos metros ela está. */
data class Manobra(val instrucao: String, val distanciaMetros: Double)

/**
 * Motor da simulação do percurso. Move o veículo ao longo da rota pela distância
 * percorrida (e não pelo índice dos pontos), com aceleração e frenagem, reduzindo
 * antes das manobras e ao chegar. A velocidade de cruzeiro de cada trecho vem do
 * tempo estimado pela Geoapify, então a telemetria fica coerente com o deslocamento.
 *
 * Não depende do Android, para poder ser testado com JUnit puro.
 */
class PercursoSimulado(val rota: RotaDetalhada) {
    val pontos: List<Coordenada> = when {
        rota.pontos.size >= 2 -> rota.pontos
        rota.pontos.size == 1 -> listOf(rota.pontos[0], rota.pontos[0])
        else -> throw IllegalArgumentException("Rota sem pontos")
    }

    /** Distância acumulada do início da rota até cada ponto. */
    private val acumulado: DoubleArray = DoubleArray(pontos.size).also { distancias ->
        for (i in 1 until pontos.size) {
            distancias[i] = distancias[i - 1] + distanciaMetros(pontos[i - 1], pontos[i])
        }
    }

    val distanciaTotal: Double = acumulado.last()

    private val passos = rota.passos
    private val inicioDoPasso = DoubleArray(passos.size) { acumulado[passos[it].indiceInicio.coerceIn(0, pontos.lastIndex)] }
    private val fimDoPasso = DoubleArray(passos.size) { acumulado[passos[it].indiceFim.coerceIn(0, pontos.lastIndex)] }

    /** Índice do segmento (entre pontos[i] e pontos[i + 1]) em que a distância [d] cai. */
    private fun segmentoEm(d: Double): Int {
        val alvo = d.coerceIn(0.0, distanciaTotal)
        var baixo = 0
        var alto = pontos.size - 2
        while (baixo < alto) {
            val meio = (baixo + alto + 1) / 2
            if (acumulado[meio] <= alvo) baixo = meio else alto = meio - 1
        }
        return baixo
    }

    fun posicaoEm(d: Double): Coordenada {
        val i = segmentoEm(d)
        val comprimento = acumulado[i + 1] - acumulado[i]
        if (comprimento <= 0.0) return pontos[i + 1]
        val fracao = ((d.coerceIn(0.0, distanciaTotal) - acumulado[i]) / comprimento).coerceIn(0.0, 1.0)
        return interpolar(pontos[i], pontos[i + 1], fracao)
    }

    /** Rumo do veículo em graus (0 = norte, sentido horário). Ignora segmentos de comprimento zero. */
    fun rumoEm(d: Double): Double {
        var i = segmentoEm(d)
        while (i < pontos.size - 2 && acumulado[i + 1] - acumulado[i] <= 0.0) i++
        while (i > 0 && acumulado[i + 1] - acumulado[i] <= 0.0) i--
        return rumoGraus(pontos[i], pontos[i + 1])
    }

    /** Passo (trecho com instrução) em que o veículo está, ou -1 quando a rota não tem passos. */
    fun indicePassoEm(d: Double): Int {
        if (passos.isEmpty()) return -1
        var atual = 0
        for (j in passos.indices) {
            val trechoVazio = fimDoPasso[j] - inicioDoPasso[j] <= 0.0 && j != passos.lastIndex
            if (inicioDoPasso[j] <= d + 1e-6 && !trechoVazio) atual = j
        }
        return atual
    }

    fun instrucaoAtual(d: Double): String? = indicePassoEm(d).takeIf { it >= 0 }?.let { passos[it].instrucao }

    fun proximaManobra(d: Double): Manobra? {
        val atual = indicePassoEm(d)
        if (atual < 0 || atual >= passos.lastIndex) return null
        val proximo = atual + 1
        return Manobra(passos[proximo].instrucao, max(0.0, inicioDoPasso[proximo] - d))
    }

    /** Velocidade de cruzeiro do trecho: média estimada pela API, limitada a uma faixa urbana plausível. */
    private fun velocidadeDeCruzeiroKmh(d: Double): Double {
        val j = indicePassoEm(d)
        if (j < 0) return VELOCIDADE_MEDIA_PADRAO_KMH
        val passo = passos[j]
        if (passo.tempoSegundos <= 0.0 || passo.distanciaMetros <= 0.0) return VELOCIDADE_MEDIA_PADRAO_KMH
        return (passo.distanciaMetros / passo.tempoSegundos * 3.6).coerceIn(VELOCIDADE_MINIMA_KMH, VELOCIDADE_MAXIMA_KMH)
    }

    fun velocidadeAlvoKmh(d: Double): Double {
        var alvo = velocidadeDeCruzeiroKmh(d)
        proximaManobra(d)?.let { manobra ->
            if (manobra.distanciaMetros < ZONA_MANOBRA_METROS) alvo = min(alvo, 20.0 + manobra.distanciaMetros * 0.5)
        }
        val restante = distanciaTotal - d
        if (restante < ZONA_CHEGADA_METROS) alvo = min(alvo, 6.0 + restante * 0.4)
        return alvo
    }

    /** Avança a simulação em [dtSegundos] de tempo simulado. */
    fun avancar(estado: EstadoPercurso, dtSegundos: Double): EstadoPercurso {
        if (estado.chegou || dtSegundos <= 0.0) return estado
        val alvo = velocidadeAlvoKmh(estado.distanciaPercorrida) / 3.6
        val atual = estado.velocidadeKmh / 3.6
        val nova = if (atual < alvo) min(alvo, atual + ACELERACAO * dtSegundos) else max(alvo, atual - FRENAGEM * dtSegundos)
        // Garante que o veículo sempre avança, mesmo saindo do zero.
        val velocidade = max(nova, VELOCIDADE_MINIMA_MOVIMENTO)
        val distancia = estado.distanciaPercorrida + velocidade * dtSegundos
        val tempo = estado.tempoDecorridoSegundos + dtSegundos
        return if (distancia >= distanciaTotal) {
            EstadoPercurso(distanciaTotal, 0.0, tempo, chegou = true)
        } else {
            EstadoPercurso(distancia, velocidade * 3.6, tempo, chegou = false)
        }
    }

    /** Tempo restante estimado, proporcional ao tempo de cada trecho estimado pela API. */
    fun tempoRestanteSegundos(d: Double): Double {
        val restante = max(0.0, distanciaTotal - d)
        val j = indicePassoEm(d)
        if (j < 0 || rota.tempoSegundos <= 0.0) return restante / (VELOCIDADE_MEDIA_PADRAO_KMH / 3.6)
        var tempo = 0.0
        for (k in j + 1 until passos.size) tempo += passos[k].tempoSegundos
        val comprimento = fimDoPasso[j] - inicioDoPasso[j]
        if (comprimento > 0.0) {
            val fracaoRestante = ((fimDoPasso[j] - d) / comprimento).coerceIn(0.0, 1.0)
            tempo += passos[j].tempoSegundos * fracaoRestante
        }
        return tempo
    }

    fun progresso(d: Double): Float =
        if (distanciaTotal <= 0.0) 1f else (d / distanciaTotal).coerceIn(0.0, 1.0).toFloat()

    /** Parte da rota já percorrida e parte que falta, para desenhar no mapa com cores diferentes. */
    fun dividirEm(d: Double): Pair<List<Coordenada>, List<Coordenada>> {
        val i = segmentoEm(d)
        val atual = posicaoEm(d)
        val percorrida = pontos.subList(0, i + 1) + atual
        val restante = listOf(atual) + pontos.subList(i + 1, pontos.size)
        return percorrida to restante
    }

    companion object {
        const val VELOCIDADE_MINIMA_KMH = 20.0
        const val VELOCIDADE_MAXIMA_KMH = 80.0
        const val ZONA_MANOBRA_METROS = 60.0
        const val ZONA_CHEGADA_METROS = 100.0
        private const val ACELERACAO = 1.5 // m/s²
        private const val FRENAGEM = 2.5 // m/s²
        private const val VELOCIDADE_MINIMA_MOVIMENTO = 0.8 // m/s
    }
}

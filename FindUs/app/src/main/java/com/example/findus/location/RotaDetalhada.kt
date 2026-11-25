package com.example.findus.location

/**
 * Um trecho da rota com a manobra que o motorista faz no início dele
 * (ex.: "Vire à direita na Rua 9 de Julho"). Os índices apontam para [RotaDetalhada.pontos].
 */
data class PassoRota(
    val indiceInicio: Int,
    val indiceFim: Int,
    val distanciaMetros: Double,
    val tempoSegundos: Double,
    val instrucao: String
)

/**
 * Rota com geometria e instruções passo a passo. [calculadaPelaApi] é falso quando a
 * Geoapify não respondeu (sem chave ou sem internet) e a rota é uma linha reta.
 */
data class RotaDetalhada(
    val pontos: List<Coordenada>,
    val distanciaMetros: Double,
    val tempoSegundos: Double,
    val passos: List<PassoRota>,
    val calculadaPelaApi: Boolean
)

/** Velocidade média assumida quando não há tempo estimado pela API. */
const val VELOCIDADE_MEDIA_PADRAO_KMH = 40.0

/** Rota de reserva: linha reta da origem ao destino, com uma instrução de saída e uma de chegada. */
fun rotaEmLinhaReta(origem: Coordenada, destino: Coordenada): RotaDetalhada {
    val distancia = distanciaMetros(origem, destino)
    val tempo = distancia / (VELOCIDADE_MEDIA_PADRAO_KMH / 3.6)
    return RotaDetalhada(
        pontos = listOf(origem, destino),
        distanciaMetros = distancia,
        tempoSegundos = tempo,
        passos = listOf(
            PassoRota(0, 1, distancia, tempo, "Siga em direção ao destino"),
            PassoRota(1, 1, 0.0, 0.0, "Você chegou ao destino")
        ),
        calculadaPelaApi = false
    )
}

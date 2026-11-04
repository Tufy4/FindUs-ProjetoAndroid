package com.example.findus.location

import com.example.findus.data.enum.TipoAlvoPerimetro
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.local.entity.GrupoVeiculoEntity
import com.example.findus.data.local.entity.PerimetroEntity
import com.example.findus.domain.RegrasNegocio
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class TipoEventoGeofence { ENTROU, SAIU }

/** De onde veio o perímetro: usado para escolher a cor no mapa e o texto do aviso. */
enum class OrigemPerimetro { CENTRO_DISTRIBUICAO, PONTO_ENTREGA, PERSONALIZADO }

/**
 * Perímetro pronto para ser avaliado. [veiculoIds] nulo significa que o perímetro
 * vale para todos os veículos da frota.
 */
data class PerimetroMonitorado(
    val chave: String,
    val nome: String,
    val origem: OrigemPerimetro,
    val centro: Coordenada,
    val raioMetros: Double,
    val veiculoIds: Set<String>? = null
)

data class EventoGeofence(
    val veiculoId: String,
    val chavePerimetro: String,
    val nomePerimetro: String,
    val tipo: TipoEventoGeofence,
    val distanciaMetros: Float,
    val placa: String = ""
) {
    fun mensagem(): String {
        val veiculo = placa.ifBlank { veiculoId }
        val acao = if (tipo == TipoEventoGeofence.ENTROU) "entrou em" else "saiu de"
        return "Veículo $veiculo $acao: $nomePerimetro"
    }
}

/**
 * Simula geofencing calculando a distância de cada veículo até o centro de cada
 * perímetro e comparando com o raio, sem depender da Geofencing API do Play Services.
 * Guarda o último estado (dentro/fora) por perímetro e veículo e só gera evento
 * quando esse estado muda. A primeira avaliação apenas registra o estado inicial.
 */
class GeofenceMonitor {
    private val dentroDoPerimetro = mutableMapOf<Pair<String, String>, Boolean>()

    fun avaliar(veiculoId: String, posicao: Coordenada, perimetros: List<PerimetroMonitorado>): List<EventoGeofence> {
        val eventos = mutableListOf<EventoGeofence>()
        for (perimetro in perimetros) {
            val alvos = perimetro.veiculoIds
            if (alvos != null && veiculoId !in alvos) continue

            val distancia = distanciaMetros(perimetro.centro, posicao)
            val estaDentroAgora = distancia <= perimetro.raioMetros
            val estavaDentroAntes = dentroDoPerimetro.put(perimetro.chave to veiculoId, estaDentroAgora)

            if (estavaDentroAntes != null && estavaDentroAntes != estaDentroAgora) {
                eventos.add(
                    EventoGeofence(
                        veiculoId = veiculoId,
                        chavePerimetro = perimetro.chave,
                        nomePerimetro = perimetro.nome,
                        tipo = if (estaDentroAgora) TipoEventoGeofence.ENTROU else TipoEventoGeofence.SAIU,
                        distanciaMetros = distancia.toFloat()
                    )
                )
            }
        }
        return eventos
    }
}

/**
 * Junta tudo o que deve ser monitorado:
 * - cada centro de distribuição, para todos os veículos;
 * - cada ponto de entrega pendente ou em rota, só para o veículo daquela entrega;
 * - cada perímetro cadastrado e ativo, para todos, um veículo ou um grupo.
 */
fun montarPerimetros(
    centros: List<CentroDistribuicaoEntity>,
    entregas: List<EntregaEntity>,
    perimetros: List<PerimetroEntity>,
    grupos: List<GrupoVeiculoEntity>
): List<PerimetroMonitorado> {
    val doCentros = centros.map { centro ->
        PerimetroMonitorado(
            chave = "centro:${centro.id}",
            nome = "Centro de distribuição ${centro.nome}",
            origem = OrigemPerimetro.CENTRO_DISTRIBUICAO,
            centro = Coordenada(centro.latitude, centro.longitude),
            raioMetros = centro.raioMetros
        )
    }

    val dasEntregas = entregas.filter { RegrasNegocio.entregaAtiva(it) }.map { entrega ->
        PerimetroMonitorado(
            chave = "entrega:${entrega.id}",
            nome = "Ponto de entrega ${entrega.descricao}",
            origem = OrigemPerimetro.PONTO_ENTREGA,
            centro = Coordenada(entrega.latitudeDestino, entrega.longitudeDestino),
            raioMetros = RegrasNegocio.RAIO_PONTO_ENTREGA_METROS,
            veiculoIds = setOf(entrega.veiculoId)
        )
    }

    val gruposPorId = grupos.associateBy { it.id }
    val personalizados = perimetros.filter { it.ativo }.mapNotNull { perimetro ->
        val alvos: Set<String>? = when (perimetro.tipoAlvo) {
            TipoAlvoPerimetro.TODOS -> null
            TipoAlvoPerimetro.VEICULO -> setOfNotNull(perimetro.alvoId)
            TipoAlvoPerimetro.GRUPO -> perimetro.alvoId?.let { gruposPorId[it] }?.veiculoIds?.toSet()
                ?: return@mapNotNull null
        }
        PerimetroMonitorado(
            chave = "perimetro:${perimetro.id}",
            nome = perimetro.nome,
            origem = OrigemPerimetro.PERSONALIZADO,
            centro = Coordenada(perimetro.latitude, perimetro.longitude),
            raioMetros = perimetro.raioMetros,
            veiculoIds = alvos
        )
    }

    return doCentros + dasEntregas + personalizados
}

/** Distância em metros entre duas coordenadas (fórmula de Haversine). */
fun distanciaMetros(a: Coordenada, b: Coordenada): Double {
    val raioTerra = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val h = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).pow(2)
    return 2 * raioTerra * asin(sqrt(h))
}

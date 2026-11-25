package com.example.findus.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.findus.BuildConfig
import com.example.findus.location.COORDENADA_PADRAO
import com.example.findus.location.Coordenada
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Marcador no mapa. Com [setaGrausNaTela] preenchido, o marcador é uma seta apontando
 * para esse ângulo da tela (0 = para cima), usada para o veículo na simulação do motorista.
 */
data class MarcadorMapa(
    val posicao: Coordenada,
    val titulo: String,
    val descricao: String = "",
    val corHex: String = "#1976D2",
    val onClick: (() -> Unit)? = null,
    val setaGrausNaTela: Float? = null
)

/** Contorno de um perímetro de geofencing. */
data class CirculoMapa(
    val centro: Coordenada,
    val raioMetros: Double,
    val corHex: String
)

/** Linha com cor e espessura próprias (ex.: trecho já percorrido em cinza). */
data class LinhaMapa(
    val pontos: List<Coordenada>,
    val corHex: String = "#1976D2",
    val larguraPx: Float = 8f
)

private fun pontosDoCirculo(circulo: CirculoMapa): List<GeoPoint> {
    val grausLatitude = circulo.raioMetros / 111_320.0
    val grausLongitude = circulo.raioMetros / (111_320.0 * cos(Math.toRadians(circulo.centro.latitude)))
    return (0..48).map { passo ->
        val angulo = 2 * PI * passo / 48
        GeoPoint(
            circulo.centro.latitude + grausLatitude * sin(angulo),
            circulo.centro.longitude + grausLongitude * cos(angulo)
        )
    }
}

private val geoapifyTiles = XYTileSource(
    "GeoapifyOsmBright2x",
    0,
    20,
    512,
    "@2x.png?apiKey=${BuildConfig.GEOAPIFY_API_KEY}",
    arrayOf("https://maps.geoapify.com/v1/tile/osm-bright/")
)

private fun iconeCirculo(corHex: String) = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(Color.parseColor(corHex))
    setStroke(4, Color.WHITE)
    setSize(40, 40)
}

/** Seta de navegação desenhada já girada: o marcador do osmdroid fica sempre "de pé" na tela. */
private fun iconeSeta(context: Context, corHex: String, grausNaTela: Float): BitmapDrawable {
    val lado = 84
    val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.rotate(grausNaTela, lado / 2f, lado / 2f)
    val seta = Path().apply {
        moveTo(lado / 2f, 8f)
        lineTo(lado - 14f, lado - 10f)
        lineTo(lado / 2f, lado * 0.70f)
        lineTo(14f, lado - 10f)
        close()
    }
    canvas.drawPath(seta, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(corHex); style = Paint.Style.FILL })
    canvas.drawPath(
        seta,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeJoin = Paint.Join.ROUND
        }
    )
    return BitmapDrawable(context.resources, bitmap)
}

/** Estado da câmera entre recomposições (o MapView é o mesmo, só os parâmetros mudam). */
private class EstadoCamera {
    var jaCentralizou = false
    var seguindo = false
    var chaveEnquadrada: Int? = null
}

/**
 * Mapa osmdroid com tiles da Geoapify.
 *
 * - [centro]: centraliza uma única vez, na primeira posição conhecida.
 * - [seguir]: quando preenchido, a câmera acompanha esse ponto a cada atualização
 *   (aplica [zoomAoSeguir] ao começar a seguir; depois o usuário pode ajustar o zoom).
 * - [orientacaoMapa]: rotação do mapa em graus; use -rumo para deixar a direção do veículo para cima.
 * - [enquadrar] + [chaveEnquadramento]: ao mudar a chave, ajusta o zoom para mostrar todos os pontos.
 */
@Composable
fun MapaOsm(
    marcadores: List<MarcadorMapa>,
    modifier: Modifier = Modifier,
    linha: List<Coordenada> = emptyList(),
    linhas: List<List<Coordenada>> = emptyList(),
    centro: Coordenada? = null,
    zoom: Double = 13.0,
    onToqueMapa: ((Coordenada) -> Unit)? = null,
    circulos: List<CirculoMapa> = emptyList(),
    linhasEstilizadas: List<LinhaMapa> = emptyList(),
    seguir: Coordenada? = null,
    zoomAoSeguir: Double = 17.0,
    orientacaoMapa: Float = 0f,
    enquadrar: List<Coordenada> = emptyList(),
    chaveEnquadramento: Int? = null
) {
    val camera = remember { EstadoCamera() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                setTileSource(geoapifyTiles)
                setMultiTouchControls(true)
                controller.setZoom(zoom)
                // Antes começava em São José dos Campos, longe da região usada no resto do app.
                controller.setCenter(GeoPoint(COORDENADA_PADRAO.latitude, COORDENADA_PADRAO.longitude))
            }
        },
        onRelease = { it.onDetach() },
        update = { view ->
            if (!camera.jaCentralizou && centro != null) {
                view.controller.setCenter(GeoPoint(centro.latitude, centro.longitude))
                camera.jaCentralizou = true
            }

            if (chaveEnquadramento != null && chaveEnquadramento != camera.chaveEnquadrada && enquadrar.size >= 2) {
                camera.chaveEnquadrada = chaveEnquadramento
                val caixa = BoundingBox.fromGeoPoints(enquadrar.map { GeoPoint(it.latitude, it.longitude) })
                if (view.width == 0) {
                    view.addOnFirstLayoutListener { _, _, _, _, _ -> view.zoomToBoundingBox(caixa, false, 96) }
                } else {
                    view.zoomToBoundingBox(caixa, false, 96)
                }
            }

            if (seguir != null) {
                if (!camera.seguindo) {
                    view.controller.setZoom(zoomAoSeguir)
                    camera.seguindo = true
                }
                view.controller.setCenter(GeoPoint(seguir.latitude, seguir.longitude))
            } else {
                camera.seguindo = false
            }
            if (view.mapOrientation != orientacaoMapa) view.setMapOrientation(orientacaoMapa)

            view.overlays.clear()

            if (onToqueMapa != null) {
                view.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(ponto: GeoPoint): Boolean {
                        onToqueMapa(Coordenada(ponto.latitude, ponto.longitude))
                        return true
                    }

                    override fun longPressHelper(ponto: GeoPoint) = false
                }))
            }

            circulos.forEach { circulo ->
                val contorno = Polyline(view)
                contorno.setPoints(pontosDoCirculo(circulo))
                contorno.outlinePaint.strokeWidth = 4f
                contorno.outlinePaint.color = Color.parseColor(circulo.corHex)
                view.overlays.add(contorno)
            }

            val todasAsLinhas = (linhas + listOf(linha)).map { LinhaMapa(it) } + linhasEstilizadas
            todasAsLinhas.filter { it.pontos.size >= 2 }.forEach { linhaMapa ->
                val polyline = Polyline(view)
                polyline.setPoints(linhaMapa.pontos.map { GeoPoint(it.latitude, it.longitude) })
                polyline.outlinePaint.strokeWidth = linhaMapa.larguraPx
                polyline.outlinePaint.color = Color.parseColor(linhaMapa.corHex)
                view.overlays.add(polyline)
            }

            marcadores.forEach { marcador ->
                val pino = Marker(view)
                pino.position = GeoPoint(marcador.posicao.latitude, marcador.posicao.longitude)
                pino.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                pino.title = marcador.titulo
                pino.snippet = marcador.descricao
                pino.icon = marcador.setaGrausNaTela
                    ?.let { iconeSeta(view.context, marcador.corHex, it) }
                    ?: iconeCirculo(marcador.corHex)
                pino.setOnMarkerClickListener { _, _ ->
                    marcador.onClick?.invoke()
                    true
                }
                view.overlays.add(pino)
            }

            view.invalidate()
        }
    )
}

package com.example.findus.ui.map

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.findus.BuildConfig
import com.example.findus.location.Coordenada
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class MarcadorMapa(
    val posicao: Coordenada,
    val titulo: String,
    val descricao: String = "",
    val corHex: String = "#1976D2",
    val onClick: (() -> Unit)? = null
)

/** Contorno de um perímetro de geofencing. */
data class CirculoMapa(
    val centro: Coordenada,
    val raioMetros: Double,
    val corHex: String
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

@Composable
fun MapaOsm(
    marcadores: List<MarcadorMapa>,
    modifier: Modifier = Modifier,
    linha: List<Coordenada> = emptyList(),
    linhas: List<List<Coordenada>> = emptyList(),
    centro: Coordenada? = null,
    zoom: Double = 13.0,
    onToqueMapa: ((Coordenada) -> Unit)? = null,
    circulos: List<CirculoMapa> = emptyList()
) {
    val jaCentralizou = remember { booleanArrayOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                setTileSource(geoapifyTiles)
                setMultiTouchControls(true)
                controller.setZoom(zoom)
                controller.setCenter(GeoPoint(-23.2237, -45.8937))
            }
        },
        onRelease = { it.onDetach() },
        update = { view ->
            if (!jaCentralizou[0] && centro != null) {
                view.controller.setCenter(GeoPoint(centro.latitude, centro.longitude))
                jaCentralizou[0] = true
            }

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

            (linhas + listOf(linha)).filter { it.size >= 2 }.forEach { pontos ->
                val polyline = Polyline(view)
                polyline.setPoints(pontos.map { GeoPoint(it.latitude, it.longitude) })
                polyline.outlinePaint.strokeWidth = 8f
                polyline.outlinePaint.color = Color.parseColor("#1976D2")
                view.overlays.add(polyline)
            }

            marcadores.forEach { marcador ->
                val pino = Marker(view)
                pino.position = GeoPoint(marcador.posicao.latitude, marcador.posicao.longitude)
                pino.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                pino.title = marcador.titulo
                pino.snippet = marcador.descricao
                pino.icon = iconeCirculo(marcador.corHex)
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

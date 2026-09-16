package com.example.findus.ui.map

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.findus.BuildConfig
import com.example.findus.location.Coordenada
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

data class MarcadorMapa(
    val posicao: Coordenada,
    val titulo: String,
    val descricao: String = "",
    val corHex: String = "#1976D2",
    val onClick: (() -> Unit)? = null
)

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
    centro: Coordenada? = null,
    zoom: Double = 13.0
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

            if (linha.size >= 2) {
                val polyline = Polyline(view)
                polyline.setPoints(linha.map { GeoPoint(it.latitude, it.longitude) })
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

package com.example.e7reversegeocoding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.e7reversegeocoding.BuildConfig
import com.example.e7reversegeocoding.location.Coordenada
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/** Mesmo ponto de fallback do app principal (região de Araraquara). */
val CENTRO_INICIAL = Coordenada(-21.77380144085756, -48.1841802686846)

// Mesma fonte de tiles do app principal: Geoapify osm-bright, com a chave do local.properties.
private val tilesGeoapify = XYTileSource(
    "GeoapifyOsmBright2x",
    0,
    20,
    512,
    "@2x.png?apiKey=${BuildConfig.GEOAPIFY_API_KEY}",
    arrayOf("https://maps.geoapify.com/v1/tile/osm-bright/")
)

@Composable
fun MapaSelecao(
    selecionado: Coordenada?,
    centralizarEm: Coordenada?,
    onToque: (Coordenada) -> Unit,
    modifier: Modifier = Modifier
) {
    val ultimoCentro = remember { arrayOfNulls<Coordenada>(1) }
    val onToqueAtual by rememberUpdatedState(onToque)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                setTileSource(tilesGeoapify)
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(CENTRO_INICIAL.latitude, CENTRO_INICIAL.longitude))
                overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(ponto: GeoPoint): Boolean {
                        onToqueAtual(Coordenada(ponto.latitude, ponto.longitude))
                        return true
                    }

                    override fun longPressHelper(ponto: GeoPoint) = false
                }))
            }
        },
        onRelease = { it.onDetach() },
        update = { mapa ->
            if (centralizarEm != null && centralizarEm != ultimoCentro[0]) {
                mapa.controller.animateTo(GeoPoint(centralizarEm.latitude, centralizarEm.longitude))
                ultimoCentro[0] = centralizarEm
            }

            mapa.overlays.removeAll { it is Marker }
            selecionado?.let { ponto ->
                mapa.overlays.add(Marker(mapa).apply {
                    position = GeoPoint(ponto.latitude, ponto.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ -> true }
                })
            }
            mapa.invalidate()
        }
    )
}

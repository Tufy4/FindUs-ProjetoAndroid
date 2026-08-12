package com.example.findus.ui.map

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import com.example.findus.data.model.Veiculo
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.maps.MapView
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint

@Composable
fun MapaVeiculos(veiculos: List<Veiculo>) {
    val simuladores = remember { veiculos.map { SimuladorVeiculo(it) } }
    LaunchedEffect(Unit) { simuladores.forEach { launch { it.iniciar() } } }

    AndroidView(factory = { ctx ->
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osm", 0))
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(-23.2, -45.9)) // ex: São José dos Campos
        }
    }, update = { mapView ->
        mapView.overlays.clear()
        simuladores.forEach { sim ->
            val ponto = sim.posicaoAtual.value
            val marker = Marker(mapView).apply {
                position = GeoPoint(ponto.lat, ponto.lon)
                icon = corPorStatus(mapView.context, ponto.status)
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    })
}

fun corPorStatus(context: Any, status: Any): Any {

}

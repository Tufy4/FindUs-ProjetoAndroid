package com.example.e5geolocalizacao.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.e5geolocalizacao.data.VehicleState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


private val cartoVoyagerTileSource = XYTileSource(
    "CartoVoyager",
    0,
    20,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
    )
)


@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel()
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        VehicleMap(
            vehicles = vehicles,
            modifier = Modifier.fillMaxSize()
        )

        TelemetryPanel(
            vehicles = vehicles,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )
    }
}

//Cria um círculo colorido simples como ícone do marcador
private fun circleMarker(colorHex: String, sizePx: Int = 40): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor(colorHex))
        setStroke(4, Color.WHITE)
        setSize(sizePx, sizePx)
    }
}

@Composable
private fun VehicleMap(
    vehicles: List<VehicleState>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Config global do osmdroid (user agent evita bloqueio pelos tiles
    // do OpenStreetMap) e cache em disco.
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            load(context, context.getSharedPreferences("osmdroid_prefs", 0))
        }
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(cartoVoyagerTileSource)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            val start = vehicles.firstOrNull()?.position
            controller.setCenter(
                GeoPoint(start?.latitude ?: -23.5445, start?.longitude ?: -46.3108)
            )
        }
    }

    // Reconstrói os marcadores sempre que a lista de veículos (posição/telemetria) muda.
    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.overlays.clear()

            vehicles.forEach { vehicle ->
                val emTransito = vehicle.telemetry.speedKmh > 0.0
                val marker = Marker(view).apply {
                    position = GeoPoint(vehicle.position.latitude, vehicle.position.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = vehicle.info.label
                    snippet = buildString {
                        append(if (emTransito) "Em trânsito" else "Parado")
                        append(" • ${vehicle.telemetry.speedKmh.toInt()} km/h")
                        append(" • Motor ${vehicle.telemetry.engineState.name.lowercase()}")
                        if (vehicle.telemetry.doorsOpen) append(" • Porta aberta")
                    }
                    icon = circleMarker(if (emTransito) "#2E7D32" else "#C62828")
                }
                view.overlays.add(marker)
            }

            view.invalidate()
        }
    )
}

@Composable
private fun TelemetryPanel(
    vehicles: List<VehicleState>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text(
                text = vehicles.joinToString(separator = "\n") { vehicle ->
                    val status = if (vehicle.telemetry.speedKmh > 0.0) "em trânsito" else "parado"
                    val porta = if (vehicle.telemetry.doorsOpen) "porta aberta" else "porta fechada"
                    "${vehicle.info.label}: $status, ${vehicle.telemetry.speedKmh.toInt()} km/h, " +
                        "motor ${vehicle.telemetry.engineState.name.lowercase()}, $porta"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

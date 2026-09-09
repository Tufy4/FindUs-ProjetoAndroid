package com.example.findus_poc_geolocaiton

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private val GEOAPIFY_KEY = BuildConfig.GEOAPIFY_API_KEY

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LocationScreen()
            }
        }
    }
}

@Composable
fun LocationScreen() {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()

    var localizacao by remember { mutableStateOf<Location?>(null) }
    var cep by remember { mutableStateOf("") }
    var mapa by remember { mutableStateOf<ImageBitmap?>(null) }
    var status by remember { mutableStateOf("") }

    val pedidoPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (!concedida) {
            status = "Permissão negada"
            return@rememberLauncherForActivityResult
        }
        status = "localizando"
        fetchLocalidade(contexto) { encontrada ->
            localizacao = encontrada
            status = ""
            escopo.launch {
                cep = fetchCEP(encontrada.latitude, encontrada.longitude)
                mapa = fetchMapa(encontrada.latitude, encontrada.longitude)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { pedidoPermissao.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Onde estou?")
        }

        localizacao?.let {
            Text("Latitude: ${it.latitude}")
            Text("Longitude: ${it.longitude}")
            Text("CEP: $cep")
        }

        if (status.isNotBlank()) {
            Text(status)
        }

        mapa?.let {
            Image(bitmap = it, contentDescription = "Mapa", modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp))
        }
    }
}

@SuppressLint("MissingPermission")
fun fetchLocalidade(contexto: Context, onFind: (Location) -> Unit) {
    val gerenciador = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val listner = object : LocationListener {
        override fun onLocationChanged(localizacao: Location) {
            gerenciador.removeUpdates(this)
            onFind(localizacao)
        }
    }
    gerenciador.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listner)
}

suspend fun fetchCEP(latitude: Double, longitude: Double): String =
    withContext(Dispatchers.IO) {
        val url = "https://api.geoapify.com/v1/geocode/reverse" +
                "?lat=$latitude&lon=$longitude&format=json&apiKey=$GEOAPIFY_KEY"
        val corpo = URL(url).readText()
        JSONObject(corpo).getJSONArray("results")
            .getJSONObject(0)
            .optString("postcode", "não encontrado")
    }

suspend fun fetchMapa(latitude: Double, longitude: Double): ImageBitmap? =
    withContext(Dispatchers.IO) {
        val url = "https://maps.geoapify.com/v1/staticmap" +
                "?style=osm-bright&width=800&height=600&zoom=18" +
                "&center=lonlat:$longitude,$latitude" +
                "&marker=lonlat:$longitude,$latitude;color:%23ff0000;size:medium" +
                "&apiKey=$GEOAPIFY_KEY"
        URL(url).openStream().use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    }

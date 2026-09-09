package com.example.findus.di

import android.content.Context
import com.example.findus.data.local.AppDatabase
import com.example.findus.data.repository.AvaliacaoProdutoRepository
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.NegocianteRepository
import com.example.findus.data.repository.ProdutoRepository
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.location.GeofenceMonitor
import com.example.findus.location.LocationHelper
import com.example.findus.location.ReverseGeocoder
import com.example.findus.location.Geofence
import com.example.findus.telemetry.RotasSimuladas
import com.example.findus.telemetry.TelemetriaSimuladorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Container manual de dependências (Service Locator) para manter a PoC
 * enxuta, sem introduzir Hilt/Koin. Instanciado uma única vez em
 * [com.example.findus.FindUsApplication].
 */
class AppContainer(context: Context) {
    private val escopoApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AppDatabase.getInstance(context)

    val veiculoRepository = VeiculoRepository(database.veiculoDao())
    val telemetriaRepository = TelemetriaRepository(database.registroTelemetriaDao())
    val produtoRepository = ProdutoRepository(database.produtoDao())
    val negocianteRepository = NegocianteRepository(database.negocianteDao())
    val motoristaRepository = MotoristaRepository(database.motoristaDao())
    val avaliacaoProdutoRepository = AvaliacaoProdutoRepository(database.avaliacaoProdutoDao())

    val telemetriaSimuladorManager = TelemetriaSimuladorManager(
        veiculoRepository = veiculoRepository,
        telemetriaRepository = telemetriaRepository,
        escopo = escopoApp
    )

    val locationHelper = LocationHelper(context)
    val reverseGeocoder = ReverseGeocoder(context)

    // Geofence de exemplo: perímetro de 1.5km ao redor do Centro de Distribuição.
    val geofenceMonitor = GeofenceMonitor(
        Geofence(
            centroLat = RotasSimuladas.centroDistribuicao.first * -1,
            centroLon = RotasSimuladas.centroDistribuicao.second * -1,
            raioMetros = 1500f
        )
    )
}

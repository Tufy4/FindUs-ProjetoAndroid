package com.example.findus.di

import android.content.Context
import com.example.findus.data.local.AppDatabase
import com.example.findus.data.remote.VeiculoRemoteDataSource
import com.example.findus.data.repository.AvaliacaoProdutoRepository
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.NegocianteRepository
import com.example.findus.data.repository.ProdutoRepository
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.data.sync.VeiculoSync
import com.example.findus.location.GeofenceMonitor
import com.example.findus.location.GeofenceNotificador
import com.example.findus.location.LocationHelper
import com.example.findus.location.ReverseGeocoder
import com.example.findus.location.RotaService
import com.example.findus.location.Geofence
import com.example.findus.telemetry.RotasSimuladas
import com.example.findus.telemetry.TelemetriaSimuladorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val escopoApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AppDatabase.getInstance(context)

    private val veiculoRemoteDataSource = VeiculoRemoteDataSource()

    private val veiculoSync = VeiculoSync(
        context = context.applicationContext,
        dao = database.veiculoDao(),
        remoto = veiculoRemoteDataSource,
        escopo = escopoApp
    )

    val veiculoRepository = VeiculoRepository(database.veiculoDao(), veiculoSync)
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
    val rotaService = RotaService()

    val geofenceMonitor = GeofenceMonitor(
        Geofence(
            centroLat = RotasSimuladas.centroDistribuicao.first * -1,
            centroLon = RotasSimuladas.centroDistribuicao.second * -1,
            raioMetros = 1500f
        )
    )

    val geofenceNotificador = GeofenceNotificador(context.applicationContext)

    init {
        veiculoSync.iniciar()
        escopoApp.launch {
            telemetriaRepository.observarUltimoPorVeiculo().collect { registros ->
                registros.forEach { registro ->
                    geofenceMonitor.avaliar(registro.veiculoId, registro.latitude, registro.longitude)
                        ?.let { geofenceNotificador.notificar(it) }
                }
            }
        }
    }
}

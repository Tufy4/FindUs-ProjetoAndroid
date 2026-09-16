package com.example.findus.di

import android.content.Context
import com.example.findus.data.local.AppDatabase
import com.example.findus.data.local.entity.AvaliacaoProdutoEntity
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.NegocianteEntity
import com.example.findus.data.local.entity.ProdutoEntity
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.remote.FirestoreColecao
import com.example.findus.data.repository.AvaliacaoProdutoRepository
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.NegocianteRepository
import com.example.findus.data.repository.ProdutoRepository
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.data.sync.sincronizar
import com.example.findus.location.GeofenceMonitor
import com.example.findus.location.GeofenceNotificador
import com.example.findus.location.LocationHelper
import com.example.findus.location.ReverseGeocoder
import com.example.findus.location.RotaService
import com.example.findus.telemetry.TelemetriaSimuladorManager
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val escopoApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AppDatabase.getInstance(context)

    private val veiculosRemoto = FirestoreColecao("veiculos", VeiculoEntity::class.java)
    private val produtosRemoto = FirestoreColecao("produtos", ProdutoEntity::class.java)
    private val negociantesRemoto = FirestoreColecao("negociantes", NegocianteEntity::class.java)
    private val motoristasRemoto = FirestoreColecao("motoristas", MotoristaEntity::class.java)
    private val avaliacoesRemoto = FirestoreColecao("avaliacoes_produto", AvaliacaoProdutoEntity::class.java)
    private val telemetriasRemoto = FirestoreColecao("registros_telemetria", RegistroTelemetriaEntity::class.java) {
        it.orderBy("timestamp", Query.Direction.DESCENDING).limit(100)
    }

    val veiculoRepository = VeiculoRepository(database.veiculoDao(), veiculosRemoto)
    val telemetriaRepository = TelemetriaRepository(database.registroTelemetriaDao(), telemetriasRemoto)
    val produtoRepository = ProdutoRepository(database.produtoDao(), produtosRemoto)
    val negocianteRepository = NegocianteRepository(database.negocianteDao(), negociantesRemoto)
    val motoristaRepository = MotoristaRepository(database.motoristaDao(), motoristasRemoto)
    val avaliacaoProdutoRepository = AvaliacaoProdutoRepository(database.avaliacaoProdutoDao(), avaliacoesRemoto)

    val locationHelper = LocationHelper(context)
    val reverseGeocoder = ReverseGeocoder(context)
    val rotaService = RotaService()

    val geofenceMonitor = GeofenceMonitor()

    val telemetriaSimuladorManager = TelemetriaSimuladorManager(
        veiculoRepository = veiculoRepository,
        telemetriaRepository = telemetriaRepository,
        locationHelper = locationHelper,
        rotaService = rotaService,
        geofenceMonitor = geofenceMonitor,
        escopo = escopoApp
    )

    val geofenceNotificador = GeofenceNotificador(context.applicationContext)

    init {
        escopoApp.sincronizar(veiculosRemoto) { database.veiculoDao().inserirTodos(it) }
        escopoApp.sincronizar(produtosRemoto) { database.produtoDao().inserirTodos(it) }
        escopoApp.sincronizar(negociantesRemoto) { database.negocianteDao().inserirTodos(it) }
        escopoApp.sincronizar(motoristasRemoto) { database.motoristaDao().inserirTodos(it) }
        escopoApp.sincronizar(avaliacoesRemoto) { database.avaliacaoProdutoDao().inserirTodas(it) }
        escopoApp.sincronizar(telemetriasRemoto) { database.registroTelemetriaDao().inserirTodos(it) }

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

package com.example.findus.di

import android.content.Context
import com.example.findus.data.local.AppDatabase
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.local.entity.GrupoVeiculoEntity
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.PerimetroEntity
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.remote.FirestoreColecao
import com.example.findus.data.repository.CentroDistribuicaoRepository
import com.example.findus.data.repository.EntregaRepository
import com.example.findus.data.repository.GrupoVeiculoRepository
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.PerimetroRepository
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.UsuarioRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.data.sync.sincronizar
import com.example.findus.location.Coordenada
import com.example.findus.location.GeofenceMonitor
import com.example.findus.location.GeofenceNotificador
import com.example.findus.location.LocationHelper
import com.example.findus.location.PerimetroMonitorado
import com.example.findus.location.ReverseGeocoder
import com.example.findus.location.RotaService
import com.example.findus.location.montarPerimetros
import com.example.findus.telemetry.TelemetriaSimuladorManager
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val escopoApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AppDatabase.getInstance(context)

    private val veiculosRemoto = FirestoreColecao("veiculos", VeiculoEntity::class.java)
    private val motoristasRemoto = FirestoreColecao("motoristas", MotoristaEntity::class.java)
    private val telemetriasRemoto = FirestoreColecao("registros_telemetria", RegistroTelemetriaEntity::class.java) {
        it.orderBy("timestamp", Query.Direction.DESCENDING).limit(100)
    }
    private val centrosRemoto = FirestoreColecao("centros_distribuicao", CentroDistribuicaoEntity::class.java)
    private val entregasRemoto = FirestoreColecao("entregas", EntregaEntity::class.java)
    private val gruposRemoto = FirestoreColecao("grupos_veiculos", GrupoVeiculoEntity::class.java)
    private val perimetrosRemoto = FirestoreColecao("perimetros", PerimetroEntity::class.java)

    val veiculoRepository = VeiculoRepository(database.veiculoDao(), veiculosRemoto)
    val telemetriaRepository = TelemetriaRepository(database.registroTelemetriaDao(), telemetriasRemoto)
    val motoristaRepository = MotoristaRepository(database.motoristaDao(), motoristasRemoto)
    val centroRepository = CentroDistribuicaoRepository(database.centroDistribuicaoDao(), centrosRemoto)
    val entregaRepository = EntregaRepository(database.entregaDao(), entregasRemoto, veiculoRepository)
    val grupoRepository = GrupoVeiculoRepository(database.grupoVeiculoDao(), gruposRemoto)
    val perimetroRepository = PerimetroRepository(database.perimetroDao(), perimetrosRemoto)
    val usuarioRepository = UsuarioRepository()

    val locationHelper = LocationHelper(context)
    val reverseGeocoder = ReverseGeocoder()
    val rotaService = RotaService()

    val geofenceMonitor = GeofenceMonitor()

    val telemetriaSimuladorManager = TelemetriaSimuladorManager(
        veiculoRepository = veiculoRepository,
        telemetriaRepository = telemetriaRepository,
        entregaRepository = entregaRepository,
        centroRepository = centroRepository,
        locationHelper = locationHelper,
        rotaService = rotaService,
        escopo = escopoApp
    )

    val geofenceNotificador = GeofenceNotificador(context.applicationContext)

    private val veiculosAtuais: StateFlow<List<VeiculoEntity>> = veiculoRepository.observarTodos()
        .stateIn(escopoApp, SharingStarted.Eagerly, emptyList())

    /** Tudo o que o geofencing está monitorando agora (também desenhado no mapa do controlador). */
    val perimetrosMonitorados: StateFlow<List<PerimetroMonitorado>> = combine(
        centroRepository.observarTodos(),
        entregaRepository.observarTodos(),
        perimetroRepository.observarTodos(),
        grupoRepository.observarTodos()
    ) { centros, entregas, perimetros, grupos ->
        montarPerimetros(centros, entregas, perimetros, grupos)
    }.stateIn(escopoApp, SharingStarted.Eagerly, emptyList())

    init {
        escopoApp.sincronizar(veiculosRemoto) { database.veiculoDao().inserirTodos(it) }
        escopoApp.sincronizar(motoristasRemoto) { database.motoristaDao().inserirTodos(it) }
        escopoApp.sincronizar(telemetriasRemoto) { database.registroTelemetriaDao().inserirTodos(it) }
        escopoApp.sincronizar(centrosRemoto) { database.centroDistribuicaoDao().inserirTodos(it) }
        escopoApp.sincronizar(entregasRemoto) { database.entregaDao().inserirTodos(it) }
        escopoApp.sincronizar(gruposRemoto) { database.grupoVeiculoDao().inserirTodos(it) }
        escopoApp.sincronizar(perimetrosRemoto) { database.perimetroDao().inserirTodos(it) }

        escopoApp.launch {
            telemetriaRepository.observarUltimoPorVeiculo().collect { registros ->
                val perimetros = perimetrosMonitorados.value
                registros.forEach { registro ->
                    val posicao = Coordenada(registro.latitude, registro.longitude)
                    geofenceMonitor.avaliar(registro.veiculoId, posicao, perimetros).forEach { evento ->
                        val placa = veiculosAtuais.value.find { it.id == evento.veiculoId }?.placa.orEmpty()
                        geofenceNotificador.notificar(evento.copy(placa = placa))
                    }
                }
            }
        }
    }
}

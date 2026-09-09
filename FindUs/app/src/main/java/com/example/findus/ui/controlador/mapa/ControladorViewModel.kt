package com.example.findus.ui.controlador.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.location.GeofenceNotificador
import com.example.findus.location.ReverseGeocoder
import com.example.findus.telemetry.TelemetriaSimuladorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ControladorViewModel(
    veiculoRepository: VeiculoRepository,
    telemetriaRepository: TelemetriaRepository,
    private val telemetriaSimuladorManager: TelemetriaSimuladorManager,
    geofenceNotificador: GeofenceNotificador,
    private val reverseGeocoder: ReverseGeocoder
) : ViewModel() {
    val veiculos: StateFlow<List<VeiculoEntity>> = veiculoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ultimasTelemetrias: StateFlow<List<RegistroTelemetriaEntity>> = telemetriaRepository.observarUltimoPorVeiculo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eventosGeofence = geofenceNotificador.eventos

    private val _enderecoSelecionado = MutableStateFlow<String?>(null)
    val enderecoSelecionado: StateFlow<String?> = _enderecoSelecionado

    fun iniciarSimulacao(veiculoId: Long) = telemetriaSimuladorManager.iniciar(veiculoId)

    fun pararSimulacao(veiculoId: Long) = telemetriaSimuladorManager.parar(veiculoId)

    fun simulacaoAtiva(veiculoId: Long) = telemetriaSimuladorManager.estaAtivo(veiculoId)

    fun consultarEndereco(latitude: Double, longitude: Double) = viewModelScope.launch {
        _enderecoSelecionado.value = reverseGeocoder.endereco(latitude, longitude)
    }
}

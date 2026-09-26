package com.example.findus.ui.controlador.rota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.location.Coordenada
import com.example.findus.location.LocationHelper
import com.example.findus.location.RotaService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RotaVeiculoViewModel(
    veiculoId: String,
    telemetriaRepository: TelemetriaRepository,
    private val locationHelper: LocationHelper,
    private val rotaService: RotaService
) : ViewModel() {
    val posicaoVeiculo: StateFlow<RegistroTelemetriaEntity?> = telemetriaRepository.observarUltimoDoVeiculo(veiculoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _posicaoControlador = MutableStateFlow<Coordenada?>(null)
    val posicaoControlador: StateFlow<Coordenada?> = _posicaoControlador

    private val _rota = MutableStateFlow<List<Coordenada>>(emptyList())
    val rota: StateFlow<List<Coordenada>> = _rota

    private var ultimoDestino: Coordenada? = null

    init {
        viewModelScope.launch {
            while (true) {
                val veiculo = posicaoVeiculo.value
                val controlador = _posicaoControlador.value
                if (veiculo != null && controlador != null) {
                    val destino = Coordenada(veiculo.latitude, veiculo.longitude)
                    if (destino != ultimoDestino) {
                        _rota.value = rotaService.rota(controlador, destino)
                        ultimoDestino = destino
                    }
                }
                delay(3_000)
            }
        }
    }

    fun atualizarLocalizacaoControlador() = viewModelScope.launch {
        _posicaoControlador.value = locationHelper.obterLocalizacaoOuPadrao()
    }
}

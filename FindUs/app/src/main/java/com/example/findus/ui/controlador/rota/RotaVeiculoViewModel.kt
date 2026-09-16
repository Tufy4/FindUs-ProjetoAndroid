package com.example.findus.ui.controlador.rota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.location.Coordenada
import com.example.findus.location.LocationHelper
import com.example.findus.location.RotaService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    init {
        viewModelScope.launch {
            combine(posicaoVeiculo, _posicaoControlador) { veiculo, controlador -> veiculo to controlador }
                .collect { (veiculo, controlador) ->
                    if (veiculo != null && controlador != null && _rota.value.isEmpty()) {
                        _rota.value = rotaService.rota(controlador, Coordenada(veiculo.latitude, veiculo.longitude))
                    }
                }
        }
    }

    fun atualizarLocalizacaoControlador() = viewModelScope.launch {
        _posicaoControlador.value = locationHelper.obterLocalizacaoAtual()
    }
}

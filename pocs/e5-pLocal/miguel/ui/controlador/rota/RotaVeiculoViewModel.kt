package com.example.findus.ui.controlador.rota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.location.Coordenada
import com.example.findus.location.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RotaVeiculoViewModel(
    veiculoId: Long,
    telemetriaRepository: TelemetriaRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {
    val posicaoVeiculo: StateFlow<RegistroTelemetriaEntity?> = telemetriaRepository.observarUltimoDoVeiculo(veiculoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _posicaoControlador = MutableStateFlow<Coordenada?>(null)
    val posicaoControlador: StateFlow<Coordenada?> = _posicaoControlador

    fun atualizarLocalizacaoControlador() = viewModelScope.launch {
        _posicaoControlador.value = locationHelper.obterLocalizacaoAtual()
    }
}

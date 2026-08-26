package com.example.findus.ui.motorista.rota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.location.Coordenada
import com.example.findus.location.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RotaEntregaViewModel(private val locationHelper: LocationHelper) : ViewModel() {
    private val _posicaoAtual = MutableStateFlow<Coordenada?>(null)
    val posicaoAtual: StateFlow<Coordenada?> = _posicaoAtual

    // Ponto de entrega fixo para a PoC (mesmo destino usado pelo simulador de telemetria).
    val destinoEntrega = Coordenada(-23.1791, -45.8641)

    fun atualizarLocalizacaoAtual() = viewModelScope.launch {
        _posicaoAtual.value = locationHelper.obterLocalizacaoAtual()
    }
}

package com.example.findus.ui.motorista.rota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.location.Coordenada
import com.example.findus.location.LocationHelper
import com.example.findus.location.RotaService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RotaEntregaViewModel(
    private val locationHelper: LocationHelper,
    private val rotaService: RotaService
) : ViewModel() {
    private val _posicaoAtual = MutableStateFlow<Coordenada?>(null)
    val posicaoAtual: StateFlow<Coordenada?> = _posicaoAtual

    private val _destino = MutableStateFlow<Coordenada?>(null)
    val destino: StateFlow<Coordenada?> = _destino

    private val _rota = MutableStateFlow<List<Coordenada>>(emptyList())
    val rota: StateFlow<List<Coordenada>> = _rota

    fun atualizarLocalizacaoAtual() = viewModelScope.launch {
        _posicaoAtual.value = locationHelper.obterLocalizacaoOuPadrao()
    }

    fun definirDestino(coordenada: Coordenada) = viewModelScope.launch {
        _destino.value = coordenada
        val origem = _posicaoAtual.value ?: return@launch
        _rota.value = rotaService.rota(origem, coordenada)
    }
}

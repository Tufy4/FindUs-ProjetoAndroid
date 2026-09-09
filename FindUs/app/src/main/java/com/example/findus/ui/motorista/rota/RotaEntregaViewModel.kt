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

    private val _rota = MutableStateFlow<List<Coordenada>>(emptyList())
    val rota: StateFlow<List<Coordenada>> = _rota

    val destinoEntrega = Coordenada(-23.1791, -45.8641)

    fun atualizarLocalizacaoAtual() = viewModelScope.launch {
        val atual = locationHelper.obterLocalizacaoAtual()
        _posicaoAtual.value = atual
        if (atual != null) _rota.value = rotaService.rota(atual, destinoEntrega)
    }
}

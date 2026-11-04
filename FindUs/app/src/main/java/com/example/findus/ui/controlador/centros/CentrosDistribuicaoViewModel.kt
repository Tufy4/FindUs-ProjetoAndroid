package com.example.findus.ui.controlador.centros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.repository.CentroDistribuicaoRepository
import com.example.findus.data.repository.EntregaRepository
import com.example.findus.domain.RegrasNegocio
import com.example.findus.location.Coordenada
import com.example.findus.location.ReverseGeocoder
import com.example.findus.ui.common.SelecaoLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CentrosDistribuicaoViewModel(
    private val centroRepository: CentroDistribuicaoRepository,
    entregaRepository: EntregaRepository,
    reverseGeocoder: ReverseGeocoder
) : ViewModel() {
    val centros: StateFlow<List<CentroDistribuicaoEntity>> = centroRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val entregas: StateFlow<List<EntregaEntity>> = entregaRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val local = SelecaoLocal(reverseGeocoder, viewModelScope)

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    fun abrir(centro: CentroDistribuicaoEntity?) {
        _erro.value = null
        if (centro == null) {
            local.limpar()
        } else {
            local.definir(Coordenada(centro.latitude, centro.longitude), centro.endereco)
        }
    }

    fun salvar(existente: CentroDistribuicaoEntity?, nome: String, raioTexto: String, onSucesso: () -> Unit) {
        val coordenada = local.coordenada.value
        val raio = raioTexto.replace(',', '.').toDoubleOrNull()
        val mensagem = RegrasNegocio.validarCentro(nome, coordenada, raio)
        if (mensagem != null || coordenada == null || raio == null) {
            _erro.value = mensagem
            return
        }
        val centro = (existente ?: CentroDistribuicaoEntity()).copy(
            nome = nome.trim(),
            endereco = local.enderecoParaSalvar(),
            latitude = coordenada.latitude,
            longitude = coordenada.longitude,
            raioMetros = raio
        )
        _erro.value = null
        viewModelScope.launch {
            centroRepository.salvar(centro)
            onSucesso()
        }
    }

    fun remover(centro: CentroDistribuicaoEntity, onSucesso: () -> Unit) {
        val mensagem = RegrasNegocio.validarRemocaoCentro(centro, entregas.value)
        if (mensagem != null) {
            _erro.value = mensagem
            return
        }
        viewModelScope.launch {
            centroRepository.remover(centro)
            onSucesso()
        }
    }
}

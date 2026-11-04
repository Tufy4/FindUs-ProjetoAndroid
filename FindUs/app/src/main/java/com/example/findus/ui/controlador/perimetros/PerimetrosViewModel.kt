package com.example.findus.ui.controlador.perimetros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.enum.TipoAlvoPerimetro
import com.example.findus.data.local.entity.GrupoVeiculoEntity
import com.example.findus.data.local.entity.PerimetroEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.GrupoVeiculoRepository
import com.example.findus.data.repository.PerimetroRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.domain.RegrasNegocio
import com.example.findus.location.Coordenada
import com.example.findus.location.ReverseGeocoder
import com.example.findus.ui.common.SelecaoLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PerimetrosViewModel(
    private val perimetroRepository: PerimetroRepository,
    veiculoRepository: VeiculoRepository,
    grupoRepository: GrupoVeiculoRepository,
    reverseGeocoder: ReverseGeocoder
) : ViewModel() {
    val perimetros: StateFlow<List<PerimetroEntity>> = perimetroRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val veiculos: StateFlow<List<VeiculoEntity>> = veiculoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val grupos: StateFlow<List<GrupoVeiculoEntity>> = grupoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val local = SelecaoLocal(reverseGeocoder, viewModelScope)

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    fun abrir(perimetro: PerimetroEntity?) {
        _erro.value = null
        if (perimetro == null) {
            local.limpar()
        } else {
            local.definir(Coordenada(perimetro.latitude, perimetro.longitude), perimetro.endereco)
        }
    }

    fun salvar(
        existente: PerimetroEntity?,
        nome: String,
        raioTexto: String,
        tipoAlvo: TipoAlvoPerimetro,
        alvoId: String?,
        ativo: Boolean,
        onSucesso: () -> Unit
    ) {
        val coordenada = local.coordenada.value
        val raio = raioTexto.replace(',', '.').toDoubleOrNull()
        val alvo = if (tipoAlvo == TipoAlvoPerimetro.TODOS) null else alvoId
        val mensagem = RegrasNegocio.validarPerimetro(nome, coordenada, raio, tipoAlvo, alvo)
        if (mensagem != null || coordenada == null || raio == null) {
            _erro.value = mensagem
            return
        }
        val perimetro = (existente ?: PerimetroEntity()).copy(
            nome = nome.trim(),
            endereco = local.enderecoParaSalvar(),
            latitude = coordenada.latitude,
            longitude = coordenada.longitude,
            raioMetros = raio,
            tipoAlvo = tipoAlvo,
            alvoId = alvo,
            ativo = ativo
        )
        _erro.value = null
        viewModelScope.launch {
            perimetroRepository.salvar(perimetro)
            onSucesso()
        }
    }

    fun remover(perimetro: PerimetroEntity, onSucesso: () -> Unit) {
        viewModelScope.launch {
            perimetroRepository.remover(perimetro)
            onSucesso()
        }
    }
}

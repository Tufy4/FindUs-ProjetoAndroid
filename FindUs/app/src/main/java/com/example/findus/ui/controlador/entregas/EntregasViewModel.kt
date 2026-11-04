package com.example.findus.ui.controlador.entregas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.CentroDistribuicaoRepository
import com.example.findus.data.repository.EntregaRepository
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.domain.RegrasNegocio
import com.example.findus.location.Coordenada
import com.example.findus.location.ReverseGeocoder
import com.example.findus.ui.common.SelecaoLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EntregasViewModel(
    private val entregaRepository: EntregaRepository,
    centroRepository: CentroDistribuicaoRepository,
    motoristaRepository: MotoristaRepository,
    veiculoRepository: VeiculoRepository,
    reverseGeocoder: ReverseGeocoder
) : ViewModel() {
    val entregas: StateFlow<List<EntregaEntity>> = entregaRepository.observarTodos()
        .map { lista -> lista.sortedBy { RegrasNegocio.ordemExibicao(it.status) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val centros: StateFlow<List<CentroDistribuicaoEntity>> = centroRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val motoristas: StateFlow<List<MotoristaEntity>> = motoristaRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val veiculos: StateFlow<List<VeiculoEntity>> = veiculoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val local = SelecaoLocal(reverseGeocoder, viewModelScope)

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    fun abrir(entrega: EntregaEntity?) {
        _erro.value = null
        if (entrega == null) {
            local.limpar()
        } else {
            local.definir(Coordenada(entrega.latitudeDestino, entrega.longitudeDestino), entrega.enderecoDestino)
        }
    }

    /** O veículo da entrega é sempre o veículo vinculado ao motorista escolhido. */
    fun salvar(
        existente: EntregaEntity?,
        descricao: String,
        centro: CentroDistribuicaoEntity?,
        motorista: MotoristaEntity?,
        onSucesso: () -> Unit
    ) {
        val destino = local.coordenada.value
        val veiculo = motorista?.veiculoId?.let { id -> veiculos.value.find { it.id == id } }
        val mensagem = RegrasNegocio.validarEntrega(descricao, centro, destino, motorista, veiculo)
        if (mensagem != null || centro == null || destino == null || motorista == null || veiculo == null) {
            _erro.value = mensagem
            return
        }
        val entrega = (existente ?: EntregaEntity()).copy(
            descricao = descricao.trim(),
            centroDistribuicaoId = centro.id,
            enderecoDestino = local.enderecoParaSalvar(),
            latitudeDestino = destino.latitude,
            longitudeDestino = destino.longitude,
            motoristaId = motorista.id,
            veiculoId = veiculo.id
        )
        _erro.value = null
        viewModelScope.launch {
            entregaRepository.salvar(entrega)
            onSucesso()
        }
    }

    fun cancelar(entrega: EntregaEntity) {
        if (!RegrasNegocio.podeCancelarEntrega(entrega)) return
        viewModelScope.launch { entregaRepository.cancelar(entrega) }
    }
}

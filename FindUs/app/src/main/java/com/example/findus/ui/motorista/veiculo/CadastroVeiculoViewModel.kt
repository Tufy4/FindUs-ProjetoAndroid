package com.example.findus.ui.motorista.veiculo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.location.LocationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CadastroVeiculoViewModel(
    private val repository: VeiculoRepository,
    private val telemetriaRepository: TelemetriaRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {
    val veiculos: StateFlow<List<VeiculoEntity>> = repository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(veiculo: VeiculoEntity, novo: Boolean) = viewModelScope.launch {
        repository.salvar(veiculo)
        if (novo) {
            val posicao = locationHelper.obterLocalizacaoOuPadrao()
            telemetriaRepository.registrar(
                RegistroTelemetriaEntity(veiculoId = veiculo.id, latitude = posicao.latitude, longitude = posicao.longitude)
            )
        }
    }

    fun remover(veiculo: VeiculoEntity) = viewModelScope.launch { repository.remover(veiculo) }
}

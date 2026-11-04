package com.example.findus.ui.motorista.veiculo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.CentroDistribuicaoRepository
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.domain.RegrasNegocio
import com.example.findus.location.Coordenada
import com.example.findus.location.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CadastroVeiculoViewModel(
    private val repository: VeiculoRepository,
    private val telemetriaRepository: TelemetriaRepository,
    private val locationHelper: LocationHelper,
    private val motoristaRepository: MotoristaRepository,
    private val centroRepository: CentroDistribuicaoRepository
) : ViewModel() {
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private val motoristaAtual: StateFlow<MotoristaEntity?> = motoristaRepository.observarTodos()
        .map { motoristas -> motoristas.find { it.uid == uid } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** O motorista vê apenas o veículo vinculado a ele (veja RegrasNegocio). */
    val veiculos: StateFlow<List<VeiculoEntity>> = combine(repository.observarTodos(), motoristaAtual) { veiculos, motorista ->
        RegrasNegocio.veiculosVisiveisAoMotorista(veiculos, motorista)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvar(veiculo: VeiculoEntity, novo: Boolean) = viewModelScope.launch {
        repository.salvar(veiculo)
        if (novo) {
            // Veículo novo fica no centro de distribuição (o primeiro cadastrado). Sem centro,
            // usa a posição do aparelho, que no emulador costuma ser Mountain View (EUA).
            val centro = centroRepository.observarTodos().first().firstOrNull()
            val posicao = centro?.let { Coordenada(it.latitude, it.longitude) }
                ?: locationHelper.obterLocalizacaoOuPadrao()
            telemetriaRepository.registrar(
                RegistroTelemetriaEntity(veiculoId = veiculo.id, latitude = posicao.latitude, longitude = posicao.longitude)
            )
            // O veículo cadastrado pelo motorista passa a ser o veículo dele.
            motoristaAtual.value?.let { motorista ->
                motoristaRepository.salvar(motorista.copy(veiculoId = veiculo.id))
            }
        }
    }

    fun remover(veiculo: VeiculoEntity) = viewModelScope.launch { repository.remover(veiculo) }
}

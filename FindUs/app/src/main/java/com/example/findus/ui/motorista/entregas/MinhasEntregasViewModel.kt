package com.example.findus.ui.motorista.entregas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.data.enum.StatusEntrega
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.CentroDistribuicaoRepository
import com.example.findus.data.repository.EntregaRepository
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.domain.RegrasNegocio
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MinhasEntregasViewModel(
    private val entregaRepository: EntregaRepository,
    motoristaRepository: MotoristaRepository,
    centroRepository: CentroDistribuicaoRepository,
    veiculoRepository: VeiculoRepository
) : ViewModel() {
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    /** Cadastro de motorista ligado à conta logada (criado pelo controlador). */
    val motoristaAtual: StateFlow<MotoristaEntity?> = motoristaRepository.observarTodos()
        .map { motoristas -> motoristas.find { it.uid == uid } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val entregas: StateFlow<List<EntregaEntity>> = combine(entregaRepository.observarTodos(), motoristaAtual) { entregas, motorista ->
        if (motorista == null) {
            emptyList()
        } else {
            entregas
                .filter { it.motoristaId == motorista.id && it.status != StatusEntrega.CANCELADA }
                .sortedBy { RegrasNegocio.ordemExibicao(it.status) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val centros: StateFlow<List<CentroDistribuicaoEntity>> = centroRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val veiculos: StateFlow<List<VeiculoEntity>> = veiculoRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    fun iniciar(entrega: EntregaEntity) {
        val mensagem = RegrasNegocio.validarInicio(entrega, entregas.value)
        _erro.value = mensagem
        if (mensagem == null) viewModelScope.launch { entregaRepository.iniciar(entrega) }
    }

    fun concluir(entrega: EntregaEntity) {
        val mensagem = RegrasNegocio.validarConclusao(entrega)
        _erro.value = mensagem
        if (mensagem == null) viewModelScope.launch { entregaRepository.concluir(entrega) }
    }
}

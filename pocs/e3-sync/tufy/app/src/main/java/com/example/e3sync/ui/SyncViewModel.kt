package com.example.e3sync.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.e3sync.data.SyncRepository
import com.example.e3sync.data.local.PocDatabase
import com.example.e3sync.data.local.VeiculoEntity
import com.example.e3sync.data.remote.VeiculoRemoteDataSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = SyncRepository(
        dao = PocDatabase.obter(application).veiculoDao(),
        remoto = VeiculoRemoteDataSource()
    )

    val veiculos: StateFlow<List<VeiculoEntity>> = repositorio.veiculos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendentes: StateFlow<Int> = repositorio.quantidadePendente
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _offlineSimulado = MutableStateFlow(false)
    val offlineSimulado: StateFlow<Boolean> = _offlineSimulado

    private val _log = MutableStateFlow("Pronto.")
    val log: StateFlow<String> = _log

    init {
        // Pull continuo: cada emissao do listener do Firestore e mesclada no Room.
        viewModelScope.launch {
            repositorio.observarRemoto()
                .catch { erro -> _log.value = "Erro ao escutar o Firestore: ${erro.message}" }
                .collect { remotos ->
                    repositorio.aplicarMudancasRemotas(remotos)
                    _log.value = "Recebidos ${remotos.size} documento(s) do Firestore."
                }
        }
    }

    fun salvar(placa: String, modelo: String, motorista: String) {
        if (placa.isBlank()) {
            _log.value = "Informe ao menos a placa."
            return
        }
        viewModelScope.launch {
            repositorio.salvarLocalmente(placa = placa, modelo = modelo, motorista = motorista)
            _log.value = "Salvo no SQLite. Enviando ao Firestore..."
            sincronizar()
        }
    }

    fun remover(id: String) {
        viewModelScope.launch {
            repositorio.removerLocalmente(id)
            sincronizar()
        }
    }

    fun sincronizar() {
        viewModelScope.launch {
            val resultado = repositorio.sincronizarPendentes()
            _log.value = when {
                resultado.enviados == 0 && resultado.falhas == 0 -> "Nada pendente para enviar."
                resultado.falhas == 0 -> "${resultado.enviados} registro(s) enviado(s) ao Firestore."
                else -> "${resultado.enviados} enviado(s), ${resultado.falhas} ainda na fila."
            }
        }
    }

    fun alternarOffline(offline: Boolean) {
        viewModelScope.launch {
            repositorio.definirRede(habilitada = !offline)
            _offlineSimulado.value = offline
            _log.value = if (offline) {
                "Modo offline. As gravacoes ficam so no SQLite."
            } else {
                "Conexao restabelecida."
            }
            if (!offline) sincronizar()
        }
    }
}

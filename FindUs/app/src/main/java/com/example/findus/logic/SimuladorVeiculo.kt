package com.example.findus.logic

import com.example.findus.data.model.PontoSimulado
import com.example.findus.data.model.Veiculo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SimuladorVeiculo(private val veiculo: Veiculo) {
    private val _posicaoAtual = MutableStateFlow(veiculo.trajeto.first())
    val posicaoAtual: StateFlow<PontoSimulado> = _posicaoAtual

    suspend fun iniciar() {
        var index = 0
        while (true) {
            _posicaoAtual.value = veiculo.trajeto[index]
            delay(3000) // 3s entre pontos, ajuste como quiser
            index = (index + 1) % veiculo.trajeto.size
        }
    }
}
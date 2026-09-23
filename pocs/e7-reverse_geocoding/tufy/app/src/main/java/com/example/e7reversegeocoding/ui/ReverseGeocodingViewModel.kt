package com.example.e7reversegeocoding.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.e7reversegeocoding.BuildConfig
import com.example.e7reversegeocoding.location.AndroidReverseGeocoder
import com.example.e7reversegeocoding.location.Coordenada
import com.example.e7reversegeocoding.location.GeoapifyReverseGeocoder
import com.example.e7reversegeocoding.location.LocalizacaoAtual
import com.example.e7reversegeocoding.location.ReverseGeocoder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/** Uma consulta exibida na tela: ponto, resultado, tempo gasto e se veio do cache. */
data class Consulta(
    val coordenada: Coordenada?,
    val estado: EstadoEndereco,
    val duracaoMs: Long? = null,
    val doCache: Boolean = false
) {
    fun resumo(): String? = duracaoMs?.let { "$it ms · ${if (doCache) "do cache" else "consultado"}" }
}

fun Coordenada.formatada(): String = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

class ReverseGeocodingViewModel(application: Application) : AndroidViewModel(application) {
    private val nativo = AndroidReverseGeocoder(application)
    private val comFallback = ReverseGeocoder(principal = GeoapifyReverseGeocoder(), reserva = nativo)
    private val somenteNativo = ReverseGeocoder(principal = nativo)
    private val localizacao = LocalizacaoAtual(application)

    val chaveConfigurada: Boolean = BuildConfig.GEOAPIFY_API_KEY.isNotBlank()

    private val _forcarNativo = MutableStateFlow(false)
    val forcarNativo: StateFlow<Boolean> = _forcarNativo

    private val _selecionado = MutableStateFlow<Coordenada?>(null)
    val selecionado: StateFlow<Coordenada?> = _selecionado

    /** Só muda quando a posição vem do GPS, para o mapa não pular a cada toque. */
    private val _centralizarEm = MutableStateFlow<Coordenada?>(null)
    val centralizarEm: StateFlow<Coordenada?> = _centralizarEm

    private val _atual = MutableStateFlow<Consulta?>(null)
    val atual: StateFlow<Consulta?> = _atual

    private val _historico = MutableStateFlow<List<Consulta>>(emptyList())
    val historico: StateFlow<List<Consulta>> = _historico

    private var consulta: Job? = null

    fun alternarForcarNativo(forcar: Boolean) {
        _forcarNativo.value = forcar
    }

    fun consultar(coordenada: Coordenada) {
        consulta?.cancel()
        consulta = viewModelScope.launch { executar(coordenada) }
    }

    fun consultarDeNovo() {
        _selecionado.value?.let { consultar(it) }
    }

    fun consultarLocalizacaoAtual() {
        consulta?.cancel()
        _atual.value = Consulta(null, EstadoEndereco.Carregando)
        consulta = viewModelScope.launch {
            val posicao = localizacao.obter()
            if (posicao == null) {
                _atual.value = Consulta(
                    null,
                    EstadoEndereco.Indisponivel("Não foi possível obter sua localização. Confira a permissão e o GPS.")
                )
                return@launch
            }
            _centralizarEm.value = posicao
            executar(posicao)
        }
    }

    private suspend fun executar(coordenada: Coordenada) {
        _selecionado.value = coordenada
        _atual.value = Consulta(coordenada, EstadoEndereco.Carregando)

        val geocoder = if (_forcarNativo.value) somenteNativo else comFallback
        val doCache = geocoder.emCache(coordenada.latitude, coordenada.longitude)
        val inicio = SystemClock.elapsedRealtime()
        val estado = geocoder.buscar(coordenada).paraEstado()
        val resultado = Consulta(coordenada, estado, SystemClock.elapsedRealtime() - inicio, doCache)

        _atual.value = resultado
        _historico.value = (listOf(resultado) + _historico.value).take(MAX_HISTORICO)
    }

    private companion object {
        const val MAX_HISTORICO = 10
    }
}

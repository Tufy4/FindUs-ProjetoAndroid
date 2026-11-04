package com.example.findus.ui.common

import com.example.findus.location.Coordenada
import com.example.findus.location.ReverseGeocoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val BUSCANDO = "Buscando endereço…"
private const val NAO_ENCONTRADO = "Endereço não encontrado"

/**
 * Estado de um ponto escolhido no mapa em um formulário, com o endereço
 * obtido por reverse geocoding (Geoapify) a cada novo toque.
 */
class SelecaoLocal(
    private val reverseGeocoder: ReverseGeocoder,
    private val escopo: CoroutineScope
) {
    private val _coordenada = MutableStateFlow<Coordenada?>(null)
    val coordenada: StateFlow<Coordenada?> = _coordenada

    private val _endereco = MutableStateFlow("")
    val endereco: StateFlow<String> = _endereco

    private var busca: Job? = null

    fun selecionar(coordenada: Coordenada) {
        _coordenada.value = coordenada
        _endereco.value = BUSCANDO
        busca?.cancel()
        busca = escopo.launch {
            _endereco.value = reverseGeocoder.endereco(coordenada.latitude, coordenada.longitude) ?: NAO_ENCONTRADO
        }
    }

    /** Preenche com um local já salvo (ao editar um cadastro), sem nova consulta. */
    fun definir(coordenada: Coordenada?, endereco: String) {
        busca?.cancel()
        _coordenada.value = coordenada
        _endereco.value = endereco
    }

    fun limpar() = definir(null, "")

    /** Endereço para gravar no banco: vazio enquanto a busca não terminou ou se não achou. */
    fun enderecoParaSalvar(): String =
        _endereco.value.takeIf { it != BUSCANDO && it != NAO_ENCONTRADO }.orEmpty()
}

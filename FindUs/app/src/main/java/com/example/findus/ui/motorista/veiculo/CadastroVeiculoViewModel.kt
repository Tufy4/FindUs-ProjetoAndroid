package com.example.findus.ui.motorista.veiculo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findus.camera.FotoArmazenamento
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.repository.CentroDistribuicaoRepository
import com.example.findus.data.repository.EntregaRepository
import com.example.findus.data.repository.MotoristaRepository
import com.example.findus.data.repository.TelemetriaRepository
import com.example.findus.data.repository.VeiculoRepository
import com.example.findus.domain.RegrasNegocio
import com.example.findus.location.Coordenada
import com.example.findus.location.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CadastroVeiculoViewModel(
    private val repository: VeiculoRepository,
    private val telemetriaRepository: TelemetriaRepository,
    private val locationHelper: LocationHelper,
    private val motoristaRepository: MotoristaRepository,
    private val centroRepository: CentroDistribuicaoRepository,
    private val fotoArmazenamento: FotoArmazenamento,
    entregaRepository: EntregaRepository
) : ViewModel() {
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    private val motoristaAtual: StateFlow<MotoristaEntity?> = motoristaRepository.observarTodos()
        .map { motoristas -> motoristas.find { it.uid == uid } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** O motorista vê apenas o veículo vinculado a ele (veja RegrasNegocio). */
    val veiculos: StateFlow<List<VeiculoEntity>> = combine(repository.observarTodos(), motoristaAtual) { veiculos, motorista ->
        RegrasNegocio.veiculosVisiveisAoMotorista(veiculos, motorista)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val entregas: StateFlow<List<EntregaEntity>> = entregaRepository.observarTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    // Fica no ViewModel (que sobrevive à recriação da Activity) até a tela consumir.
    private val _fotoCapturada = MutableStateFlow<String?>(null)
    val fotoCapturada: StateFlow<String?> = _fotoCapturada

    private val _processandoFoto = MutableStateFlow(false)
    val processandoFoto: StateFlow<Boolean> = _processandoFoto

    /**
     * Reduz a foto capturada e grava no sistema de arquivos do aparelho. Antes isso era feito
     * na thread principal (decodificar uma foto de 12 MP travava a tela) e a imagem ia em
     * Base64 para dentro do SQLite, contrariando o manual (fotos ficam no filesystem).
     */
    fun processarFoto(uri: Uri) {
        _processandoFoto.value = true
        viewModelScope.launch {
            val nome = withContext(Dispatchers.IO) { runCatching { fotoArmazenamento.salvarCaptura(uri) }.getOrNull() }
            _processandoFoto.value = false
            if (nome == null) _erro.value = "Não foi possível processar a foto. Tente novamente."
            else _fotoCapturada.value = nome
        }
    }

    /** A tela copia a foto para o formulário e avisa que já usou. */
    fun consumirFotoCapturada() {
        _fotoCapturada.value = null
    }

    /** Devolve false (e mostra o erro) quando o formulário é inválido. */
    fun salvar(veiculo: VeiculoEntity, novo: Boolean): Boolean {
        // Cadastrar um veículo novo troca o veículo do motorista: mesma regra da tela do controlador.
        val motorista = motoristaAtual.value
        val mensagem = RegrasNegocio.validarVeiculo(veiculo.placa, veiculo.modelo)
            ?: if (novo && motorista != null) RegrasNegocio.validarTrocaVeiculo(motorista, veiculo.id, entregas.value) else null
        _erro.value = mensagem
        if (mensagem != null) return false

        viewModelScope.launch {
            // "Em rota" vem da entrega ou da simulação em andamento: a edição não pode desfazê-lo.
            val atual = if (novo) null else repository.buscarPorId(veiculo.id)
            val status = if (atual?.status == StatusOperacionalVeiculo.EM_ROTA) StatusOperacionalVeiculo.EM_ROTA else veiculo.status
            val paraSalvar = veiculo.copy(placa = veiculo.placa.trim().uppercase(), modelo = veiculo.modelo.trim(), status = status)
            repository.salvar(paraSalvar)
            if (novo) {
                // Veículo novo fica no centro de distribuição (o primeiro cadastrado). Sem centro,
                // usa a posição do aparelho, que no emulador costuma ser Mountain View (EUA).
                val centro = centroRepository.observarTodos().first().firstOrNull()
                val posicao = centro?.let { Coordenada(it.latitude, it.longitude) }
                    ?: locationHelper.obterLocalizacaoOuPadrao()
                telemetriaRepository.registrar(
                    RegistroTelemetriaEntity(veiculoId = paraSalvar.id, latitude = posicao.latitude, longitude = posicao.longitude)
                )
                // O veículo cadastrado pelo motorista passa a ser o veículo dele.
                motoristaAtual.value?.let { motorista ->
                    motoristaRepository.salvar(motorista.copy(veiculoId = paraSalvar.id))
                }
            }
        }
        return true
    }

    fun limparErro() {
        _erro.value = null
    }

    fun remover(veiculo: VeiculoEntity) = viewModelScope.launch { repository.remover(veiculo) }
}

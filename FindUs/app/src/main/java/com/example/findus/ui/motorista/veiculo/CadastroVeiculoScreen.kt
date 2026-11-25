package com.example.findus.ui.motorista.veiculo

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.camera.FotoCaptureUtils
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.enum.TipoVeiculo
import com.example.findus.data.enum.rotulo
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.domain.RegrasNegocio
import com.example.findus.ui.common.EnumDropdown
import com.example.findus.ui.common.FotoVeiculo
import com.example.findus.ui.common.MensagemErro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroVeiculoScreen(onVoltar: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FindUsApplication
    val viewModel: CadastroVeiculoViewModel = viewModel(factory = viewModelFactory {
        initializer {
            CadastroVeiculoViewModel(
                app.container.veiculoRepository,
                app.container.telemetriaRepository,
                app.container.locationHelper,
                app.container.motoristaRepository,
                app.container.centroRepository,
                app.container.fotoArmazenamento,
                app.container.entregaRepository
            )
        }
    })
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    val erro by viewModel.erro.collectAsStateWithLifecycle()
    val processandoFoto by viewModel.processandoFoto.collectAsStateWithLifecycle()
    val fotoCapturada by viewModel.fotoCapturada.collectAsStateWithLifecycle()

    // rememberSaveable: o app de câmera costuma recriar a Activity (rotação ou falta de memória).
    // Com remember, o URI da foto e o formulário se perdiam e a foto tirada era descartada.
    var veiculoEmEdicaoId by rememberSaveable { mutableStateOf<String?>(null) }
    var placa by rememberSaveable { mutableStateOf("") }
    var modelo by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf(TipoVeiculo.CAMINHAO) }
    var status by rememberSaveable { mutableStateOf(StatusOperacionalVeiculo.DISPONIVEL) }
    var fotoArquivo by rememberSaveable { mutableStateOf<String?>(null) }
    var uriPendente by rememberSaveable { mutableStateOf<Uri?>(null) }

    LaunchedEffect(fotoCapturada) {
        fotoCapturada?.let {
            fotoArquivo = it
            viewModel.consumirFotoCapturada()
        }
    }

    val veiculoEmEdicao = veiculoEmEdicaoId?.let { id -> veiculos.find { it.id == id } }
    val statusControladoPelaEntrega = status == StatusOperacionalVeiculo.EM_ROTA

    val lancadorCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
        val uri = uriPendente
        if (sucesso && uri != null) {
            viewModel.processarFoto(uri)
        }
        uriPendente = null
    }
    val lancadorPermissaoCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) {
            val uri = FotoCaptureUtils.criarUriParaFoto(context)
            uriPendente = uri
            lancadorCamera.launch(uri)
        }
    }

    fun limparFormulario() {
        veiculoEmEdicaoId = null; placa = ""; modelo = ""; tipo = TipoVeiculo.CAMINHAO
        status = StatusOperacionalVeiculo.DISPONIVEL; fotoArquivo = null
        viewModel.limparErro()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastro de veículo") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = placa, onValueChange = { placa = it }, label = { Text("Placa") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = modelo, onValueChange = { modelo = it }, label = { Text("Modelo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                EnumDropdown(
                    label = "Tipo",
                    opcoes = TipoVeiculo.entries.toTypedArray(),
                    selecionado = tipo,
                    onSelecionar = { tipo = it },
                    modifier = Modifier.fillMaxWidth(),
                    rotulo = { it.rotulo() }
                )
                EnumDropdown(
                    label = if (statusControladoPelaEntrega) "Status (definido pela entrega em andamento)" else "Status",
                    opcoes = RegrasNegocio.statusEscolhiveisNoCadastro.toTypedArray(),
                    selecionado = status,
                    onSelecionar = { status = it },
                    modifier = Modifier.fillMaxWidth(),
                    rotulo = { it.rotulo() },
                    habilitado = !statusControladoPelaEntrega
                )

                OutlinedButton(
                    onClick = { lancadorPermissaoCamera.launch(Manifest.permission.CAMERA) },
                    enabled = !processandoFoto
                ) {
                    Text(
                        when {
                            processandoFoto -> "Processando foto…"
                            fotoArquivo == null -> "Tirar foto do veículo"
                            else -> "Tirar outra foto"
                        }
                    )
                }
                FotoVeiculo(fotoArquivo, Modifier.fillMaxWidth().height(180.dp))

                MensagemErro(erro)
                Button(
                    onClick = {
                        val entidade = veiculoEmEdicao?.copy(
                            placa = placa,
                            modelo = modelo,
                            tipo = tipo,
                            status = status,
                            fotoArquivo = fotoArquivo
                        ) ?: VeiculoEntity(
                            placa = placa,
                            modelo = modelo,
                            tipo = tipo,
                            fotoArquivo = fotoArquivo,
                            status = status
                        )
                        if (viewModel.salvar(entidade, novo = veiculoEmEdicao == null)) limparFormulario()
                    },
                    enabled = !processandoFoto,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (veiculoEmEdicao == null) "Cadastrar veículo" else "Salvar alterações") }
                if (veiculoEmEdicao != null) {
                    TextButton(onClick = { limparFormulario() }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar edição") }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Seu veículo", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(veiculos, key = { it.id }) { veiculo ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            viewModel.limparErro()
                            veiculoEmEdicaoId = veiculo.id
                            placa = veiculo.placa
                            modelo = veiculo.modelo
                            tipo = veiculo.tipo
                            status = veiculo.status
                            fotoArquivo = veiculo.fotoArquivo
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FotoVeiculo(veiculo.fotoArquivo, Modifier.size(64.dp))
                            Column {
                                Text("${veiculo.placa} · ${veiculo.modelo}", style = MaterialTheme.typography.titleMedium)
                                Text("${veiculo.tipo.rotulo()} · ${veiculo.status.rotulo()}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

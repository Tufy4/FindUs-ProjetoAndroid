package com.example.findus.ui.motorista.veiculo

import android.Manifest
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.camera.FotoCaptureUtils
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.enum.TipoVeiculo
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.ui.common.EnumDropdown
import com.example.findus.ui.common.FotoVeiculo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroVeiculoScreen(onVoltar: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FindUsApplication
    val viewModel: CadastroVeiculoViewModel = viewModel(factory = viewModelFactory {
        initializer { CadastroVeiculoViewModel(app.container.veiculoRepository) }
    })
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()

    var veiculoEmEdicao by remember { mutableStateOf<VeiculoEntity?>(null) }
    var placa by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoVeiculo.CAMINHAO) }
    var status by remember { mutableStateOf(StatusOperacionalVeiculo.DISPONIVEL) }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var uriPendente by remember { mutableStateOf<Uri?>(null) }

    val lancadorCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
        if (sucesso) fotoUri = uriPendente
    }
    val lancadorPermissaoCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) {
            val uri = FotoCaptureUtils.criarUriParaFoto(context)
            uriPendente = uri
            lancadorCamera.launch(uri)
        }
    }

    fun limparFormulario() {
        veiculoEmEdicao = null; placa = ""; modelo = ""; tipo = TipoVeiculo.CAMINHAO
        status = StatusOperacionalVeiculo.DISPONIVEL; fotoUri = null
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
                OutlinedTextField(value = placa, onValueChange = { placa = it }, label = { Text("Placa") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = modelo, onValueChange = { modelo = it }, label = { Text("Modelo") }, modifier = Modifier.fillMaxWidth())
                EnumDropdown("Tipo", TipoVeiculo.entries.toTypedArray(), tipo, { tipo = it }, Modifier.fillMaxWidth())
                EnumDropdown("Status", StatusOperacionalVeiculo.entries.toTypedArray(), status, { status = it }, Modifier.fillMaxWidth())

                OutlinedButton(onClick = { lancadorPermissaoCamera.launch(Manifest.permission.CAMERA) }) {
                    Text(if (fotoUri == null) "Tirar foto do veículo" else "Tirar outra foto")
                }
                fotoUri?.let { uri ->
                    AndroidView(
                        factory = { ctx -> ImageView(ctx) },
                        update = { it.setImageURI(uri) },
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }

                Button(
                    onClick = {
                        val entidade = veiculoEmEdicao?.copy(
                            placa = placa,
                            modelo = modelo,
                            tipo = tipo,
                            status = status,
                            fotoUri = fotoUri?.toString()
                        ) ?: VeiculoEntity(
                            placa = placa,
                            modelo = modelo,
                            tipo = tipo,
                            fotoUri = fotoUri?.toString(),
                            status = status
                        )
                        viewModel.salvar(entidade)
                        limparFormulario()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (veiculoEmEdicao == null) "Cadastrar veículo" else "Salvar alterações") }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Veículos cadastrados", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(veiculos, key = { it.id }) { veiculo ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            veiculoEmEdicao = veiculo
                            placa = veiculo.placa
                            modelo = veiculo.modelo
                            tipo = veiculo.tipo
                            status = veiculo.status
                            fotoUri = veiculo.fotoUri?.let { Uri.parse(it) }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FotoVeiculo(veiculo.fotoUri, Modifier.size(64.dp))
                            Column {
                                Text("${veiculo.placa} · ${veiculo.modelo}", style = MaterialTheme.typography.titleMedium)
                                Text("${veiculo.tipo} · ${veiculo.status}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

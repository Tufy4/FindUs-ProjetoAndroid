package com.example.findus.ui.motorista.cadastros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.findus.FindUsApplication
import com.example.findus.data.local.entity.AvaliacaoProdutoEntity
import com.example.findus.data.local.entity.ProdutoEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoProdutoScreen(onVoltar: () -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: AvaliacaoProdutoViewModel = viewModel(factory = viewModelFactory {
        initializer { AvaliacaoProdutoViewModel(app.container.avaliacaoProdutoRepository, app.container.produtoRepository) }
    })
    val avaliacoes by viewModel.avaliacoes.collectAsStateWithLifecycle()
    val produtos by viewModel.produtos.collectAsStateWithLifecycle()
    val formatador = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var mostrarDialogo by remember { mutableStateOf(false) }
    var emEdicao by remember { mutableStateOf<AvaliacaoProdutoEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avaliações de produto") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { emEdicao = null; mostrarDialogo = true }, ) {
                if (produtos.isNotEmpty()) Text("+")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            items(avaliacoes, key = { it.id }) { avaliacao ->
                val produto = produtos.find { it.id == avaliacao.produtoId }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { emEdicao = avaliacao; mostrarDialogo = true }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(produto?.nome ?: "Produto removido", style = MaterialTheme.typography.titleMedium)
                        Text("Nota: ${avaliacao.nota}/5 · ${formatador.format(Date(avaliacao.data))}", style = MaterialTheme.typography.bodySmall)
                        avaliacao.comentario?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }

    if (mostrarDialogo && produtos.isNotEmpty()) {
        AvaliacaoFormDialog(
            avaliacao = emEdicao,
            produtos = produtos,
            onDismiss = { mostrarDialogo = false },
            onSalvar = { viewModel.salvar(it); mostrarDialogo = false },
            onRemover = emEdicao?.let { alvo -> { viewModel.remover(alvo); mostrarDialogo = false } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvaliacaoFormDialog(
    avaliacao: AvaliacaoProdutoEntity?,
    produtos: List<ProdutoEntity>,
    onDismiss: () -> Unit,
    onSalvar: (AvaliacaoProdutoEntity) -> Unit,
    onRemover: (() -> Unit)?
) {
    var produtoSelecionado by remember {
        mutableStateOf(produtos.find { it.id == avaliacao?.produtoId } ?: produtos.first())
    }
    var nota by remember { mutableStateOf((avaliacao?.nota ?: 5).toString()) }
    var comentario by remember { mutableStateOf(avaliacao?.comentario.orEmpty()) }
    var expandido by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (avaliacao == null) "Nova avaliação" else "Editar avaliação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
                    OutlinedTextField(
                        value = produtoSelecionado.nome,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Produto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                        produtos.forEach { produto ->
                            DropdownMenuItem(text = { Text(produto.nome) }, onClick = { produtoSelecionado = produto; expandido = false })
                        }
                    }
                }
                OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Nota (1 a 5)") })
                OutlinedTextField(value = comentario, onValueChange = { comentario = it }, label = { Text("Comentário") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSalvar(
                    AvaliacaoProdutoEntity(
                        id = avaliacao?.id ?: UUID.randomUUID().toString(),
                        produtoId = produtoSelecionado.id,
                        nota = nota.toIntOrNull()?.coerceIn(1, 5) ?: 5,
                        comentario = comentario.ifBlank { null },
                        data = avaliacao?.data ?: System.currentTimeMillis()
                    )
                )
            }) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (onRemover != null) {
                    IconButton(onClick = onRemover) { Icon(Icons.Filled.Delete, contentDescription = "Remover") }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

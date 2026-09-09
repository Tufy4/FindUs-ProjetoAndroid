package com.example.findus.ui.motorista.cadastros

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.findus.data.local.entity.ProdutoEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutoScreen(onVoltar: () -> Unit) {
    val app = LocalContext.current.applicationContext as FindUsApplication
    val viewModel: ProdutoViewModel = viewModel(factory = viewModelFactory {
        initializer { ProdutoViewModel(app.container.produtoRepository) }
    })
    val produtos by viewModel.produtos.collectAsStateWithLifecycle()

    var editandoId by remember { mutableStateOf(0L) }
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }

    fun limparFormulario() {
        editandoId = 0L; nome = ""; descricao = ""; peso = ""; categoria = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produtos") },
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
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = peso, onValueChange = { peso = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        viewModel.salvar(
                            ProdutoEntity(
                                id = editandoId,
                                nome = nome,
                                descricao = descricao,
                                peso = peso.toDoubleOrNull() ?: 0.0,
                                categoria = categoria
                            )
                        )
                        limparFormulario()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (editandoId == 0L) "Cadastrar produto" else "Salvar alterações") }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Produtos cadastrados", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(produtos, key = { it.id }) { produto ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            editandoId = produto.id
                            nome = produto.nome
                            descricao = produto.descricao
                            peso = produto.peso.toString()
                            categoria = produto.categoria
                        }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(produto.nome, style = MaterialTheme.typography.titleMedium)
                            Text("${produto.categoria} · ${produto.peso} kg", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

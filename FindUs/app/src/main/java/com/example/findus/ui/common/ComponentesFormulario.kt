package com.example.findus.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.example.findus.location.COORDENADA_PADRAO
import com.example.findus.location.Coordenada
import com.example.findus.ui.map.MapaOsm
import com.example.findus.ui.map.MarcadorMapa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Estrutura comum das telas de cadastro do controlador: uma lista com botão "novo"
 * e, quando [formularioAberto], o formulário no lugar da lista. O voltar (da barra
 * e do sistema) fecha o formulário antes de sair da tela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCadastro(
    titulo: String,
    formularioAberto: Boolean,
    textoBotaoNovo: String,
    onVoltar: () -> Unit,
    onNovo: () -> Unit,
    onFecharFormulario: () -> Unit,
    formulario: @Composable ColumnScope.() -> Unit,
    lista: LazyListScope.() -> Unit
) {
    BackHandler(enabled = formularioAberto) { onFecharFormulario() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = if (formularioAberto) onFecharFormulario else onVoltar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (formularioAberto) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = formulario
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Button(onClick = onNovo, modifier = Modifier.fillMaxWidth()) { Text(textoBotaoNovo) }
                LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp), content = lista)
            }
        }
    }
}

/** Lista suspensa para escolher um item de uma lista (motorista, centro, veículo, grupo...). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SeletorLista(
    label: String,
    opcoes: List<T>,
    selecionado: T?,
    textoDe: (T) -> String,
    onSelecionar: (T) -> Unit,
    modifier: Modifier = Modifier,
    textoVazio: String = "Selecione"
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }, modifier = modifier) {
        OutlinedTextField(
            value = selecionado?.let { textoDe(it) } ?: textoVazio,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            if (opcoes.isEmpty()) {
                DropdownMenuItem(text = { Text("Nenhuma opção cadastrada") }, onClick = { expandido = false })
            }
            opcoes.forEach { opcao ->
                DropdownMenuItem(text = { Text(textoDe(opcao)) }, onClick = {
                    onSelecionar(opcao)
                    expandido = false
                })
            }
        }
    }
}

/** Mapa para escolher um ponto com um toque, mostrando o endereço encontrado logo abaixo. */
@Composable
fun SeletorLocalMapa(
    coordenada: Coordenada?,
    endereco: String,
    onSelecionar: (Coordenada) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            MapaOsm(
                marcadores = listOfNotNull(
                    coordenada?.let { MarcadorMapa(posicao = it, titulo = "Local escolhido", corHex = "#C62828") }
                ),
                modifier = Modifier.fillMaxSize(),
                centro = coordenada ?: COORDENADA_PADRAO,
                zoom = 14.0,
                onToqueMapa = onSelecionar
            )
        }
        Text(
            if (coordenada == null) "Toque no mapa para escolher o local" else endereco.ifBlank { "Endereço não disponível" },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Mostra a mensagem de erro do formulário, quando houver. */
@Composable
fun MensagemErro(mensagem: String?) {
    if (mensagem != null) {
        Text(mensagem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Texto do endereço, ou as coordenadas quando o endereço não foi encontrado. */
fun descreverLocal(endereco: String, latitude: Double, longitude: Double): String =
    endereco.ifBlank { "%.5f, %.5f".format(latitude, longitude) }

fun formatarDataHora(instante: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(instante))

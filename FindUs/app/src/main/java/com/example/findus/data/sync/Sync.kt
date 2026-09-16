package com.example.findus.data.sync

import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch

fun <T : Any> CoroutineScope.sincronizar(colecao: FirestoreColecao<T>, salvar: suspend (List<T>) -> Unit) {
    launch {
        colecao.observarColecao()
            .retry {
                delay(5_000)
                true
            }
            .collect { salvar(it) }
    }
}

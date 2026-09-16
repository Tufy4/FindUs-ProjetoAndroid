package com.example.findus.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreColecao<T : Any>(
    nome: String,
    private val classe: Class<T>,
    private val filtro: (CollectionReference) -> Query = { it }
) {
    private val colecao = FirebaseFirestore.getInstance().collection(nome)

    fun enviar(id: String, entidade: T) {
        colecao.document(id).set(entidade)
    }

    fun observarColecao(): Flow<List<T>> = callbackFlow {
        val registro = filtro(colecao).addSnapshotListener { snapshot, erro ->
            if (erro != null) close(erro)
            else trySend(runCatching { snapshot?.toObjects(classe) }.getOrNull().orEmpty())
        }
        awaitClose { registro.remove() }
    }
}

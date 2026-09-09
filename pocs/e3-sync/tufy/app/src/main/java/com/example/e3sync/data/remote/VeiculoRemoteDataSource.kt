package com.example.e3sync.data.remote

import com.example.e3sync.data.local.VeiculoEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class VeiculoRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val colecao get() = firestore.collection(COLECAO)

    suspend fun enviar(veiculo: VeiculoEntity) {
        colecao.document(veiculo.id).set(veiculo.paraMapa()).await()
    }

    fun observarColecao(): Flow<List<VeiculoEntity>> = callbackFlow {
        val registro = colecao.addSnapshotListener { snapshot, erro ->
            if (erro != null) {
                close(erro)
                return@addSnapshotListener
            }
            val documentos = snapshot?.documents.orEmpty().mapNotNull { it.paraEntidade() }
            trySend(documentos)
        }
        awaitClose { registro.remove() }
    }

    suspend fun definirRede(habilitada: Boolean) {
        if (habilitada) firestore.enableNetwork().await() else firestore.disableNetwork().await()
    }

    private fun VeiculoEntity.paraMapa(): Map<String, Any> = mapOf(
        "placa" to placa,
        "modelo" to modelo,
        "motorista" to motorista,
        "atualizadoEm" to atualizadoEm,
        "deletado" to deletado
    )

    private fun DocumentSnapshot.paraEntidade(): VeiculoEntity? {
        val placa = getString("placa") ?: return null
        return VeiculoEntity(
            id = id,
            placa = placa,
            modelo = getString("modelo").orEmpty(),
            motorista = getString("motorista").orEmpty(),
            atualizadoEm = getLong("atualizadoEm") ?: 0L,
            pendenteSync = false,
            deletado = getBoolean("deletado") ?: false
        )
    }

    companion object {
        private const val COLECAO = "veiculos"
    }
}

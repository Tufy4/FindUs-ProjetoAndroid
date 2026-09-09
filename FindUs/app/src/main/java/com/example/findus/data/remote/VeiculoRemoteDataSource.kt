package com.example.findus.data.remote

import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.enum.TipoVeiculo
import com.example.findus.data.local.entity.VeiculoEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class VeiculoRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val colecao get() = firestore.collection("veiculos")

    suspend fun enviar(veiculo: VeiculoEntity, fotoBase64: String?) {
        val dados = mutableMapOf<String, Any>(
            "placa" to veiculo.placa,
            "modelo" to veiculo.modelo,
            "tipo" to veiculo.tipo.name,
            "status" to veiculo.status.name,
            "atualizadoEm" to veiculo.atualizadoEm,
            "deletado" to veiculo.deletado
        )
        if (fotoBase64 != null) dados["fotoBase64"] = fotoBase64
        colecao.document(veiculo.syncId).set(dados, SetOptions.merge()).await()
    }

    fun observarColecao(): Flow<List<VeiculoEntity>> = callbackFlow {
        val registro = colecao.addSnapshotListener { snapshot, erro ->
            if (erro != null) {
                close(erro)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull { it.paraEntidade() })
        }
        awaitClose { registro.remove() }
    }

    private fun DocumentSnapshot.paraEntidade(): VeiculoEntity? {
        val placa = getString("placa") ?: return null
        return VeiculoEntity(
            placa = placa,
            modelo = getString("modelo").orEmpty(),
            tipo = runCatching { TipoVeiculo.valueOf(getString("tipo").orEmpty()) }
                .getOrDefault(TipoVeiculo.CAMINHAO),
            status = runCatching { StatusOperacionalVeiculo.valueOf(getString("status").orEmpty()) }
                .getOrDefault(StatusOperacionalVeiculo.DISPONIVEL),
            syncId = id,
            atualizadoEm = getLong("atualizadoEm") ?: 0L,
            pendenteSync = false,
            deletado = getBoolean("deletado") ?: false
        )
    }
}

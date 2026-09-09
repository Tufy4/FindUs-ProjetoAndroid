package com.example.e3sync.data

import com.example.e3sync.data.local.VeiculoDao
import com.example.e3sync.data.local.VeiculoEntity
import com.example.e3sync.data.remote.VeiculoRemoteDataSource
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import java.util.UUID

data class ResultadoSync(val enviados: Int, val falhas: Int)


class SyncRepository(
    private val dao: VeiculoDao,
    private val remoto: VeiculoRemoteDataSource
) {

    val veiculos: Flow<List<VeiculoEntity>> = dao.observarAtivos()
    val quantidadePendente: Flow<Int> = dao.observarQuantidadePendente()

    suspend fun salvarLocalmente(
        id: String? = null,
        placa: String,
        modelo: String,
        motorista: String
    ) {
        dao.salvar(
            VeiculoEntity(
                id = id ?: UUID.randomUUID().toString(),
                placa = placa.trim().uppercase(),
                modelo = modelo.trim(),
                motorista = motorista.trim(),
                atualizadoEm = System.currentTimeMillis(),
                pendenteSync = true,
                deletado = false
            )
        )
    }

    suspend fun removerLocalmente(id: String) {
        val atual = dao.buscarPorId(id) ?: return
        dao.salvar(
            atual.copy(
                deletado = true,
                atualizadoEm = System.currentTimeMillis(),
                pendenteSync = true
            )
        )
    }

    suspend fun sincronizarPendentes(): ResultadoSync {
        var enviados = 0
        var falhas = 0

        dao.listarPendentes().forEach { pendente ->
            try {
                withTimeout(TIMEOUT_ENVIO_MS) { remoto.enviar(pendente) }
                dao.marcarComoSincronizado(pendente.id, pendente.atualizadoEm)
                enviados++
            } catch (e: TimeoutCancellationException) {
                // Sem rede a Task do Firestore fica pendente em vez de falhar.
                // O registro permanece na fila para a proxima tentativa.
                falhas++
            } catch (e: Exception) {
                falhas++
            }
        }

        return ResultadoSync(enviados, falhas)
    }

    suspend fun aplicarMudancasRemotas(remotos: List<VeiculoEntity>) {
        remotos.forEach { vindoDoServidor ->
            val local = dao.buscarPorId(vindoDoServidor.id)
            val deveAplicar = when {
                local == null -> true
                local.pendenteSync && local.atualizadoEm >= vindoDoServidor.atualizadoEm -> false
                vindoDoServidor.atualizadoEm > local.atualizadoEm -> true
                else -> false
            }
            if (deveAplicar) dao.salvar(vindoDoServidor)
        }
    }

    fun observarRemoto(): Flow<List<VeiculoEntity>> = remoto.observarColecao()

    suspend fun definirRede(habilitada: Boolean) = remoto.definirRede(habilitada)

    companion object {
        private const val TIMEOUT_ENVIO_MS = 5_000L
    }
}

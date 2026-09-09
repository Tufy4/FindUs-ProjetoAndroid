package com.example.findus.data.sync

import android.content.Context
import android.net.Uri
import com.example.findus.camera.FotoBase64
import com.example.findus.data.local.dao.VeiculoDao
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.remote.VeiculoRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class VeiculoSync(
    private val context: Context,
    private val dao: VeiculoDao,
    private val remoto: VeiculoRemoteDataSource,
    private val escopo: CoroutineScope
) {
    fun iniciar() {
        escopo.launch {
            remoto.observarColecao()
                .retry {
                    delay(5_000)
                    true
                }
                .collect { aplicarMudancasRemotas(it) }
        }
        escopo.launch {
            while (isActive) {
                runCatching { sincronizarPendentes() }
                delay(15_000)
            }
        }
    }

    fun agendarPush() {
        escopo.launch { runCatching { sincronizarPendentes() } }
    }

    private suspend fun sincronizarPendentes() {
        dao.listarPendentes().forEach { veiculo ->
            val fotoBase64 = if (veiculo.fotoUri != null && !veiculo.fotoSincronizada) {
                runCatching { FotoBase64.paraBase64(context, Uri.parse(veiculo.fotoUri)) }.getOrNull()
            } else {
                null
            }
            runCatching {
                withTimeout(5_000) { remoto.enviar(veiculo, fotoBase64) }
                if (fotoBase64 != null) dao.atualizar(veiculo.copy(fotoSincronizada = true))
                dao.marcarComoSincronizado(veiculo.id, veiculo.atualizadoEm)
            }
        }
    }

    private suspend fun aplicarMudancasRemotas(remotos: List<VeiculoEntity>) {
        remotos.forEach { vindoDoServidor ->
            val local = dao.buscarPorSyncId(vindoDoServidor.syncId)
                ?: dao.buscarPorPlaca(vindoDoServidor.placa)

            if (local == null) {
                dao.inserir(vindoDoServidor)
                return@forEach
            }
            if (local.pendenteSync || vindoDoServidor.atualizadoEm <= local.atualizadoEm) return@forEach

            dao.atualizar(
                local.copy(
                    syncId = vindoDoServidor.syncId,
                    placa = vindoDoServidor.placa,
                    modelo = vindoDoServidor.modelo,
                    tipo = vindoDoServidor.tipo,
                    status = vindoDoServidor.status,
                    atualizadoEm = vindoDoServidor.atualizadoEm,
                    pendenteSync = false,
                    deletado = vindoDoServidor.deletado
                )
            )
        }
    }
}

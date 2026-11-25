package com.example.findus.data.repository

import com.example.findus.camera.FotoArmazenamento
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.local.dao.VeiculoDao
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.data.remote.FirestoreColecao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Dados do veículo no Room; a foto fica em arquivo local (FotoArmazenamento) e só
 * é convertida para Base64 no momento de enviar o documento para o Firestore.
 */
class VeiculoRepository(
    private val dao: VeiculoDao,
    private val remoto: FirestoreColecao<VeiculoEntity>,
    private val fotos: FotoArmazenamento
) {
    fun observarTodos(): Flow<List<VeiculoEntity>> = dao.observarTodos()

    suspend fun buscarPorId(id: String): VeiculoEntity? = dao.buscarPorId(id)

    suspend fun salvar(veiculo: VeiculoEntity) {
        val local = veiculo.copy(fotoBase64 = null)
        dao.inserir(local)
        val fotoRemota = withContext(Dispatchers.IO) { fotos.lerBase64(local.fotoArquivo) }
        remoto.enviar(local.id, local.copy(fotoBase64 = fotoRemota))
    }

    /** Usado pela sincronização: a foto que chega em Base64 vira arquivo local antes de ir para o Room. */
    suspend fun salvarDoRemoto(veiculos: List<VeiculoEntity>) {
        val locais = veiculos.map { remoto ->
            val arquivo = withContext(Dispatchers.IO) { remoto.fotoBase64?.let { fotos.salvarDoRemoto(it) } }
                ?: dao.buscarPorId(remoto.id)?.fotoArquivo
            remoto.copy(fotoBase64 = null, fotoArquivo = arquivo)
        }
        dao.inserirTodos(locais)
    }

    suspend fun remover(veiculo: VeiculoEntity) = salvar(veiculo.copy(deletado = true))

    suspend fun atualizarStatus(veiculoId: String, status: StatusOperacionalVeiculo) {
        dao.buscarPorId(veiculoId)?.let { salvar(it.copy(status = status)) }
    }
}

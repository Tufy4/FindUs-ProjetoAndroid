package com.example.findus.domain

import com.example.findus.data.enum.StatusEntrega
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.enum.TipoAlvoPerimetro
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.local.entity.GrupoVeiculoEntity
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.PerimetroEntity
import com.example.findus.data.local.entity.VeiculoEntity
import com.example.findus.location.Coordenada

/**
 * Regras de negócio do app reunidas em um só lugar. As funções de validação
 * devolvem a mensagem de erro a mostrar para o usuário, ou null quando está tudo certo.
 */
object RegrasNegocio {
    /** Raio do perímetro criado automaticamente em volta de cada ponto de entrega ativo. */
    const val RAIO_PONTO_ENTREGA_METROS = 200.0

    fun entregaAtiva(entrega: EntregaEntity): Boolean =
        entrega.status == StatusEntrega.PENDENTE || entrega.status == StatusEntrega.EM_ROTA

    /** Ordem das listas: primeiro o que está em andamento, depois o que falta fazer, por fim o histórico. */
    fun ordemExibicao(status: StatusEntrega): Int = when (status) {
        StatusEntrega.EM_ROTA -> 0
        StatusEntrega.PENDENTE -> 1
        StatusEntrega.ENTREGUE -> 2
        StatusEntrega.CANCELADA -> 3
    }

    fun podeEditarEntrega(entrega: EntregaEntity): Boolean = entrega.status == StatusEntrega.PENDENTE

    fun podeCancelarEntrega(entrega: EntregaEntity): Boolean = entregaAtiva(entrega)

    fun veiculoPodeReceberEntrega(veiculo: VeiculoEntity): Boolean =
        veiculo.status != StatusOperacionalVeiculo.MANUTENCAO && veiculo.status != StatusOperacionalVeiculo.INATIVO

    fun validarEntrega(
        descricao: String,
        centro: CentroDistribuicaoEntity?,
        destino: Coordenada?,
        motorista: MotoristaEntity?,
        veiculo: VeiculoEntity?
    ): String? = when {
        descricao.isBlank() -> "Informe uma descrição para a entrega"
        centro == null -> "Escolha o centro de distribuição de origem"
        destino == null -> "Toque no mapa para escolher o ponto de entrega"
        motorista == null -> "Escolha o motorista responsável"
        veiculo == null -> "O motorista ${motorista.nome} não tem veículo vinculado"
        !veiculoPodeReceberEntrega(veiculo) ->
            "O veículo ${veiculo.placa} está ${veiculo.status} e não pode receber entregas"
        else -> null
    }

    fun validarInicio(entrega: EntregaEntity, entregasDoMotorista: List<EntregaEntity>): String? = when {
        entrega.status != StatusEntrega.PENDENTE -> "Só é possível iniciar uma entrega pendente"
        entregasDoMotorista.any { it.id != entrega.id && it.status == StatusEntrega.EM_ROTA } ->
            "Conclua a entrega em andamento antes de iniciar outra"
        else -> null
    }

    fun validarConclusao(entrega: EntregaEntity): String? =
        if (entrega.status != StatusEntrega.EM_ROTA) "Só é possível concluir uma entrega em rota" else null

    fun validarCentro(nome: String, local: Coordenada?, raioMetros: Double?): String? = when {
        nome.isBlank() -> "Informe o nome do centro de distribuição"
        local == null -> "Toque no mapa para escolher a localização do centro"
        raioMetros == null || raioMetros <= 0.0 -> "Informe um raio válido, em metros"
        else -> null
    }

    fun validarRemocaoCentro(centro: CentroDistribuicaoEntity, entregas: List<EntregaEntity>): String? =
        if (entregas.any { it.centroDistribuicaoId == centro.id && entregaAtiva(it) }) {
            "Este centro tem entregas pendentes ou em rota e não pode ser removido"
        } else {
            null
        }

    fun validarGrupo(nome: String, veiculoIds: Collection<String>): String? = when {
        nome.isBlank() -> "Informe o nome do grupo"
        veiculoIds.isEmpty() -> "Selecione ao menos um veículo"
        else -> null
    }

    fun validarRemocaoGrupo(grupo: GrupoVeiculoEntity, perimetros: List<PerimetroEntity>): String? =
        if (perimetros.any { it.tipoAlvo == TipoAlvoPerimetro.GRUPO && it.alvoId == grupo.id }) {
            "Este grupo é monitorado por um perímetro. Altere o perímetro antes de remover o grupo"
        } else {
            null
        }

    fun validarPerimetro(
        nome: String,
        local: Coordenada?,
        raioMetros: Double?,
        tipoAlvo: TipoAlvoPerimetro,
        alvoId: String?
    ): String? = when {
        nome.isBlank() -> "Informe o nome do perímetro"
        local == null -> "Toque no mapa para escolher o centro do perímetro"
        raioMetros == null || raioMetros <= 0.0 -> "Informe um raio válido, em metros"
        tipoAlvo != TipoAlvoPerimetro.TODOS && alvoId == null -> "Escolha o veículo ou o grupo monitorado"
        else -> null
    }

    /** Um veículo só pode estar vinculado a um motorista por vez. */
    fun veiculosLivresParaMotorista(
        veiculos: List<VeiculoEntity>,
        motoristas: List<MotoristaEntity>,
        motoristaId: String
    ): List<VeiculoEntity> {
        val ocupados = motoristas.filter { it.id != motoristaId }.mapNotNull { it.veiculoId }.toSet()
        return veiculos.filter { it.id !in ocupados }
    }

    /**
     * O motorista vê só o veículo vinculado a ele. Contas sem cadastro de motorista
     * (criadas direto no console do Firebase) continuam vendo a frota toda.
     */
    fun veiculosVisiveisAoMotorista(veiculos: List<VeiculoEntity>, motorista: MotoristaEntity?): List<VeiculoEntity> =
        if (motorista == null) veiculos else veiculos.filter { it.id == motorista.veiculoId }
}

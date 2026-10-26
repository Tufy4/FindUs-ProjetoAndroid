package com.example.findus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.findus.data.enum.TipoUsuario
import com.example.findus.ui.controlador.ControladorHomeScreen
import com.example.findus.ui.controlador.cadastros.MotoristaScreen
import com.example.findus.ui.controlador.mapa.MapaControladorScreen
import com.example.findus.ui.controlador.rota.RotaVeiculoScreen
import com.example.findus.ui.motorista.MotoristaHomeScreen
import com.example.findus.ui.motorista.rota.RotaEntregaScreen
import com.example.findus.ui.motorista.veiculo.CadastroVeiculoScreen

private object Rotas {
    const val SELECAO_PERFIL = "selecao_perfil"
    const val MOTORISTA_HOME = "motorista_home"
    const val MOTORISTA_VEICULO = "motorista_veiculo"
    const val MOTORISTA_ROTA = "motorista_rota"
    const val CONTROLADOR_HOME = "controlador_home"
    const val CONTROLADOR_MAPA = "controlador_mapa"
    const val CONTROLADOR_MOTORISTAS = "controlador_motoristas"
    const val CONTROLADOR_ROTA = "controlador_rota/{veiculoId}"
    fun controladorRota(veiculoId: String) = "controlador_rota/$veiculoId"
}

/**
 * [tipoUsuario] é o perfil já identificado da conta (salvo no cadastro). Quando nulo
 * (contas antigas sem perfil definido), o fluxo abre a tela de seleção manual e persiste
 * a escolha via [onSelecionarTipo] para que o próximo login já entre direto na home certa.
 */
@Composable
fun FindUsNavHost(
    tipoUsuario: TipoUsuario?,
    onSelecionarTipo: (TipoUsuario) -> Unit,
    onSair: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = when (tipoUsuario) {
        TipoUsuario.MOTORISTA -> Rotas.MOTORISTA_HOME
        TipoUsuario.CONTROLADOR -> Rotas.CONTROLADOR_HOME
        null -> Rotas.SELECAO_PERFIL
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Rotas.SELECAO_PERFIL) {
            PerfilSelectScreen(
                onEscolherMotorista = {
                    onSelecionarTipo(TipoUsuario.MOTORISTA)
                    navController.navigate(Rotas.MOTORISTA_HOME) {
                        popUpTo(Rotas.SELECAO_PERFIL) { inclusive = true }
                    }
                },
                onEscolherControlador = {
                    onSelecionarTipo(TipoUsuario.CONTROLADOR)
                    navController.navigate(Rotas.CONTROLADOR_HOME) {
                        popUpTo(Rotas.SELECAO_PERFIL) { inclusive = true }
                    }
                },
                onSair = onSair
            )
        }

        composable(Rotas.MOTORISTA_HOME) {
            MotoristaHomeScreen(
                onVoltar = onSair,
                onCadastroVeiculo = { navController.navigate(Rotas.MOTORISTA_VEICULO) },
                onRotaEntrega = { navController.navigate(Rotas.MOTORISTA_ROTA) }
            )
        }
        composable(Rotas.MOTORISTA_VEICULO) { CadastroVeiculoScreen(onVoltar = { navController.popBackStack() }) }
        composable(Rotas.MOTORISTA_ROTA) { RotaEntregaScreen(onVoltar = { navController.popBackStack() }) }

        composable(Rotas.CONTROLADOR_HOME) {
            ControladorHomeScreen(
                onVoltar = onSair,
                onMapaTelemetria = { navController.navigate(Rotas.CONTROLADOR_MAPA) },
                onCadastroMotoristas = { navController.navigate(Rotas.CONTROLADOR_MOTORISTAS) }
            )
        }
        composable(Rotas.CONTROLADOR_MOTORISTAS) { MotoristaScreen(onVoltar = { navController.popBackStack() }) }
        composable(Rotas.CONTROLADOR_MAPA) {
            MapaControladorScreen(
                onVoltar = { navController.popBackStack() },
                onVerRota = { veiculoId -> navController.navigate(Rotas.controladorRota(veiculoId)) }
            )
        }
        composable(
            Rotas.CONTROLADOR_ROTA,
            arguments = listOf(navArgument("veiculoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val veiculoId = backStackEntry.arguments?.getString("veiculoId").orEmpty()
            RotaVeiculoScreen(veiculoId = veiculoId, onVoltar = { navController.popBackStack() })
        }
    }
}

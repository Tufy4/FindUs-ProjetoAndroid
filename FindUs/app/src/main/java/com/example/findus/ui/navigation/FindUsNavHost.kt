package com.example.findus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.findus.ui.controlador.ControladorHomeScreen
import com.example.findus.ui.controlador.cadastros.MotoristaScreen
import com.example.findus.ui.controlador.mapa.MapaControladorScreen
import com.example.findus.ui.controlador.rota.RotaVeiculoScreen
import com.example.findus.ui.motorista.MotoristaHomeScreen
import com.example.findus.ui.motorista.cadastros.AvaliacaoProdutoScreen
import com.example.findus.ui.motorista.cadastros.NegocianteScreen
import com.example.findus.ui.motorista.cadastros.ProdutoScreen
import com.example.findus.ui.motorista.rota.RotaEntregaScreen
import com.example.findus.ui.motorista.veiculo.CadastroVeiculoScreen

private object Rotas {
    const val SELECAO_PERFIL = "selecao_perfil"
    const val MOTORISTA_HOME = "motorista_home"
    const val MOTORISTA_VEICULO = "motorista_veiculo"
    const val MOTORISTA_PRODUTOS = "motorista_produtos"
    const val MOTORISTA_NEGOCIANTES = "motorista_negociantes"
    const val MOTORISTA_AVALIACOES = "motorista_avaliacoes"
    const val MOTORISTA_ROTA = "motorista_rota"
    const val CONTROLADOR_HOME = "controlador_home"
    const val CONTROLADOR_MAPA = "controlador_mapa"
    const val CONTROLADOR_MOTORISTAS = "controlador_motoristas"
    const val CONTROLADOR_ROTA = "controlador_rota/{veiculoId}"
    fun controladorRota(veiculoId: String) = "controlador_rota/$veiculoId"
}

@Composable
fun FindUsNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Rotas.SELECAO_PERFIL) {
        composable(Rotas.SELECAO_PERFIL) {
            PerfilSelectScreen(
                onEscolherMotorista = { navController.navigate(Rotas.MOTORISTA_HOME) },
                onEscolherControlador = { navController.navigate(Rotas.CONTROLADOR_HOME) }
            )
        }

        composable(Rotas.MOTORISTA_HOME) {
            MotoristaHomeScreen(
                onVoltar = { navController.popBackStack() },
                onCadastroVeiculo = { navController.navigate(Rotas.MOTORISTA_VEICULO) },
                onProdutos = { navController.navigate(Rotas.MOTORISTA_PRODUTOS) },
                onNegociantes = { navController.navigate(Rotas.MOTORISTA_NEGOCIANTES) },
                onAvaliacoes = { navController.navigate(Rotas.MOTORISTA_AVALIACOES) },
                onRotaEntrega = { navController.navigate(Rotas.MOTORISTA_ROTA) }
            )
        }
        composable(Rotas.MOTORISTA_VEICULO) { CadastroVeiculoScreen(onVoltar = { navController.popBackStack() }) }
        composable(Rotas.MOTORISTA_PRODUTOS) { ProdutoScreen(onVoltar = { navController.popBackStack() }) }
        composable(Rotas.MOTORISTA_NEGOCIANTES) { NegocianteScreen(onVoltar = { navController.popBackStack() }) }
        composable(Rotas.MOTORISTA_AVALIACOES) { AvaliacaoProdutoScreen(onVoltar = { navController.popBackStack() }) }
        composable(Rotas.MOTORISTA_ROTA) { RotaEntregaScreen(onVoltar = { navController.popBackStack() }) }

        composable(Rotas.CONTROLADOR_HOME) {
            ControladorHomeScreen(
                onVoltar = { navController.popBackStack() },
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

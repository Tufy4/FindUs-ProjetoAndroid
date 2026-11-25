package com.example.findus.data.repository

import android.content.Context
import com.example.findus.data.enum.TipoUsuario
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UsuarioRepository(
    context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val colecao = firestore.collection("usuarios")

    // Cópia local do perfil: sem ela, abrir o app sem internet (e sem o documento no cache
    // do Firestore) lançava exceção na tela inicial e o app fechava.
    private val preferencias = context.applicationContext.getSharedPreferences("perfil_usuario", Context.MODE_PRIVATE)

    /**
     * Perfil da conta. Tenta o Firestore e, se falhar, usa o perfil salvo no aparelho.
     * Lança exceção só quando não há nenhum dos dois, para a tela poder oferecer "tentar de novo".
     */
    suspend fun buscarTipo(uid: String): TipoUsuario? {
        val remoto = runCatching { colecao.document(uid).get().await() }
        if (remoto.isFailure) {
            return tipoSalvoLocalmente(uid) ?: throw remoto.exceptionOrNull()!!
        }
        val tipo = remoto.getOrNull()?.getString("tipo")
            ?.let { runCatching { TipoUsuario.valueOf(it) }.getOrNull() }
        if (tipo != null) salvarLocalmente(uid, tipo)
        return tipo
    }

    suspend fun salvarTipo(uid: String, tipo: TipoUsuario) {
        salvarLocalmente(uid, tipo)
        colecao.document(uid).set(mapOf("tipo" to tipo.name)).await()
    }

    private fun tipoSalvoLocalmente(uid: String): TipoUsuario? =
        preferencias.getString(uid, null)?.let { runCatching { TipoUsuario.valueOf(it) }.getOrNull() }

    private fun salvarLocalmente(uid: String, tipo: TipoUsuario) {
        preferencias.edit().putString(uid, tipo.name).apply()
    }

    // Usa uma instância de FirebaseApp isolada: createUserWithEmailAndPassword loga
    // automaticamente com a conta criada, e isso derrubaria a sessão do controlador.
    suspend fun criarContaMotorista(context: Context, email: String, senha: String): String {
        val appIsolado = FirebaseApp.initializeApp(
            context.applicationContext,
            FirebaseApp.getInstance().options,
            "cadastro_motorista_${System.nanoTime()}"
        )
        return try {
            val resultado = FirebaseAuth.getInstance(appIsolado)
                .createUserWithEmailAndPassword(email, senha)
                .await()
            resultado.user?.uid ?: throw IllegalStateException("Usuário não foi criado")
        } finally {
            appIsolado.delete()
        }
    }
}

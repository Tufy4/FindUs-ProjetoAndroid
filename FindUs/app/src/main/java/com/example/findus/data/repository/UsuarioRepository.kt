package com.example.findus.data.repository

import android.content.Context
import com.example.findus.data.enum.TipoUsuario
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UsuarioRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val colecao = firestore.collection("usuarios")

    suspend fun buscarTipo(uid: String): TipoUsuario? {
        val documento = colecao.document(uid).get().await()
        val tipo = documento.getString("tipo") ?: return null
        return runCatching { TipoUsuario.valueOf(tipo) }.getOrNull()
    }

    suspend fun salvarTipo(uid: String, tipo: TipoUsuario) {
        colecao.document(uid).set(mapOf("tipo" to tipo.name)).await()
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

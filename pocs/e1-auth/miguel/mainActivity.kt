package com.example.findus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TelaLogin()
            }
        }
    }
}

@Composable
fun TelaLogin() {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "FINDus", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (email.isBlank() || senha.isBlank()) {
                        status = "Preencha e-mail e senha"
                        return@Button
                    }
                    auth.createUserWithEmailAndPassword(email, senha)
                        .addOnCompleteListener { task ->
                            status = if (task.isSuccessful) {
                                "Conta criada! Usuário: ${auth.currentUser?.email}"
                            } else {
                                "Erro ao cadastrar: ${task.exception?.message}"
                            }
                        }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cadastrar")
            }

            Button(
                onClick = {
                    if (email.isBlank() || senha.isBlank()) {
                        status = "Preencha e-mail e senha"
                        return@Button
                    }
                    auth.signInWithEmailAndPassword(email, senha)
                        .addOnCompleteListener { task ->
                            status = if (task.isSuccessful) {
                                "Login feito! Usuário: ${auth.currentUser?.email}"
                            } else {
                                "Erro ao entrar: ${task.exception?.message}"
                            }
                        }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Entrar")
            }
        }

        if (status.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = status)
        }
    }
}
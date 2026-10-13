package com.example.recollect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(onSignIn: (String, String) -> Unit, onSignUp: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Recollect", style = MaterialTheme.typography.headlineLarge)
        Text("Your personal save-for-later inbox")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password (6+ characters)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (email.isBlank() || password.length < 6) message = "Enter a valid email and password with at least 6 characters"
                else if (isSignUp) onSignUp(email, password) else onSignIn(email, password)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isSignUp) "Create account" else "Sign in") }
        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(if (isSignUp) "Already have an account? Sign in" else "New here? Create an account")
        }
        if (message.isNotBlank()) Text(message)
    }
}

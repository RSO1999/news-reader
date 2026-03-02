package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.storystream.reader_app.ui.components.Masthead
import com.storystream.reader_app.ui.theme.AppTheme
import kotlinx.coroutines.launch
import com.storystream.reader_app.repository.AuthRepository

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLogin: (email: String, password: String) -> Unit = { _, _ -> },
    onCreateAccount: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val enabled = email.isNotBlank() && password.isNotBlank() && !loading
    val repo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(top = 24.dp)
    ) {
        Masthead(title = "Sign in")

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                scope.launch {
                    loading = true
                    error = null
                    val res = repo.login(email.trim(), password)
                    loading = false
                    if (res.isSuccess) {
                        onLogin(email.trim(), password)
                    } else {
                        error = res.exceptionOrNull()?.localizedMessage ?: "Login failed"
                    }
                }
            }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(text = if (loading) "Signing in..." else "Sign in")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Create an account")
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AppTheme {
        LoginScreen()
    }
}

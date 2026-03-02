package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.storystream.reader_app.ui.components.Masthead
import com.storystream.reader_app.ui.theme.AppTheme
import kotlinx.coroutines.launch
import com.storystream.reader_app.repository.AuthRepository

@Composable
fun CreateAccountScreen(
    modifier: Modifier = Modifier,
    onCreate: (email: String, password: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canCreate = email.isNotBlank() && password.length >= 6 && password == confirm && !loading
    val repo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(top = 24.dp)
    ) {
        Masthead(title = "Create account", showBack = true, onBack = onBack)

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (min 6)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text("Confirm password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                scope.launch {
                    loading = true
                    error = null
                    val res = repo.register(email.trim(), password)
                    loading = false
                    if (res.isSuccess) {
                        onCreate(email.trim(), password)
                    } else {
                        error = res.exceptionOrNull()?.localizedMessage ?: "Create account failed"
                    }
                }
            }, enabled = canCreate, modifier = Modifier.fillMaxWidth()) {
                Text(text = if (loading) "Creating..." else "Create account")
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
fun CreateAccountPreview() {
    AppTheme {
        CreateAccountScreen()
    }
}

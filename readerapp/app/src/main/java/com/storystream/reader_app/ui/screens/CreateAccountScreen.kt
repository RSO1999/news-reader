package com.storystream.reader_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storystream.reader_app.ui.components.Masthead
import com.storystream.reader_app.ui.theme.AppTheme
import com.storystream.reader_app.ui.viewmodel.AuthEvent
import com.storystream.reader_app.ui.viewmodel.AuthViewModel

@Composable
fun CreateAccountScreen(
    modifier: Modifier = Modifier,
    onCreate: (email: String, password: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val emailValid = email.isBlank() || email.contains("@")
    val passwordMatch = confirm.isBlank() || password == confirm
    val vm: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm.events) {
        vm.events.collect { ev ->
            when (ev) {
                AuthEvent.RegisterSucceeded -> snackbarHostState.showSnackbar("Account created")
                AuthEvent.RegisterFailed -> snackbarHostState.showSnackbar("Create account failed")
                AuthEvent.LoginSucceeded -> Unit
                AuthEvent.LoginFailed -> Unit
            }
        }
    }

    val canCreate = email.isNotBlank() && emailValid && password.length >= 6 && password == confirm && !uiState.loading

    AppTheme {
        Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { scaffoldPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Masthead(title = "Create account", showBack = true, onBack = onBack)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Get started",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create your free account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        isError = email.isNotBlank() && !emailValid,
                        supportingText = if (email.isNotBlank() && !emailValid) {
                            { Text("Enter a valid email", color = MaterialTheme.colorScheme.error) }
                        } else {
                            null
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min 6)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text = if (passwordVisible) "Hide" else "Show",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        isError = password.isNotBlank() && password.length < 6,
                        supportingText = if (password.isNotBlank() && password.length < 6) {
                            { Text("Minimum 6 characters", color = MaterialTheme.colorScheme.error) }
                        } else {
                            null
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = confirm.isNotBlank() && !passwordMatch,
                        supportingText = if (confirm.isNotBlank() && !passwordMatch) {
                            { Text("Passwords don't match", color = MaterialTheme.colorScheme.error) }
                        } else {
                            null
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            vm.register(email.trim(), password) { ok ->
                                if (ok) onCreate(email.trim(), password)
                            }
                        },
                        enabled = canCreate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (uiState.loading) "Creating..." else "Create account",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    uiState.error?.let { message ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
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

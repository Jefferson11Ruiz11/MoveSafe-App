package com.app.riesgo.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.riesgo.R
import com.app.riesgo.ui.theme.SlateDark
import com.app.riesgo.ui.theme.SlateMuted
import com.app.riesgo.util.Validators
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var isResetting by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val loginState by viewModel.loginState.collectAsState()

    val isLoading = loginState is AuthState.Loading
    val authError = (loginState as? AuthState.Error)?.message

    val emailVal = Validators.validateEmail(email)
    val passwordVal = Validators.validatePassword(password)
    val isFormValid = emailVal.isValid && passwordVal.isValid

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .systemBarsPadding()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo MoveSafe
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_movesafe_logo),
                        contentDescription = "Logo MoveSafe",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MoveSafe",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark
            )
            Text(
                text = "Prevención Inteligente de Lesiones",
                fontSize = 14.sp,
                color = SlateMuted
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Correo Electrónico
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it.trim()
                    if (authError != null) viewModel.clearErrors()
                },
                label = { Text("Correo Electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = (email.isNotEmpty() && !emailVal.isValid) || authError != null,
                supportingText = {
                    if (email.isNotEmpty() && !emailVal.isValid) {
                        Text(emailVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Contraseña con botón de visibilidad
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (authError != null) viewModel.clearErrors()
                },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = (password.isNotEmpty() && !passwordVal.isValid) || authError != null,
                supportingText = {
                    if (password.isNotEmpty() && !passwordVal.isValid) {
                        Text(passwordVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            // ¿Olvidaste tu contraseña?
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        resetEmail = email
                        resetMessage = null
                        showResetDialog = true
                    },
                    enabled = !isLoading
                ) {
                    Text("¿Olvidaste tu contraseña?", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isFormValid && !isLoading) {
                        viewModel.login(
                            email = email,
                            password = password,
                            onSuccess = onLoginSuccess
                        )
                    }
                },
                enabled = isFormValid && !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Iniciar Sesión", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToRegister,
                enabled = !isLoading
            ) {
                Text("¿No tienes cuenta? Regístrate aquí", color = MaterialTheme.colorScheme.primary)
            }
        }

        // Diálogo para reestablecer contraseña
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { if (!isResetting) showResetDialog = false },
                title = { Text("Reestablecer Contraseña", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Ingresa tu correo registrado para enviarte un enlace de recuperación seguro.",
                            fontSize = 13.sp,
                            color = SlateMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it.trim() },
                            label = { Text("Correo Electrónico") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isResetting
                        )
                        if (resetMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = resetMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (resetEmail.isNotBlank()) {
                                isResetting = true
                                viewModel.resetPassword(resetEmail) { success, msg ->
                                    isResetting = false
                                    if (success) {
                                        showResetDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    } else {
                                        resetMessage = msg
                                    }
                                }
                            } else {
                                resetMessage = "Ingresa un correo electrónico válido."
                            }
                        },
                        enabled = !isResetting
                    ) {
                        if (isResetting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Enviar Enlace")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showResetDialog = false },
                        enabled = !isResetting
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

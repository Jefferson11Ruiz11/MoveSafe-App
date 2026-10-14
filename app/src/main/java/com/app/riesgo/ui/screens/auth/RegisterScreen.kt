package com.app.riesgo.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.riesgo.ui.theme.SlateDark
import com.app.riesgo.ui.theme.SlateMuted
import com.app.riesgo.util.Validators

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var deporte by remember { mutableStateOf("Fútbol") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val registerState by viewModel.registerState.collectAsState()

    val isLoading = registerState is AuthState.Loading
    val authError = (registerState as? AuthState.Error)?.message

    // Evaluaciones reactivas
    val nombreVal = Validators.validateName(nombre)
    val emailVal = Validators.validateEmail(email)
    val passwordVal = Validators.validatePassword(password)
    val confirmPasswordVal = Validators.validateConfirmPassword(password, confirmPassword)
    val edadVal = Validators.validateAge(edad)

    val isFormValid = nombreVal.isValid &&
            emailVal.isValid &&
            passwordVal.isValid &&
            confirmPasswordVal.isValid &&
            edadVal.isValid &&
            deporte.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Atleta / Evaluador") },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .systemBarsPadding()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Crear Perfil de Salud",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Tus datos permitirán personalizar los algoritmos de riesgo MoveSafe",
                fontSize = 13.sp,
                color = SlateMuted
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Nombre Completo
            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = Validators.sanitizeName(it)
                    if (authError != null) viewModel.clearErrors()
                },
                label = { Text("Nombre Completo") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = nombre.isNotEmpty() && !nombreVal.isValid,
                supportingText = {
                    if (nombre.isNotEmpty() && !nombreVal.isValid) {
                        Text(nombreVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it.trim()
                    if (authError != null) viewModel.clearErrors()
                },
                label = { Text("Correo Electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = email.isNotEmpty() && !emailVal.isValid,
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

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (authError != null) viewModel.clearErrors()
                },
                label = { Text("Contraseña (mínimo 6 caracteres)") },
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
                isError = password.isNotEmpty() && !passwordVal.isValid,
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

            Spacer(modifier = Modifier.height(8.dp))

            // Confirmar Contraseña
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (authError != null) viewModel.clearErrors()
                },
                label = { Text("Confirmar Contraseña") },
                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                        Icon(
                            imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isConfirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = confirmPassword.isNotEmpty() && !confirmPasswordVal.isValid,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && !confirmPasswordVal.isValid) {
                        Text(confirmPasswordVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Edad
            OutlinedTextField(
                value = edad,
                onValueChange = {
                    edad = Validators.sanitizeAge(it)
                    if (authError != null) viewModel.clearErrors()
                },
                label = { Text("Edad (10 - 90 años)") },
                leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = edad.isNotEmpty() && !edadVal.isValid,
                supportingText = {
                    if (edad.isNotEmpty() && !edadVal.isValid) {
                        Text(edadVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Disciplina Deportiva Principal
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Disciplina Deportiva Principal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = deporte == "Fútbol",
                        onClick = { deporte = "Fútbol" },
                        label = { Text("Fútbol", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )

                    FilterChip(
                        selected = deporte == "Vóley",
                        onClick = { deporte = "Vóley" },
                        label = { Text("Vóley", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.SportsVolleyball, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )

                    FilterChip(
                        selected = deporte == "General",
                        onClick = { deporte = "General" },
                        label = { Text("General", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
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

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isFormValid && !isLoading) {
                        viewModel.register(
                            nombre = nombre,
                            email = email,
                            password = password,
                            deporte = deporte,
                            edad = edad.toIntOrNull() ?: 25,
                            onSuccess = onRegisterSuccess
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
                    Text("Registrar y Continuar", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

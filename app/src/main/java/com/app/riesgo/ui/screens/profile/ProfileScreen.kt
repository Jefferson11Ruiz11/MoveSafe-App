package com.app.riesgo.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.riesgo.data.repository.AppRepository
import com.app.riesgo.data.repository.AuthRepository
import com.app.riesgo.ui.components.GlassCard
import com.app.riesgo.ui.theme.SlateDark
import com.app.riesgo.ui.theme.SlateMuted
import com.app.riesgo.util.ThemeManager
import com.app.riesgo.util.ThemeMode
import com.app.riesgo.util.Validators
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit
) {
    val user = AppRepository.currentUser
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var nombre by remember(user) { mutableStateOf(user?.nombre ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
    var edad by remember(user) { mutableStateOf(user?.edad?.toString() ?: "25") }
    var peso by remember(user) { mutableStateOf(user?.peso?.toString() ?: "70") }
    var altura by remember(user) { mutableStateOf(user?.altura?.toString() ?: "175") }
    var deporte by remember(user) { mutableStateOf(user?.deporte ?: "Fútbol") }
    var tieneLesiones by remember(user?.tieneLesiones) { mutableStateOf(user?.tieneLesiones ?: false) }

    var isLoading by remember { mutableStateOf(false) }

    val currentThemeMode by ThemeManager.themeMode.collectAsState()

    val nombreVal = Validators.validateName(nombre)
    val edadVal = Validators.validateAge(edad)
    val pesoVal = Validators.validateWeight(peso)
    val alturaVal = Validators.validateHeight(altura)

    val isFormValid = nombreVal.isValid && edadVal.isValid && pesoVal.isValid && alturaVal.isValid && deporte.isNotBlank()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Perfil del Atleta (MoveSafe)", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            AuthRepository.logout()
                            onLogout()
                        },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = MaterialTheme.colorScheme.error
                        )
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header del perfil
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = nombre.take(1).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (nombre.isNotBlank()) nombre else "Atleta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(text = email, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = deporte,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Sección Accesibilidad y Apariencia
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentThemeMode == ThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Tema",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accesibilidad y Apariencia", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Personaliza el modo visual de la aplicación", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = { ThemeManager.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text("Claro", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = currentThemeMode == ThemeMode.DARK,
                        onClick = { ThemeManager.setThemeMode(ThemeMode.DARK) },
                        label = { Text("Oscuro", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = { ThemeManager.setThemeMode(ThemeMode.SYSTEM) },
                        label = { Text("Sistema", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.SettingsSuggest, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Formulario de Edición
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Información Personal y Biométricos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Nombre
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = Validators.sanitizeName(it) },
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

                    // Email (Lectura)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        enabled = false,
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Selector Estricto de Disciplina
                    Text(
                        "Disciplina Deportiva Principal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                            selected = deporte == "General" || deporte == "General / Ninguno",
                            onClick = { deporte = "General" },
                            label = { Text("General", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        )
                    }

                    // Edad y Peso
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = edad,
                            onValueChange = { edad = Validators.sanitizeAge(it) },
                            label = { Text("Edad (10 - 90)") },
                            leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = edad.isNotEmpty() && !edadVal.isValid,
                            supportingText = {
                                if (edad.isNotEmpty() && !edadVal.isValid) {
                                    Text(edadVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isLoading
                        )

                        OutlinedTextField(
                            value = peso,
                            onValueChange = { peso = Validators.sanitizeWeight(it) },
                            label = { Text("Peso (30 - 250 kg)") },
                            leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = peso.isNotEmpty() && !pesoVal.isValid,
                            supportingText = {
                                if (peso.isNotEmpty() && !pesoVal.isValid) {
                                    Text(pesoVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isLoading
                        )
                    }

                    // Altura
                    OutlinedTextField(
                        value = altura,
                        onValueChange = { altura = Validators.sanitizeHeight(it) },
                        label = { Text("Estatura (100 - 230 cm)") },
                        leadingIcon = { Icon(Icons.Default.Height, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = altura.isNotEmpty() && !alturaVal.isValid,
                        supportingText = {
                            if (altura.isNotEmpty() && !alturaVal.isValid) {
                                Text(alturaVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading
                    )

                    // Switch Sincronizado de Historial de Lesiones
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("¿Historial de lesiones previas?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Sincronizado con la pantalla de evaluación", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = tieneLesiones,
                            onCheckedChange = { newChecked ->
                                tieneLesiones = newChecked
                                scope.launch {
                                    AuthRepository.updateUserProfile(
                                        nombre = nombre,
                                        deporte = deporte,
                                        edad = edad.toIntOrNull() ?: 25,
                                        peso = peso.toFloatOrNull() ?: 70f,
                                        altura = altura.toFloatOrNull() ?: 175f,
                                        tieneLesiones = newChecked
                                    )
                                }
                            },
                            enabled = !isLoading
                        )
                    }
                }
            }

            // Botón Guardar Cambios
            Button(
                onClick = {
                    if (isFormValid && !isLoading) {
                        isLoading = true
                        scope.launch {
                            AuthRepository.updateUserProfile(
                                nombre = nombre,
                                deporte = deporte,
                                edad = edad.toInt(),
                                peso = peso.toFloat(),
                                altura = altura.toFloat(),
                                tieneLesiones = tieneLesiones
                            )
                            isLoading = false
                            snackbarHostState.showSnackbar("Perfil MoveSafe actualizado correctamente")
                        }
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
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Cambios", fontWeight = FontWeight.SemiBold)
                }
            }

            // Botón de Cerrar Sesión
            OutlinedButton(
                onClick = {
                    AuthRepository.logout()
                    onLogout()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

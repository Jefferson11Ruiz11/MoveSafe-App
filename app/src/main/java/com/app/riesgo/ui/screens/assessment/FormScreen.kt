package com.app.riesgo.ui.screens.assessment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.riesgo.data.model.SportType
import com.app.riesgo.data.repository.AppRepository
import com.app.riesgo.data.repository.AuthRepository
import com.app.riesgo.ui.components.GlassCard
import com.app.riesgo.util.Validators
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    onNavigateToSelection: (edad: Int, peso: Float, altura: Float, tieneLesiones: Boolean) -> Unit,
    onNavigateToSurvey: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val user = AppRepository.currentUser
    val scope = rememberCoroutineScope()

    var edad by remember(user) { mutableStateOf(user?.edad?.toString() ?: "25") }
    var peso by remember(user) { mutableStateOf(user?.peso?.toString() ?: "70") }
    var altura by remember(user) { mutableStateOf(user?.altura?.toString() ?: "175") }
    var tieneLesiones by remember(user?.tieneLesiones) { mutableStateOf(user?.tieneLesiones ?: false) }
    var selectedSport by remember(user) {
        mutableStateOf(SportType.fromString(user?.deporte ?: "Fútbol"))
    }

    val edadVal = Validators.validateAge(edad)
    val pesoVal = Validators.validateWeight(peso)
    val alturaVal = Validators.validateHeight(altura)

    val isFormValid = edadVal.isValid && pesoVal.isValid && alturaVal.isValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Datos Biométricos (MoveSafe)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(user?.nombre ?: "Atleta Activo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Historial")
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
            // Card de Selección de Deporte
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Disciplina Deportiva Evaluada", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Selecciona la disciplina para aplicar el motor de reglas biomecánicas correspondiente",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSport == SportType.FUTBOL,
                            onClick = {
                                selectedSport = SportType.FUTBOL
                                scope.launch {
                                    AuthRepository.updateUserProfile(
                                        nombre = user?.nombre ?: "Atleta",
                                        deporte = "Fútbol",
                                        edad = edad.toIntOrNull() ?: 25,
                                        peso = peso.toFloatOrNull() ?: 70f,
                                        altura = altura.toFloatOrNull() ?: 175f,
                                        tieneLesiones = tieneLesiones
                                    )
                                }
                            },
                            label = { Text("Fútbol") },
                            leadingIcon = { Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedSport == SportType.VOLEY,
                            onClick = {
                                selectedSport = SportType.VOLEY
                                scope.launch {
                                    AuthRepository.updateUserProfile(
                                        nombre = user?.nombre ?: "Atleta",
                                        deporte = "Vóley",
                                        edad = edad.toIntOrNull() ?: 25,
                                        peso = peso.toFloatOrNull() ?: 70f,
                                        altura = altura.toFloatOrNull() ?: 175f,
                                        tieneLesiones = tieneLesiones
                                    )
                                }
                            },
                            label = { Text("Vóley") },
                            leadingIcon = { Icon(Icons.Default.SportsVolleyball, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedSport == SportType.GENERAL,
                            onClick = {
                                selectedSport = SportType.GENERAL
                                scope.launch {
                                    AuthRepository.updateUserProfile(
                                        nombre = user?.nombre ?: "Atleta",
                                        deporte = "General",
                                        edad = edad.toIntOrNull() ?: 25,
                                        peso = peso.toFloatOrNull() ?: 70f,
                                        altura = altura.toFloatOrNull() ?: 175f,
                                        tieneLesiones = tieneLesiones
                                    )
                                }
                            },
                            label = { Text("General") },
                            leadingIcon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Parámetros Antropométricos", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

                    // Edad
                    OutlinedTextField(
                        value = edad,
                        onValueChange = { edad = Validators.sanitizeAge(it) },
                        label = { Text("Edad cronológica (10 - 90 años)") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = edad.isNotEmpty() && !edadVal.isValid,
                        supportingText = {
                            if (edad.isNotEmpty() && !edadVal.isValid) {
                                Text(edadVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Peso
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { peso = Validators.sanitizeWeight(it) },
                        label = { Text("Peso corporal (30.0 - 250.0 kg)") },
                        leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = peso.isNotEmpty() && !pesoVal.isValid,
                        supportingText = {
                            if (peso.isNotEmpty() && !pesoVal.isValid) {
                                Text(pesoVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Altura
                    OutlinedTextField(
                        value = altura,
                        onValueChange = { altura = Validators.sanitizeHeight(it) },
                        label = { Text("Estatura (100.0 - 230.0 cm)") },
                        leadingIcon = { Icon(Icons.Default.Height, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = altura.isNotEmpty() && !alturaVal.isValid,
                        supportingText = {
                            if (altura.isNotEmpty() && !alturaVal.isValid) {
                                Text(alturaVal.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Historial de Lesiones y Cuestionario Dinámico
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Historial de lesiones previas", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Esguinces, desgarros o cirugías en los últimos 12 meses",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = tieneLesiones,
                            onCheckedChange = { newChecked ->
                                tieneLesiones = newChecked
                                scope.launch {
                                    AuthRepository.updateUserProfile(
                                        nombre = user?.nombre ?: "Atleta",
                                        deporte = selectedSport.displayName,
                                        edad = edad.toIntOrNull() ?: 25,
                                        peso = peso.toFloatOrNull() ?: 70f,
                                        altura = altura.toFloatOrNull() ?: 175f,
                                        tieneLesiones = newChecked
                                    )
                                }
                            }
                        )
                    }

                    if (tieneLesiones) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val survey = AppRepository.currentSurveyResult
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Árbol de Decisión de Lesiones (Adaptativo)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (survey != null && survey.zonaAnatomica.isNotBlank()) {
                                    Text(
                                        "✔ Zona: ${survey.zonaAnatomica} (${survey.mecanismoLesion})",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Índice de Vulnerabilidad: ${survey.indiceVulnerabilidad.toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = onNavigateToSurvey,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Modificar Respuestas", fontSize = 13.sp)
                                    }
                                } else {
                                    Text(
                                        "Responde el cuestionario interactivo para estimar la vulnerabilidad articular específica.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onNavigateToSurvey,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Completar Cuestionario Dinámico", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        scope.launch {
                            AuthRepository.updateUserProfile(
                                nombre = user?.nombre ?: "Atleta",
                                deporte = selectedSport.displayName,
                                edad = edad.toInt(),
                                peso = peso.toFloat(),
                                altura = altura.toFloat(),
                                tieneLesiones = tieneLesiones
                            )
                        }
                        onNavigateToSelection(
                            edad.toInt(),
                            peso.toFloat(),
                            altura.toFloat(),
                            tieneLesiones
                        )
                    }
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Continuar a Selección de Prueba", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

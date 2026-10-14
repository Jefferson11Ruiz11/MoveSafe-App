package com.app.riesgo.ui.screens.assessment

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.riesgo.data.model.DynamicSurveyResult
import com.app.riesgo.data.model.SportType
import com.app.riesgo.data.repository.AppRepository
import com.app.riesgo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InjurySurveyScreen(
    onComplete: (DynamicSurveyResult) -> Unit,
    onBack: () -> Unit
) {
    val user = AppRepository.currentUser
    val sportType = SportType.fromString(user?.deporte ?: "Fútbol")

    var zona by remember { mutableStateOf("") }
    var mecanismo by remember { mutableStateOf("") }
    var antiguedad by remember { mutableStateOf("") }
    var estadoRecuperacion by remember { mutableStateOf("") }
    var condicionFinal by remember { mutableStateOf("") }

    // Opciones para Pregunta 1 (Zona Anatómica según Deporte)
    val zonaOptions = when (sportType) {
        SportType.FUTBOL -> listOf("Rodilla (LCA/Menisco)", "Tobillo lateral", "Isquiotibiales/Muslo", "Aductores/Pubis")
        SportType.VOLEY -> listOf("Rodilla (Tendón rotuliano)", "Hombro dominante", "Tobillo", "Dedos/Muñeca")
        SportType.GENERAL -> listOf("Rodilla", "Tobillo", "Hombro", "Espalda / Lumbar")
    }

    // Opciones para Pregunta 2 (Mecanismo condicionado por Zona)
    val mecanismoOptions = when {
        zona.contains("Rodilla", ignoreCase = true) -> listOf("Pivoteo o giro brusco", "Aterrizaje tras salto", "Golpe/Traumatismo directo")
        zona.contains("Hombro", ignoreCase = true) -> listOf("Dolor acumulativo al rematar/sacar", "Traumatismo en bloqueo", "Sensación de luxación/inestabilidad")
        zona.contains("Tobillo", ignoreCase = true) -> listOf("Torcedura al pisar el pie de un rival/compañero", "Pisar mal en césped/superficie irregular")
        else -> listOf("Sobrecarga o esfuerzo muscular intenso", "Traumatismo o estiramiento brusco")
    }

    // Opciones para Pregunta 3 (Antigüedad)
    val antiguedadOptions = listOf("Menos de 3 meses", "Entre 3 y 6 meses", "Más de 6 meses / Crónica")

    // Opciones para Pregunta 4 (Estado de Recuperación)
    val recuperacionOptions = listOf(
        "Fisioterapia completa y alta médica",
        "Recuperación empírica/incompleta",
        "Sigo en tratamiento"
    )

    // Pregunta 5 (Condicional según P4)
    val isAltaMedica = estadoRecuperacion == "Fisioterapia completa y alta médica"
    val p5Title = if (isAltaMedica) {
        "¿Sientes seguridad total al 100% al competir?"
    } else {
        "¿Presentas dolor o limitación de movimiento en entrenamientos actuales?"
    }
    val p5Options = if (isAltaMedica) {
        listOf("Sí, totalmente seguro", "No, siento cierto temor o desconfianza")
    } else {
        listOf("Sí, dolor/limitación frecuente", "Ocasional", "No, asintomático actualmente")
    }

    // Cálculo en tiempo real del Índice de Vulnerabilidad
    val vulnerabilityIndex = remember(sportType, zona, mecanismo, antiguedad, estadoRecuperacion, condicionFinal) {
        computeVulnerability(sportType, zona, mecanismo, antiguedad, estadoRecuperacion, condicionFinal)
    }

    val isFormComplete = zona.isNotBlank() &&
            mecanismo.isNotBlank() &&
            antiguedad.isNotBlank() &&
            estadoRecuperacion.isNotBlank() &&
            condicionFinal.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Árbol de Decisión de Lesiones", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            // Header: Disciplina Deportiva
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (sportType == SportType.FUTBOL) Icons.Default.SportsSoccer else Icons.Default.SportsVolleyball,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Evaluación Adaptativa para ${sportType.displayName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tus respuestas adaptarán el modelo biomecánico de riesgo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Pregunta 1: Zona Anatómica
            QuestionSection(
                stepNumber = "1",
                title = "Zona Anatómica Afectada (${sportType.displayName})",
                options = zonaOptions,
                selectedOption = zona,
                onSelect = {
                    zona = it
                    mecanismo = ""
                    antiguedad = ""
                    estadoRecuperacion = ""
                    condicionFinal = ""
                }
            )

            // Pregunta 2: Mecanismo de Lesión (P1 respondida)
            AnimatedVisibility(
                visible = zona.isNotBlank(),
                enter = fadeIn(tween(300)) + expandVertically(tween(300))
            ) {
                QuestionSection(
                    stepNumber = "2",
                    title = "Mecanismo Biomecánico de Lesión",
                    options = mecanismoOptions,
                    selectedOption = mecanismo,
                    onSelect = {
                        mecanismo = it
                        antiguedad = ""
                        estadoRecuperacion = ""
                        condicionFinal = ""
                    }
                )
            }

            // Pregunta 3: Antigüedad de la Lesión (P2 respondida)
            AnimatedVisibility(
                visible = mecanismo.isNotBlank(),
                enter = fadeIn(tween(300)) + expandVertically(tween(300))
            ) {
                QuestionSection(
                    stepNumber = "3",
                    title = "Antigüedad / Cronicidad de la Lesión",
                    options = antiguedadOptions,
                    selectedOption = antiguedad,
                    onSelect = {
                        antiguedad = it
                        estadoRecuperacion = ""
                        condicionFinal = ""
                    }
                )
            }

            // Pregunta 4: Estado de Recuperación (P3 respondida)
            AnimatedVisibility(
                visible = antiguedad.isNotBlank(),
                enter = fadeIn(tween(300)) + expandVertically(tween(300))
            ) {
                QuestionSection(
                    stepNumber = "4",
                    title = "Estado de Rehabilitación y Alta Médica",
                    options = recuperacionOptions,
                    selectedOption = estadoRecuperacion,
                    onSelect = {
                        estadoRecuperacion = it
                        condicionFinal = ""
                    }
                )
            }

            // Pregunta 5: Condicional según P4 (P4 respondida)
            AnimatedVisibility(
                visible = estadoRecuperacion.isNotBlank(),
                enter = fadeIn(tween(300)) + expandVertically(tween(300))
            ) {
                QuestionSection(
                    stepNumber = "5",
                    title = p5Title,
                    options = p5Options,
                    selectedOption = condicionFinal,
                    onSelect = { condicionFinal = it }
                )
            }

            // Panel de Resultados de Vulnerabilidad y Botón Final
            AnimatedVisibility(
                visible = isFormComplete,
                enter = fadeIn(tween(400)) + expandVertically(tween(400))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Índice de Vulnerabilidad Calculado:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "${vulnerabilityIndex.toInt()}%",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = if (vulnerabilityIndex >= 50f) RiskHigh else RiskModerate
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Este índice reponderará el análisis articular en $zona durante las pruebas físicas.",
                                fontSize = 12.sp,
                                color = SlateMuted
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val result = DynamicSurveyResult(
                                sportName = sportType.displayName,
                                zonaAnatomica = zona,
                                mecanismoLesion = mecanismo,
                                antiguedad = antiguedad,
                                estadoRecuperacion = estadoRecuperacion,
                                condicionSeguridadO_Dolor = condicionFinal,
                                indiceVulnerabilidad = vulnerabilityIndex
                            )
                            AppRepository.currentSurveyResult = result
                            onComplete(result)
                        },
                        enabled = isFormComplete,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Completar Cuestionario", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionSection(
    stepNumber: String,
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stepNumber,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SlateDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val isSelected = selectedOption == option
                    OutlinedCard(
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, SlateBorder)
                        },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else SlateDark,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(option) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun computeVulnerability(
    sport: SportType,
    zona: String,
    mecanismo: String,
    antiguedad: String,
    estado: String,
    condicion: String
): Float {
    if (zona.isBlank()) return 0f
    var vul = 15f

    when (antiguedad) {
        "Menos de 3 meses" -> vul += 25f
        "Entre 3 y 6 meses" -> vul += 20f
        "Más de 6 meses / Crónica" -> vul += 15f
    }

    when (estado) {
        "Fisioterapia completa y alta médica" -> vul += 10f
        "Recuperación empírica/incompleta" -> vul += 35f
        "Sigo en tratamiento" -> vul += 40f
    }

    when (condicion) {
        "No, siento cierto temor o desconfianza" -> vul += 20f
        "Sí, dolor/limitación frecuente" -> vul += 30f
        "Ocasional" -> vul += 15f
        "Sí, totalmente seguro", "No, asintomático actualmente" -> vul += 0f
    }

    if (sport == SportType.FUTBOL && (mecanismo.contains("Pivoteo") || zona.contains("Isquiotibiales"))) {
        vul += 15f
    }
    if (sport == SportType.VOLEY && (zona.contains("Hombro") || zona.contains("Tendón rotuliano"))) {
        vul += 15f
    }

    return vul.coerceIn(0f, 100f)
}

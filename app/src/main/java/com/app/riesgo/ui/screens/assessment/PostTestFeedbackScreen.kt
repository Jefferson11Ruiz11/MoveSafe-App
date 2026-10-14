package com.app.riesgo.ui.screens.assessment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.riesgo.data.model.CameraBiomechanicalMetrics
import com.app.riesgo.data.model.PostTestFeedback
import com.app.riesgo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostTestFeedbackScreen(
    cameraMetrics: CameraBiomechanicalMetrics,
    onCompleteFeedback: (PostTestFeedback) -> Unit,
    onBack: () -> Unit = {}
) {
    var painScale by remember { mutableFloatStateOf(0f) }
    var fatigueZone by remember { mutableStateOf("Ninguna") }
    var stabilityFeeling by remember { mutableStateOf("Seguro / Estable") }
    var ankleDiscomfort by remember { mutableStateOf("Ninguna") }
    var muscleWeakness by remember { mutableStateOf(false) }

    val fatigueOptions = listOf("Rodillas", "Tobillos", "Espalda baja", "Ninguna")
    val stabilityOptions = listOf("Seguro / Estable", "Ligera inestabilidad", "Muy inestable / Inseguro")
    val ankleOptions = listOf("Ninguna", "Leve", "Inseguridad marcada")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuestionario Post-Prueba (MoveSafe)", fontWeight = FontWeight.Bold) }
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
            // Resumen de métricas de cámara
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Métricas Biomecánicas Registradas por Cámara",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (cameraMetrics.isBalanceTest) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• Apoyo Izq: ${cameraMetrics.tiempoSostenidoIzquierdo.toInt()}s / 10s", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("• Apoyo Der: ${cameraMetrics.tiempoSostenidoDerecho.toInt()}s / 10s", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• Índice de Oscilación Lateral: ${(cameraMetrics.indiceOscilacion * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• Repeticiones: ${cameraMetrics.repsCount}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("• Ángulo Mín: ${cameraMetrics.minKneeAngle.toInt()}°", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        if (cameraMetrics.valgusCollapsesCount > 0) {
                            Text(
                                "• Colapsos en valgo detectados: ${cameraMetrics.valgusCollapsesCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RiskHigh
                            )
                        }
                    }
                }
            }

            // Pregunta de molestias en tobillo (Específica para Equilibrio Monopodal)
            if (cameraMetrics.isBalanceTest) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "¿Presentó molestias o sensación de torcedura en el tobillo evaluado?",
                            fontWeight = FontWeight.SemiBold,
                            color = SlateDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ankleOptions.forEach { opt ->
                            FilterChip(
                                selected = ankleDiscomfort == opt,
                                onClick = { ankleDiscomfort = opt },
                                label = { Text(opt, fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("¿Experimentó temblor o debilidad en cuádriceps/glúteo medio?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SlateDark)
                        }
                        Switch(
                            checked = muscleWeakness,
                            onCheckedChange = { muscleWeakness = it }
                        )
                    }
                }
            }

            // Pregunta 1: Escala de dolor EVA
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Nivel de dolor o molestia articular durante la prueba", fontWeight = FontWeight.SemiBold, color = SlateDark)
                    Text("Escala de 0 (Sin molestia) a 10 (Dolor intenso)", fontSize = 12.sp, color = SlateMuted)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Valor: ${painScale.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SlateDark)
                        Text(
                            text = when {
                                painScale == 0f -> "Sin dolor"
                                painScale <= 3f -> "Leve"
                                painScale <= 6f -> "Moderado"
                                else -> "Intenso"
                            },
                            color = if (painScale >= 4f) RiskHigh else RiskLow,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = painScale,
                        onValueChange = { painScale = it },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                }
            }

            // Pregunta 2: Zona de fatiga
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. ¿En qué zona sintió mayor sobrecarga o fatiga?", fontWeight = FontWeight.SemiBold, color = SlateDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fatigueOptions.take(2).forEach { opt ->
                            FilterChip(
                                selected = fatigueZone == opt,
                                onClick = { fatigueZone = opt },
                                label = { Text(opt, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fatigueOptions.drop(2).forEach { opt ->
                            FilterChip(
                                selected = fatigueZone == opt,
                                onClick = { fatigueZone = opt },
                                label = { Text(opt, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Pregunta 3: Estabilidad
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("3. Sensación subjetiva de estabilidad en las articulaciones", fontWeight = FontWeight.SemiBold, color = SlateDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    stabilityOptions.forEach { opt ->
                        FilterChip(
                            selected = stabilityFeeling == opt,
                            onClick = { stabilityFeeling = opt },
                            label = { Text(opt, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val feedback = PostTestFeedback(
                        painScale = painScale.toInt(),
                        fatigueZone = fatigueZone,
                        stabilityFeeling = stabilityFeeling,
                        ankleDiscomfort = ankleDiscomfort,
                        muscleWeaknessFeeling = muscleWeakness
                    )
                    onCompleteFeedback(feedback)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar y Generar Reporte Final", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

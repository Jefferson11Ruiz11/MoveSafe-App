package com.app.riesgo.ui.screens.imc

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.riesgo.data.repository.AppRepository
import com.app.riesgo.data.repository.RiskEngine
import com.app.riesgo.ui.components.GlassCard
import com.app.riesgo.ui.theme.*
import com.app.riesgo.util.Validators
import java.util.Locale

// Categorías de la OMS
data class WhoImcCategory(
    val nombre: String,
    val rango: String,
    val color: Color,
    val recomendaciones: List<String>,
    val impactoArticular: String
)

object WhoImcStandards {
    val Underweight = WhoImcCategory(
        nombre = "Bajo Peso",
        rango = "< 18.5 kg/m²",
        color = Color(0xFF3B82F6),
        recomendaciones = listOf(
            "Incrementar masa muscular magra mediante hipertrofia y trabajo de fuerza.",
            "Asegurar aporte proteico e hídrico adecuado pre y post entrenamiento.",
            "Supervisar densidad mineral ósea y estabilidad estructural articular."
        ),
        impactoArticular = "Menor amortiguación muscular ante impactos pliométricos; mayor vulnerabilidad tendinosa."
    )

    val Normal = WhoImcCategory(
        nombre = "Peso Normal",
        rango = "18.5 - 24.9 kg/m²",
        color = Color(0xFF10B981),
        recomendaciones = listOf(
            "Mantener balance energético e hídrico óptimo.",
            "Combinar trabajo pliométrico con fortalecimiento de cadena posterior.",
            "Mantenimiento de movilidad articular en caderas y tobillos."
        ),
        impactoArticular = "Carga mecánica fisiológica idónea para la preservación de cartílago y meniscos."
    )

    val Overweight = WhoImcCategory(
        nombre = "Sobrepeso",
        rango = "25.0 - 29.9 kg/m²",
        color = Color(0xFFF59E0B),
        recomendaciones = listOf(
            "Priorizar ejercicios de acondicionamiento de bajo impacto (ciclismo, natación).",
            "Fortalecer estabilizadores de rodilla (cuádriceps y glúteo medio).",
            "Monitorear signos de sobrecarga patelar o fascitis plantar."
        ),
        impactoArticular = "Cada kg adicional genera entre 3 y 4 kg de fuerza de compresión sobre las rodillas en cada paso."
    )

    val Obesity = WhoImcCategory(
        nombre = "Obesidad",
        rango = "≥ 30.0 kg/m²",
        color = Color(0xFFEF4444),
        recomendaciones = listOf(
            "Evitar deportes con saltos repetitivos o desaceleraciones bruscas sin adaptar.",
            "Trabajo coordinado con nutricionista y kinesiólogo.",
            "Priorizar ejercicios de fuerza analítica sin carga axial excesiva."
        ),
        impactoArticular = "Elevada compresión en tobillos y columna lumbar; mayor riesgo de degeneración de cartílago articular."
    )

    fun getCategory(imc: Float): WhoImcCategory {
        return when {
            imc < 18.5f -> Underweight
            imc < 25.0f -> Normal
            imc < 30.0f -> Overweight
            else -> Obesity
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImcScreen() {
    val user = AppRepository.currentUser

    var pesoStr by remember(user) { mutableStateOf(user?.peso?.toString() ?: "70") }
    var alturaStr by remember(user) { mutableStateOf(user?.altura?.toString() ?: "175") }

    val pesoVal = Validators.validateWeight(pesoStr)
    val alturaVal = Validators.validateHeight(alturaStr)

    val peso = pesoStr.toFloatOrNull() ?: 70f
    val altura = alturaStr.toFloatOrNull() ?: 175f

    val imc = RiskEngine.calculateImc(peso, altura)
    val category = WhoImcStandards.getCategory(imc)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis de IMC (OMS)", fontWeight = FontWeight.Bold) }
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Principal de Resultado IMC
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Índice de Masa Corporal", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f", imc)} kg/m²",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = category.color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, category.color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = "${category.nombre} (${category.rango})",
                            color = category.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Componente Gráfico Segmentado OMS
                    BmiSegmentedBarGraph(currentImc = imc)
                }
            }

            // Entradas para ajustar Peso y Altura en tiempo real
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ajustar Parámetros de Cálculo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = pesoStr,
                        onValueChange = { pesoStr = Validators.sanitizeWeight(it) },
                        label = { Text("Peso (kg)") },
                        leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = pesoStr.isNotEmpty() && !pesoVal.isValid,
                        supportingText = {
                            if (pesoStr.isNotEmpty() && !pesoVal.isValid) {
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
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = alturaStr,
                        onValueChange = { alturaStr = Validators.sanitizeHeight(it) },
                        label = { Text("Estatura (cm)") },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = alturaStr.isNotEmpty() && !alturaVal.isValid,
                        supportingText = {
                            if (alturaStr.isNotEmpty() && !alturaVal.isValid) {
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
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Card de Impacto Biomecánico Articular
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = category.color)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Impacto en Sobrecarga Articular", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(category.impactoArticular, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Card de Recomendaciones OMS
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recomendaciones de la OMS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(10.dp))

                category.recomendaciones.forEach { rec ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        Text("•", fontWeight = FontWeight.Bold, color = category.color, modifier = Modifier.padding(end = 8.dp))
                        Text(rec, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun BmiSegmentedBarGraph(currentImc: Float) {
    val animatedImc by animateFloatAsState(
        targetValue = currentImc,
        animationSpec = tween(durationMillis = 800),
        label = "imcAnimation"
    )

    val pointerColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            val barHeight = 16.dp
            val clampedImc = animatedImc.coerceIn(15f, 35f)
            val progressRatio = (clampedImc - 15f) / (35f - 15f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasBarHeight = barHeight.toPx()
                val topY = 28.dp.toPx()

                val segmentWidths = listOf(
                    canvasWidth * 0.175f,
                    canvasWidth * 0.325f,
                    canvasWidth * 0.25f,
                    canvasWidth * 0.25f
                )

                val segmentColors = listOf(
                    WhoImcStandards.Underweight.color,
                    WhoImcStandards.Normal.color,
                    WhoImcStandards.Overweight.color,
                    WhoImcStandards.Obesity.color
                )

                var currentX = 0f
                val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

                segmentWidths.forEachIndexed { index, segWidth ->
                    drawRoundRect(
                        color = segmentColors[index],
                        topLeft = Offset(currentX, topY),
                        size = Size(segWidth - 4.dp.toPx(), canvasBarHeight),
                        cornerRadius = cornerRadius
                    )
                    currentX += segWidth
                }

                val indicatorX = canvasWidth * progressRatio
                val trianglePath = Path().apply {
                    moveTo(indicatorX, topY - 2.dp.toPx())
                    lineTo(indicatorX - 12.dp.toPx(), topY - 16.dp.toPx())
                    lineTo(indicatorX + 12.dp.toPx(), topY - 16.dp.toPx())
                    close()
                }

                drawPath(path = trianglePath, color = pointerColor)
            }
        }

        // Etiquetas de rangos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("< 18.5", fontSize = 10.sp, color = WhoImcStandards.Underweight.color, fontWeight = FontWeight.Bold)
            Text("18.5 - 24.9", fontSize = 10.sp, color = WhoImcStandards.Normal.color, fontWeight = FontWeight.Bold)
            Text("25.0 - 29.9", fontSize = 10.sp, color = WhoImcStandards.Overweight.color, fontWeight = FontWeight.Bold)
            Text("≥ 30.0", fontSize = 10.sp, color = WhoImcStandards.Obesity.color, fontWeight = FontWeight.Bold)
        }
    }
}

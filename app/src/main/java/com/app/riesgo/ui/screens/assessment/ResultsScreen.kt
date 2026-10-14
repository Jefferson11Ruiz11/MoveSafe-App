package com.app.riesgo.ui.screens.assessment

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.riesgo.data.model.AiCorrectivePlan
import com.app.riesgo.data.model.EvaluationRecord
import com.app.riesgo.data.model.RiskLevel
import com.app.riesgo.data.model.SportType
import com.app.riesgo.data.model.ZoneRiskDto
import com.app.riesgo.data.repository.AppRepository
import com.app.riesgo.data.repository.EvaluationRepository
import com.app.riesgo.service.InjuryPreventionAiService
import com.app.riesgo.ui.components.GlassCard
import com.app.riesgo.ui.theme.*
import com.app.riesgo.util.PdfReportGenerator
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    record: EvaluationRecord,
    onFinish: () -> Unit,
    onViewHistory: () -> Unit
) {
    val context = LocalContext.current
    val user = AppRepository.currentUser
    val scope = rememberCoroutineScope()

    var isGeneratingPdf by remember { mutableStateOf(false) }
    var aiPlanState by remember { mutableStateOf<AiCorrectivePlan?>(record.aiCorrectivePlan) }
    var isLoadingAi by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    fun triggerAiGeneration() {
        val metricaText = if (record.cameraMetrics?.isBalanceTest == true) {
            "Sostenido Izq: ${record.tiempoSostenidoIzquierdo.toInt()}s, Der: ${record.tiempoSostenidoDerecho.toInt()}s, Oscilación: ${(record.indiceOscilacion * 100).toInt()}%"
        } else {
            "Ángulo mín: ${record.cameraMetrics?.minKneeAngle?.toInt() ?: 180}°, Valgos: ${record.cameraMetrics?.valgusCollapsesCount ?: 0}, Reps: ${record.cameraMetrics?.repsCount ?: 0}"
        }

        isLoadingAi = true
        aiError = null

        scope.launch {
            val result = InjuryPreventionAiService.generateCorrectivePlan(
                deporte = record.deporteEvaluado,
                edad = record.edad,
                imc = record.imc,
                lesionPrevia = record.injurySurvey?.zonaAnatomica,
                tipoPrueba = record.tipoPrueba,
                metricaCinematica = metricaText,
                nivelRiesgo = record.nivelRiesgo.name,
                dolorReportado = record.postTestFeedback?.ankleDiscomfort
            )

            result.fold(
                onSuccess = { plan ->
                    aiPlanState = plan
                    isLoadingAi = false
                    val updatedRecord = record.copy(aiCorrectivePlan = plan)
                    AppRepository.addEvaluationLocally(updatedRecord)
                    scope.launch {
                        EvaluationRepository.saveEvaluation(updatedRecord)
                    }
                },
                onFailure = { err ->
                    isLoadingAi = false
                    aiError = err.localizedMessage ?: "No se pudo conectar con el servicio de IA"
                }
            )
        }
    }

    LaunchedEffect(record.id) {
        if (record.aiCorrectivePlan == null) {
            triggerAiGeneration()
        }
    }

    val sportType = SportType.fromString(record.deporteEvaluado)
    val (sportIcon, sportTitle) = when (sportType) {
        SportType.FUTBOL -> Pair(Icons.Default.SportsSoccer, "Fútbol")
        SportType.VOLEY -> Pair(Icons.Default.SportsVolleyball, "Vóley")
        SportType.GENERAL -> Pair(Icons.Default.FitnessCenter, "Acondicionamiento General")
    }

    val (colorRiesgo, bgRiesgo, labelRiesgo) = when (record.nivelRiesgo) {
        RiskLevel.BAJO -> Triple(RiskLow, RiskLowBg, "Riesgo Lesional Bajo")
        RiskLevel.MODERADO -> Triple(RiskModerate, RiskModerateBg, "Riesgo Lesional Moderado")
        RiskLevel.ALTO -> Triple(RiskHigh, RiskHighBg, "Riesgo Lesional Elevado")
    }

    val animatedProb by animateFloatAsState(
        targetValue = record.probabilidad,
        animationSpec = tween(durationMillis = 900),
        label = "probAnim"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reporte Biomecánico (MoveSafe)", fontWeight = FontWeight.Bold) }
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
            // Header GlassCard: Tarjeta del Deporte Evaluado
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = sportIcon,
                                contentDescription = sportTitle,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Disciplina: $sportTitle",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Prueba: ${record.tipoPrueba}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Fecha: ${record.fecha}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // GlassCard de Indicador Radial
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Gauge Radial
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                        Canvas(modifier = Modifier.size(150.dp)) {
                            drawArc(
                                color = SlateBorder.copy(alpha = 0.3f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = colorRiesgo,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProb,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(animatedProb * 100).toInt()}%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Probabilidad", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = bgRiesgo,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, colorRiesgo.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = labelRiesgo,
                            color = colorRiesgo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Índice IMC Calculado: ${String.format(Locale.US, "%.1f", record.imc)} kg/m²",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // SECCIÓN: Zonas Anatómicas en Riesgo
            if (record.zonasCriticas.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Accessibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Zonas Anatómicas en Riesgo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    record.zonasCriticas.forEach { zone ->
                        ZoneRiskItem(zone)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // SECCIÓN INTELIGENTE: PLAN CORRECTIVO IA (GEMINI)
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Plan Preventivo Inteligente (MoveSafe AI)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Gemini 1.5",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoadingAi) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Analizando biomecánica y diseñando tu plan preventivo con Gemini...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (aiPlanState != null) {
                    val plan = aiPlanState!!

                    if (plan.resumenEnfoque.isNotBlank()) {
                        Text(
                            text = plan.resumenEnfoque,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Text(
                        "Ejercicios Prescritos:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    plan.ejerciciosRecomendados.forEach { ex ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ex.nombre,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = ex.dosificacion,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (ex.explicacionTecnica.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ex.explicacionTecnica,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (plan.pautaFrecuencia.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Frecuencia sugerida: ${plan.pautaFrecuencia}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (plan.alertaSeguridad.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = RiskHighBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠ ${plan.alertaSeguridad}",
                                fontSize = 11.sp,
                                color = RiskHigh,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiError ?: "No se pudo generar el plan automático.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { triggerAiGeneration() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar generación con IA")
                        }
                    }
                }
            }

            // Desglose de Factores Detectados
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Factores Biomecánicos Observados", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(10.dp))

                record.factoresDetectados.forEach { factor ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = colorRiesgo,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                            )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(factor, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón PDF MoveSafe
            Button(
                onClick = {
                    if (!isGeneratingPdf) {
                        isGeneratingPdf = true
                        val recordToExport = if (aiPlanState != null) record.copy(aiCorrectivePlan = aiPlanState) else record
                        PdfReportGenerator.generateAndSharePdf(
                            context = context,
                            record = recordToExport,
                            userName = user?.nombre ?: "Atleta"
                        )
                        isGeneratingPdf = false
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isGeneratingPdf) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Descargar Reporte PDF MoveSafe", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // Botones de acción
            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Nueva Evaluación", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onViewHistory,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Ir al Historial de Evaluaciones", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ZoneRiskItem(zone: ZoneRiskDto) {
    val level = try {
        RiskLevel.valueOf(zone.alertLevel)
    } catch (_: Exception) {
        RiskLevel.BAJO
    }

    val (colorAlert, bgAlert) = when (level) {
        RiskLevel.ALTO -> Pair(RiskHigh, RiskHighBg)
        RiskLevel.MODERADO -> Pair(RiskModerate, RiskModerateBg)
        RiskLevel.BAJO -> Pair(RiskLow, RiskLowBg)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = zone.zoneName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = bgAlert,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, colorAlert.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "Alerta: ${level.name}",
                        color = colorAlert,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (zone.category.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = zone.category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (zone.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = zone.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

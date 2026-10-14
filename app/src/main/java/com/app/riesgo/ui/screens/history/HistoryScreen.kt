package com.app.riesgo.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.riesgo.data.model.EvaluationRecord
import com.app.riesgo.data.model.RiskLevel
import com.app.riesgo.data.repository.AppRepository
import com.app.riesgo.data.repository.EvaluationRepository
import com.app.riesgo.ui.components.GlassCard
import com.app.riesgo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNewAssessment: () -> Unit,
    onBack: () -> Unit,
    onSelectRecord: (String) -> Unit
) {
    val user = AppRepository.currentUser
    val userId = user?.id ?: "guest"

    val evaluationsState by remember(userId) {
        EvaluationRepository.getUserEvaluationsFlow(userId)
    }.collectAsState(initial = AppRepository.getEvaluationsForCurrentUser())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Historial de Evaluaciones", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(user?.nombre ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewAssessment,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva Evaluación") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (evaluationsState.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HistoryEdu,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No hay evaluaciones registradas aún", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Pulsa en 'Nueva Evaluación' para iniciar", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .systemBarsPadding()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(evaluationsState) { record ->
                    HistoryItemCard(record = record, onClick = { onSelectRecord(record.id) })
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(record: EvaluationRecord, onClick: () -> Unit) {
    val (colorRiesgo, bgRiesgo, labelRiesgo) = when (record.nivelRiesgo) {
        RiskLevel.BAJO -> Triple(RiskLow, RiskLowBg, "Bajo")
        RiskLevel.MODERADO -> Triple(RiskModerate, RiskModerateBg, "Medio")
        RiskLevel.ALTO -> Triple(RiskHigh, RiskHighBg, "Alto")
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.tipoPrueba, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Fecha: ${record.fecha}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("IMC: ${record.imc} kg/m²", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Surface(
                color = bgRiesgo,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(1.dp, colorRiesgo.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = "$labelRiesgo (${(record.probabilidad * 100).toInt()}%)",
                    color = colorRiesgo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

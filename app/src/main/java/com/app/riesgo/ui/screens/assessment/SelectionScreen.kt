package com.app.riesgo.ui.screens.assessment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.riesgo.data.model.PhysicalTest
import com.app.riesgo.data.model.TestDifficulty
import com.app.riesgo.data.repository.AppRepository
import com.app.riesgo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(
    onSelectTest: (PhysicalTest) -> Unit,
    onBack: () -> Unit
) {
    val tests = AppRepository.tests

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protocolos de Prueba", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Selecciona el test cinemático a evaluar en el atleta:",
                    fontSize = 14.sp,
                    color = SlateMuted
                )
            }

            items(tests) { test ->
                ElevatedCard(
                    onClick = { onSelectTest(test) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                test.nombre,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SlateDark,
                                modifier = Modifier.weight(1f)
                            )
                            DifficultyBadge(test.dificultad)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            test.objetivoBiomecanico,
                            fontSize = 13.sp,
                            color = SlateMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    test.articulacionesClave,
                                    fontSize = 12.sp,
                                    color = SlateNavy,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = SlateMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultyBadge(diff: TestDifficulty) {
    val (label, bg, color) = when (diff) {
        TestDifficulty.PRINCIPIANTE -> Triple("Básico", RiskLowBg, RiskLow)
        TestDifficulty.INTERMEDIO -> Triple("Intermedio", RiskModerateBg, RiskModerate)
        TestDifficulty.AVANZADO -> Triple("Avanzado", RiskHighBg, RiskHigh)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
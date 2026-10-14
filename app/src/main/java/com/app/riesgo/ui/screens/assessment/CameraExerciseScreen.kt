package com.app.riesgo.ui.screens.assessment

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.app.riesgo.data.model.CameraBiomechanicalMetrics
import com.app.riesgo.data.model.DetectedSkeleton
import com.app.riesgo.data.model.RealLandmark
import com.app.riesgo.ui.theme.*
import com.app.riesgo.util.PoseEstimationAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraExerciseScreen(
    testName: String,
    onFinishCameraTest: (CameraBiomechanicalMetrics) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isBalanceTest = testName.contains("monopodal", ignoreCase = true) || testName.contains("equilibrio", ignoreCase = true)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var cameraSelectorLens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var detectedSkeleton by remember { mutableStateOf(DetectedSkeleton()) }
    var currentMetrics by remember { mutableStateOf(CameraBiomechanicalMetrics()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evaluación con Cámara ($testName)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        cameraSelectorLens = if (cameraSelectorLens == CameraSelector.LENS_FACING_FRONT) {
                            CameraSelector.LENS_FACING_BACK
                        } else {
                            CameraSelector.LENS_FACING_FRONT
                        }
                    }) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Cambiar Cámara")
                    }
                }
            )
        }
    ) { padding ->
        if (!hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VideocamOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Se requiere permiso de cámara para la evaluación biomecánica MoveSafe",
                        fontWeight = FontWeight.SemiBold,
                        color = SlateDark
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Otorgar Permiso de Cámara")
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val isFront = cameraSelectorLens == CameraSelector.LENS_FACING_FRONT
                key(cameraSelectorLens) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraExecutor = Executors.newSingleThreadExecutor()
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(
                                            cameraExecutor,
                                            PoseEstimationAnalyzer(ctx, isFront, isBalanceTest) { skeleton, metrics ->
                                                detectedSkeleton = skeleton
                                                currentMetrics = metrics
                                            }
                                        )
                                    }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.Builder().requireLensFacing(cameraSelectorLens).build(),
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (_: Exception) {}
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Overlay Canvas de Esqueleto Real MediaPipe (SOLO si las piernas/pies están bien encuadrados)
                val skeleton = detectedSkeleton
                if (skeleton.landmarks.size >= 29 && skeleton.feetVisible) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        fun RealLandmark.toPixel(): Offset {
                            return Offset(x * canvasW, y * canvasH)
                        }

                        fun RealLandmark.isValid(): Boolean {
                            return visibility >= 0.65f && presence >= 0.65f && y <= 0.95f
                        }

                        val lms = skeleton.landmarks
                        val lineColor = if (skeleton.isValgusDetected) RiskHigh else RiskLow

                        val connections = listOf(
                            Pair(lms[11], lms[12]), // Hombros
                            Pair(lms[11], lms[13]), Pair(lms[13], lms[15]), // Brazo Izquierdo
                            Pair(lms[12], lms[14]), Pair(lms[14], lms[16]), // Brazo Derecho
                            Pair(lms[11], lms[23]), Pair(lms[12], lms[24]), Pair(lms[23], lms[24]), // Torso
                            Pair(lms[23], lms[25]), Pair(lms[25], lms[27]), // Pierna Izquierda
                            Pair(lms[24], lms[26]), Pair(lms[26], lms[28])  // Pierna Derecha
                        )

                        connections.forEach { (p1, p2) ->
                            if (p1.isValid() && p2.isValid()) {
                                drawLine(
                                    color = lineColor,
                                    start = p1.toPixel(),
                                    end = p2.toPixel(),
                                    strokeWidth = 6.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        val mainIndices = listOf(11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)
                        mainIndices.forEach { idx ->
                            if (idx < lms.size) {
                                val lm = lms[idx]
                                if (lm.isValid()) {
                                    val offset = lm.toPixel()
                                    drawCircle(
                                        color = Color.White,
                                        radius = 8.dp.toPx(),
                                        center = offset
                                    )
                                    drawCircle(
                                        color = lineColor,
                                        radius = 5.dp.toPx(),
                                        center = offset
                                    )
                                }
                            }
                        }
                    }
                }

                // Panel Superior Flotante
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    if (isBalanceTest) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Apoyo Izquierdo", fontSize = 11.sp, color = SlateMuted)
                                Text("${currentMetrics.tiempoSostenidoIzquierdo.toInt()}s / 10s", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Apoyo Derecho", fontSize = 11.sp, color = SlateMuted)
                                Text("${currentMetrics.tiempoSostenidoDerecho.toInt()}s / 10s", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Oscilación", fontSize = 11.sp, color = SlateMuted)
                                Text("${(currentMetrics.indiceOscilacion * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (currentMetrics.indiceOscilacion > 0.10f) RiskHigh else RiskLow)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Repeticiones Válidas", fontSize = 11.sp, color = SlateMuted)
                                Text("${currentMetrics.repsCount} / 3", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SlateDark)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ángulo Rodilla Actual", fontSize = 11.sp, color = SlateMuted)
                                Text(
                                    if (skeleton.feetVisible) "${skeleton.currentKneeAngle.toInt()}°" else "--",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (skeleton.currentKneeAngle <= 90f) RiskLow else RiskModerate
                                )
                            }

                            if (skeleton.isValgusDetected) {
                                Surface(
                                    color = RiskHighBg,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.border(1.dp, RiskHigh, RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        "VALGO ALERTA",
                                        color = RiskHigh,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Panel Inferior de Instrucciones y Retroalimentación de Encuadre
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val guidanceText = when {
                            skeleton.landmarks.isEmpty() -> "No se detecta a ninguna persona. Ubícate frente a la cámara."
                            !skeleton.feetVisible -> "⚠ Da pasos atrás: pies y piernas deben ser visibles"
                            isBalanceTest -> {
                                when (currentMetrics.currentBalanceLeg) {
                                    "IZQUIERDA" -> "Fase 1: Apóyate sobre la pierna izquierda y levanta la rodilla derecha (10s)."
                                    "DERECHA" -> "Fase 2: Apóyate sobre la pierna derecha y levanta la rodilla izquierda (10s)."
                                    else -> "¡Excelente! Has completado el protocolo de equilibrio en ambas piernas."
                                }
                            }
                            currentMetrics.repsCount >= 3 -> "¡Excelente! Has completado las 3 sentadillas profundas."
                            else -> "Realiza 3 sentadillas profundas manteniendo las rodillas alineadas."
                        }

                        val textColor = if (!skeleton.feetVisible && skeleton.landmarks.isNotEmpty()) RiskHigh else SlateDark

                        Text(
                            text = guidanceText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onFinishCameraTest(currentMetrics) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finalizar Prueba y Responder Autopercepción", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

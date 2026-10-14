package com.app.riesgo.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.app.riesgo.data.model.CameraBiomechanicalMetrics
import com.app.riesgo.data.model.DetectedSkeleton
import com.app.riesgo.data.model.RealLandmark
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker.PoseLandmarkerOptions
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.abs

class PoseEstimationAnalyzer(
    context: Context,
    private val isFrontCamera: Boolean,
    private val isBalanceTest: Boolean = false,
    private val onSkeletonUpdated: (DetectedSkeleton, CameraBiomechanicalMetrics) -> Unit
) : ImageAnalysis.Analyzer {

    enum class SquatState { UP, DESCENDING, DOWN, ASCENDING }

    private var squatState = SquatState.UP
    private var repsCount = 0
    private var minKneeAngle = 180f
    private var valgusCount = 0
    private var maxAsymmetry = 0f

    private var downStartTimeMs = 0L
    private var lastRepTimeMs = 0L

    // Variables para Test de Equilibrio Monopodal
    private var balanceStartTimeMs = 0L
    private var currentBalanceLeg = "IZQUIERDA"
    private var timeSustainedLeftMs = 0L
    private var timeSustainedRightMs = 0L
    private val hipXWindow = ArrayDeque<Float>()
    private var totalOscillation = 0f

    private val angleWindow = ArrayDeque<Float>()
    private var poseLandmarker: PoseLandmarker? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_lite.task")
                .build()

            val options = PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result: PoseLandmarkerResult, _: MPImage ->
                    mainScope.launch {
                        processPoseResult(result)
                    }
                }
                .setErrorListener { _ ->
                    mainScope.launch {
                        processEmptyResult()
                    }
                }
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (_: Exception) {
            poseLandmarker = null
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val frameTimeMs = System.currentTimeMillis()

        try {
            val landmarker = poseLandmarker
            if (landmarker == null) {
                mainScope.launch { processEmptyResult() }
                return
            }

            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val matrix = Matrix()
            if (rotationDegrees != 0) {
                matrix.postRotate(rotationDegrees.toFloat())
            }
            if (isFrontCamera) {
                matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }

            val processedBitmap = if (!matrix.isIdentity) {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val mpImage = BitmapImageBuilder(processedBitmap).build()
            landmarker.detectAsync(mpImage, frameTimeMs)
        } catch (_: Exception) {
            mainScope.launch { processEmptyResult() }
        } finally {
            imageProxy.close()
        }
    }

    private fun processEmptyResult() {
        angleWindow.clear()
        val skeleton = DetectedSkeleton(
            landmarks = emptyList(),
            isFrontCamera = isFrontCamera,
            isBodyFullyVisible = false,
            feetVisible = false,
            currentKneeAngle = 180f,
            isValgusDetected = false,
            currentLegElevated = "NINGUNA"
        )
        val metrics = CameraBiomechanicalMetrics(
            repsCount = repsCount,
            minKneeAngle = minKneeAngle,
            valgusCollapsesCount = valgusCount,
            asymmetryAngleDiff = maxAsymmetry,
            isDepthAchieved = minKneeAngle <= 90f,
            userPositionedOk = false,
            isBalanceTest = isBalanceTest,
            tiempoSostenidoIzquierdo = (timeSustainedLeftMs / 1000f).coerceAtMost(10f),
            tiempoSostenidoDerecho = (timeSustainedRightMs / 1000f).coerceAtMost(10f),
            indiceOscilacion = totalOscillation,
            currentBalanceLeg = currentBalanceLeg
        )
        onSkeletonUpdated(skeleton, metrics)
    }

    private fun processPoseResult(result: PoseLandmarkerResult) {
        val landmarkList = result.landmarks().firstOrNull()

        if (landmarkList.isNullOrEmpty() || landmarkList.size < 29) {
            processEmptyResult()
            return
        }

        val realLandmarks = landmarkList.map { lm ->
            val vis = try { if (lm.visibility().isPresent) lm.visibility().get() else 1.0f } catch (_: Exception) { 1.0f }
            val pres = try { if (lm.presence().isPresent) lm.presence().get() else 1.0f } catch (_: Exception) { 1.0f }
            RealLandmark(
                x = lm.x(),
                y = lm.y(),
                z = lm.z(),
                visibility = vis,
                presence = pres
            )
        }

        val leftHip = realLandmarks[23]
        val rightHip = realLandmarks[24]
        val leftKnee = realLandmarks[25]
        val rightKnee = realLandmarks[26]
        val leftAnkle = realLandmarks[27]
        val rightAnkle = realLandmarks[28]

        fun isJointValid(lm: RealLandmark): Boolean {
            return lm.visibility >= 0.65f && lm.presence >= 0.65f && lm.y <= 0.95f
        }

        val leftLegOk = isJointValid(leftHip) && isJointValid(leftKnee) && isJointValid(leftAnkle)
        val rightLegOk = isJointValid(rightHip) && isJointValid(rightKnee) && isJointValid(rightAnkle)
        val feetVisible = isJointValid(leftAnkle) && isJointValid(rightAnkle)

        if (!leftLegOk && !rightLegOk) {
            processUnframedResult(realLandmarks, feetVisible)
            return
        }

        val rawLeftAngle = if (leftLegOk) calculateKneeAngle(leftHip, leftKnee, leftAnkle) else 180f
        val rawRightAngle = if (rightLegOk) calculateKneeAngle(rightHip, rightKnee, rightAnkle) else 180f

        val rawAvgAngle = when {
            leftLegOk && rightLegOk -> (rawLeftAngle + rawRightAngle) / 2f
            leftLegOk -> rawLeftAngle
            else -> rawRightAngle
        }

        val smoothedAngle = applyMovingAverage(rawAvgAngle)

        if (leftLegOk && rightLegOk) {
            val asymmetry = abs(rawLeftAngle - rawRightAngle)
            if (asymmetry > maxAsymmetry) {
                maxAsymmetry = asymmetry
            }
        }

        if (smoothedAngle < minKneeAngle) {
            minKneeAngle = smoothedAngle
        }

        val isValgus = (leftKnee.x > leftAnkle.x + 0.03f) || (rightKnee.x < rightAnkle.x - 0.03f)
        val now = System.currentTimeMillis()

        var legElevated = "NINGUNA"
        if (isBalanceTest) {
            val ankleDiffY = leftAnkle.y - rightAnkle.y
            if (ankleDiffY > 0.07f) {
                legElevated = "IZQUIERDA"
            } else if (ankleDiffY < -0.07f) {
                legElevated = "DERECHA"
            }

            val avgHipX = (leftHip.x + rightHip.x) / 2f
            if (hipXWindow.size >= 10) hipXWindow.removeFirst()
            hipXWindow.addLast(avgHipX)
            if (hipXWindow.size >= 3) {
                val mean = hipXWindow.average().toFloat()
                val varSum = hipXWindow.sumOf { (it - mean) * (it - mean).toDouble() }
                totalOscillation = (Math.sqrt(varSum / hipXWindow.size).toFloat() * 10f).coerceIn(0f, 1f)
            }

            if (currentBalanceLeg == "IZQUIERDA") {
                if (legElevated == "DERECHA" || legElevated == "IZQUIERDA") {
                    if (balanceStartTimeMs == 0L) balanceStartTimeMs = now
                    timeSustainedLeftMs += (now - balanceStartTimeMs).coerceAtMost(200L)
                    balanceStartTimeMs = now
                    if (timeSustainedLeftMs >= 10000L) {
                        currentBalanceLeg = "DERECHA"
                        balanceStartTimeMs = 0L
                    }
                } else {
                    balanceStartTimeMs = 0L
                }
            } else if (currentBalanceLeg == "DERECHA") {
                if (legElevated == "DERECHA" || legElevated == "IZQUIERDA") {
                    if (balanceStartTimeMs == 0L) balanceStartTimeMs = now
                    timeSustainedRightMs += (now - balanceStartTimeMs).coerceAtMost(200L)
                    balanceStartTimeMs = now
                    if (timeSustainedRightMs >= 10000L) {
                        currentBalanceLeg = "COMPLETADO"
                    }
                } else {
                    balanceStartTimeMs = 0L
                }
            }
        } else {
            when (squatState) {
                SquatState.UP -> {
                    if (smoothedAngle < 140f) {
                        squatState = SquatState.DESCENDING
                    }
                }
                SquatState.DESCENDING -> {
                    if (smoothedAngle <= 100f) {
                        squatState = SquatState.DOWN
                        downStartTimeMs = now
                    } else if (smoothedAngle >= 155f) {
                        squatState = SquatState.UP
                    }
                }
                SquatState.DOWN -> {
                    if (smoothedAngle <= 100f) {
                        if (now - downStartTimeMs >= 300L) {
                            if (isValgus) {
                                valgusCount++
                            }
                            squatState = SquatState.ASCENDING
                        }
                    } else if (smoothedAngle > 115f) {
                        squatState = SquatState.DESCENDING
                    }
                }
                SquatState.ASCENDING -> {
                    if (smoothedAngle >= 155f) {
                        if (now - lastRepTimeMs >= 1200L) {
                            repsCount++
                            lastRepTimeMs = now
                        }
                        squatState = SquatState.UP
                    }
                }
            }
        }

        val skeleton = DetectedSkeleton(
            landmarks = realLandmarks,
            isFrontCamera = isFrontCamera,
            isBodyFullyVisible = leftLegOk && rightLegOk,
            feetVisible = feetVisible,
            currentKneeAngle = smoothedAngle,
            isValgusDetected = isValgus,
            currentLegElevated = legElevated
        )

        val metrics = CameraBiomechanicalMetrics(
            repsCount = repsCount,
            minKneeAngle = minKneeAngle,
            valgusCollapsesCount = valgusCount,
            asymmetryAngleDiff = maxAsymmetry,
            isDepthAchieved = minKneeAngle <= 90f,
            userPositionedOk = feetVisible,
            isBalanceTest = isBalanceTest,
            tiempoSostenidoIzquierdo = (timeSustainedLeftMs / 1000f).coerceAtMost(10f),
            tiempoSostenidoDerecho = (timeSustainedRightMs / 1000f).coerceAtMost(10f),
            indiceOscilacion = totalOscillation,
            currentBalanceLeg = currentBalanceLeg
        )

        onSkeletonUpdated(skeleton, metrics)
    }

    private fun processUnframedResult(
        realLandmarks: List<RealLandmark>,
        feetVisible: Boolean
    ) {
        val skeleton = DetectedSkeleton(
            landmarks = realLandmarks,
            isFrontCamera = isFrontCamera,
            isBodyFullyVisible = false,
            feetVisible = feetVisible,
            currentKneeAngle = 180f,
            isValgusDetected = false,
            currentLegElevated = "NINGUNA"
        )
        val metrics = CameraBiomechanicalMetrics(
            repsCount = repsCount,
            minKneeAngle = minKneeAngle,
            valgusCollapsesCount = valgusCount,
            asymmetryAngleDiff = maxAsymmetry,
            isDepthAchieved = minKneeAngle <= 90f,
            userPositionedOk = false,
            isBalanceTest = isBalanceTest,
            tiempoSostenidoIzquierdo = (timeSustainedLeftMs / 1000f).coerceAtMost(10f),
            tiempoSostenidoDerecho = (timeSustainedRightMs / 1000f).coerceAtMost(10f),
            indiceOscilacion = totalOscillation,
            currentBalanceLeg = currentBalanceLeg
        )
        onSkeletonUpdated(skeleton, metrics)
    }

    private fun applyMovingAverage(newAngle: Float): Float {
        if (angleWindow.size >= 5) {
            angleWindow.removeFirst()
        }
        angleWindow.addLast(newAngle)
        return angleWindow.average().toFloat()
    }

    companion object {
        fun calculateKneeAngle(
            hip: RealLandmark,
            knee: RealLandmark,
            ankle: RealLandmark
        ): Float {
            val radians = atan2((ankle.y - knee.y).toDouble(), (ankle.x - knee.x).toDouble()) -
                          atan2((hip.y - knee.y).toDouble(), (hip.x - knee.x).toDouble())

            var angle = abs(radians * (180.0 / Math.PI)).toFloat()
            if (angle > 180f) {
                angle = 360f - angle
            }
            return angle.coerceIn(0f, 180f)
        }
    }
}

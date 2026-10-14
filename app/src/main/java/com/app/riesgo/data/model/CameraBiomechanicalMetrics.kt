package com.app.riesgo.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class RealLandmark(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val visibility: Float = 1.0f,
    val presence: Float = 1.0f
)

@IgnoreExtraProperties
data class DetectedSkeleton(
    val landmarks: List<RealLandmark> = emptyList(),
    val isFrontCamera: Boolean = true,
    val isBodyFullyVisible: Boolean = false,
    val feetVisible: Boolean = false,
    val currentKneeAngle: Float = 180f,
    val isValgusDetected: Boolean = false,
    val currentLegElevated: String = "NINGUNA"
)

@IgnoreExtraProperties
data class CameraBiomechanicalMetrics(
    val repsCount: Int = 0,
    val minKneeAngle: Float = 180f,
    val valgusCollapsesCount: Int = 0,
    val asymmetryAngleDiff: Float = 0f,
    val isDepthAchieved: Boolean = false,
    val userPositionedOk: Boolean = false,
    val isBalanceTest: Boolean = false,
    val tiempoSostenidoIzquierdo: Float = 0f,
    val tiempoSostenidoDerecho: Float = 0f,
    val indiceOscilacion: Float = 0f,
    val currentBalanceLeg: String = "IZQUIERDA"
) {
    constructor() : this(0, 180f, 0, 0f, false, false, false, 0f, 0f, 0f, "IZQUIERDA")

    fun toMap(): Map<String, Any> {
        return mapOf(
            "repsCount" to repsCount,
            "minKneeAngle" to minKneeAngle,
            "valgusCollapsesCount" to valgusCollapsesCount,
            "asymmetryAngleDiff" to asymmetryAngleDiff,
            "isDepthAchieved" to isDepthAchieved,
            "userPositionedOk" to userPositionedOk,
            "isBalanceTest" to isBalanceTest,
            "tiempoSostenidoIzquierdo" to tiempoSostenidoIzquierdo,
            "tiempoSostenidoDerecho" to tiempoSostenidoDerecho,
            "indiceOscilacion" to indiceOscilacion,
            "currentBalanceLeg" to currentBalanceLeg
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): CameraBiomechanicalMetrics {
            if (map == null) return CameraBiomechanicalMetrics()
            return CameraBiomechanicalMetrics(
                repsCount = (map["repsCount"] as? Number)?.toInt() ?: 0,
                minKneeAngle = (map["minKneeAngle"] as? Number)?.toFloat() ?: 180f,
                valgusCollapsesCount = (map["valgusCollapsesCount"] as? Number)?.toInt() ?: 0,
                asymmetryAngleDiff = (map["asymmetryAngleDiff"] as? Number)?.toFloat() ?: 0f,
                isDepthAchieved = map["isDepthAchieved"] as? Boolean ?: false,
                userPositionedOk = map["userPositionedOk"] as? Boolean ?: false,
                isBalanceTest = map["isBalanceTest"] as? Boolean ?: false,
                tiempoSostenidoIzquierdo = (map["tiempoSostenidoIzquierdo"] as? Number)?.toFloat() ?: 0f,
                tiempoSostenidoDerecho = (map["tiempoSostenidoDerecho"] as? Number)?.toFloat() ?: 0f,
                indiceOscilacion = (map["indiceOscilacion"] as? Number)?.toFloat() ?: 0f,
                currentBalanceLeg = map["currentBalanceLeg"] as? String ?: "IZQUIERDA"
            )
        }
    }
}

@IgnoreExtraProperties
data class PostTestFeedback(
    val painScale: Int = 0,
    val fatigueZone: String = "Ninguna",
    val stabilityFeeling: String = "Seguro / Estable",
    val ankleDiscomfort: String = "Ninguna",
    val muscleWeaknessFeeling: Boolean = false
) {
    constructor() : this(0, "Ninguna", "Seguro / Estable", "Ninguna", false)

    fun toMap(): Map<String, Any> {
        return mapOf(
            "painScale" to painScale,
            "fatigueZone" to fatigueZone,
            "stabilityFeeling" to stabilityFeeling,
            "ankleDiscomfort" to ankleDiscomfort,
            "muscleWeaknessFeeling" to muscleWeaknessFeeling
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): PostTestFeedback {
            if (map == null) return PostTestFeedback()
            return PostTestFeedback(
                painScale = (map["painScale"] as? Number)?.toInt() ?: 0,
                fatigueZone = map["fatigueZone"] as? String ?: "Ninguna",
                stabilityFeeling = map["stabilityFeeling"] as? String ?: "Seguro / Estable",
                ankleDiscomfort = map["ankleDiscomfort"] as? String ?: "Ninguna",
                muscleWeaknessFeeling = map["muscleWeaknessFeeling"] as? Boolean ?: false
            )
        }
    }
}

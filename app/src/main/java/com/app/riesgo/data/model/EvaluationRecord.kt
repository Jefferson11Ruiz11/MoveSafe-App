package com.app.riesgo.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

enum class RiskLevel { BAJO, MODERADO, ALTO }

@IgnoreExtraProperties
data class ZoneRiskDto(
    val zoneName: String = "",
    val category: String = "",
    val alertLevel: String = "BAJO",
    val description: String = ""
) {
    constructor() : this("", "", "BAJO", "")
}

@IgnoreExtraProperties
data class EvaluationRecord(
    val id: String = "",
    val userId: String = "",
    val fecha: String = "",
    val tipoPrueba: String = "",
    val deporteEvaluado: String = "General",
    val edad: Int = 0,
    val peso: Float = 70.0f,
    val altura: Float = 175.0f,
    val imc: Float = 0.0f,
    val nivelRiesgo: RiskLevel = RiskLevel.BAJO,
    val probabilidad: Float = 0.0f,
    val factoresDetectados: List<String> = emptyList(),
    val recomendaciones: List<String> = emptyList(),
    val zonasCriticas: List<ZoneRiskDto> = emptyList(),
    val injurySurvey: DynamicSurveyResult? = null,
    val cameraMetrics: CameraBiomechanicalMetrics? = null,
    val postTestFeedback: PostTestFeedback? = null,
    val tiempoSostenidoIzquierdo: Float = 0f,
    val tiempoSostenidoDerecho: Float = 0f,
    val indiceOscilacion: Float = 0f,
    val aiCorrectivePlan: AiCorrectivePlan? = null
) {
    // Constructor sin argumentos requerido por Firestore
    constructor() : this("", "", "", "", "General", 0, 70.0f, 175.0f, 0.0f, RiskLevel.BAJO, 0.0f, emptyList(), emptyList(), emptyList(), null, null, null, 0f, 0f, 0f, null)
}

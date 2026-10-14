package com.app.riesgo.data.repository

import com.app.riesgo.data.model.CameraBiomechanicalMetrics
import com.app.riesgo.data.model.CriticalZoneRisk
import com.app.riesgo.data.model.DynamicSurveyResult
import com.app.riesgo.data.model.PostTestFeedback
import com.app.riesgo.data.model.RiskAnalysisResult
import com.app.riesgo.data.model.RiskLevel
import com.app.riesgo.data.model.SportType
import java.util.Locale

object InjuryRiskEngine {

    fun calculateImc(pesoKg: Float, alturaCm: Float): Float {
        val alturaM = alturaCm / 100f
        return if (alturaM > 0) pesoKg / (alturaM * alturaM) else 0f
    }

    fun calculateSportRisk(
        deporte: SportType,
        prueba: String,
        edad: Int,
        imc: Float,
        tieneLesionesPrevias: Boolean,
        surveyResult: DynamicSurveyResult? = null,
        cameraMetrics: CameraBiomechanicalMetrics? = null,
        postTestFeedback: PostTestFeedback? = null
    ): RiskAnalysisResult {
        val baseResult = when (deporte) {
            SportType.FUTBOL -> calculateFutbolRisk(prueba, edad, imc, tieneLesionesPrevias)
            SportType.VOLEY -> calculateVoleyRisk(prueba, edad, imc, tieneLesionesPrevias)
            SportType.GENERAL -> calculateGeneralRisk(prueba, edad, imc, tieneLesionesPrevias)
        }

        var extraRiskScore = 0f
        val extraFactores = baseResult.biomechanicalFactors.toMutableList()
        val extraRecomendaciones = baseResult.targetedRecommendations.toMutableList()
        var updatedZonas = baseResult.criticalZones.toMutableList()

        // 1. Cuestionario dinámico de lesiones
        if (surveyResult != null && surveyResult.zonaAnatomica.isNotBlank()) {
            val vulIndex = surveyResult.indiceVulnerabilidad
            extraRiskScore += (vulIndex / 100f) * 0.20f

            extraFactores.add("Vulnerabilidad focalizada en ${surveyResult.zonaAnatomica} (Índice específico: ${vulIndex.toInt()}%)")
            if (surveyResult.mecanismoLesion.isNotBlank()) {
                extraFactores.add("Mecanismo desencadenante: ${surveyResult.mecanismoLesion} [${surveyResult.estadoRecuperacion}]")
            }

            if (surveyResult.condicionSeguridadO_Dolor.contains("dolor", ignoreCase = true) ||
                surveyResult.condicionSeguridadO_Dolor.contains("temor", ignoreCase = true) ||
                surveyResult.estadoRecuperacion.contains("incompleta", ignoreCase = true)
            ) {
                extraRecomendaciones.add("Priorizar fortalecimiento excéntrico y reeducación propioceptiva en ${surveyResult.zonaAnatomica} antes de competencia.")
            }

            updatedZonas = updatedZonas.map { zone ->
                val isTargetZone = zone.zoneName.contains(surveyResult.zonaAnatomica, ignoreCase = true) ||
                        zone.category.contains(surveyResult.zonaAnatomica, ignoreCase = true) ||
                        (surveyResult.zonaAnatomica.contains("Rodilla", ignoreCase = true) && zone.category.contains("Rodilla", ignoreCase = true)) ||
                        (surveyResult.zonaAnatomica.contains("Hombro", ignoreCase = true) && zone.category.contains("Hombro", ignoreCase = true)) ||
                        (surveyResult.zonaAnatomica.contains("Tobillo", ignoreCase = true) && zone.category.contains("Tobillo", ignoreCase = true))

                if (isTargetZone && vulIndex >= 40f) {
                    zone.copy(
                        alertLevel = RiskLevel.ALTO,
                        description = "${zone.description} [VULNERABILIDAD ALTA por antecedente en ${surveyResult.zonaAnatomica}]"
                    )
                } else {
                    zone
                }
            }.toMutableList()
        }

        // 2. Métricas Biomecánicas de Cámara MediaPipe
        if (cameraMetrics != null) {
            if (cameraMetrics.valgusCollapsesCount > 0) {
                extraRiskScore += 0.15f
                extraFactores.add("Colapso de valgo dinámico de rodilla detectado visualmente por cámara (${cameraMetrics.valgusCollapsesCount} eventos)")
                extraRecomendaciones.add("Aumentar activación de glúteo medio y ejercicios de abducción de cadera con banda elástica.")
            }

            if (cameraMetrics.minKneeAngle > 95f) {
                extraRiskScore += 0.08f
                extraFactores.add("Profundidad incompleta en sentadilla (Ángulo mínimo registrado: ${cameraMetrics.minKneeAngle.toInt()}°)")
                extraRecomendaciones.add("Trabajar movilidad de tobillo (dorsiflexión) y flexibilidad de sóleo/gemelos.")
            }

            if (cameraMetrics.asymmetryAngleDiff > 10f) {
                extraRiskScore += 0.10f
                extraFactores.add("Asimetría articular bilateral de ${cameraMetrics.asymmetryAngleDiff.toInt()}° detectada durante la prueba")
                extraRecomendaciones.add("Incorporar trabajo unilateral (sentadillas búlgaras y zancadas) para corregir asimetría muscular.")
            }
        }

        // 3. Autopercepción Post-Prueba
        if (postTestFeedback != null) {
            if (postTestFeedback.painScale >= 4) {
                extraRiskScore += 0.15f
                extraFactores.add("Dolor articular autopercibido durante la prueba (${postTestFeedback.painScale}/10 en la escala EVA)")
                extraRecomendaciones.add("Cesar cargas de alta intensidad y realizar evaluación kinésica para descartar inflamación articular.")
            }

            if (postTestFeedback.stabilityFeeling.contains("inestable", ignoreCase = true)) {
                extraRiskScore += 0.10f
                extraFactores.add("Sensación de inestabilidad o aprensión articular percibida por el atleta")
            }
        }

        val updatedPercentage = (baseResult.riskPercentage + (extraRiskScore * 100f)).coerceIn(5f, 98f)
        val updatedLevel = when {
            updatedPercentage >= 65f -> RiskLevel.ALTO
            updatedPercentage >= 40f -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }

        return baseResult.copy(
            overallRiskLevel = updatedLevel,
            riskPercentage = updatedPercentage,
            criticalZones = updatedZonas,
            biomechanicalFactors = extraFactores.distinct(),
            targetedRecommendations = extraRecomendaciones.distinct()
        )
    }

    private fun calculateFutbolRisk(
        prueba: String,
        edad: Int,
        imc: Float,
        tieneLesionesPrevias: Boolean
    ): RiskAnalysisResult {
        var score = 0.20f
        val factores = mutableListOf<String>()
        val recomendaciones = mutableListOf<String>()
        val zonas = mutableListOf<CriticalZoneRisk>()

        if (tieneLesionesPrevias) {
            score += 0.30f
            factores.add("Antecedente de lesión previa no rehabilitada (Sensibilidad +30% en cinemática futbolística)")
            recomendaciones.add("Protocolo FIFA 11+ de calentamiento neuromuscular enfocado en control excéntrico de isquiotibiales (Nordic Curls).")
        }

        if (imc > 25.0f) {
            val extraImc = ((imc - 25.0f) * 0.03f).coerceAtMost(0.20f)
            score += 0.15f + extraImc
            factores.add("Sobrecarga por IMC (${String.format(Locale.US, "%.1f", imc)}) en desaceleraciones y pivots rotacionales")
            recomendaciones.add("Acondicionamiento aeróbico alternativo de bajo impacto e integración de drills de desaceleración controlada.")
        } else {
            factores.add("Índice de masa corporal en rango funcional para giros y aceleraciones")
        }

        if (edad > 28) {
            score += 0.10f
            factores.add("Rigidez miotendinosa asociada a edad mayor a 28 años en desaceleración reactiva")
            recomendaciones.add("Protocolo diario de movilidad de cadera y flexibilidad de cadena posterior.")
        }

        when {
            prueba.contains("squat", ignoreCase = true) || prueba.contains("sentadilla", ignoreCase = true) -> {
                score += 0.05f
                factores.add("Estrés articular axial y vulnerabilidad al valgo dinámico de rodilla en squat")
            }
            prueba.contains("monopodal", ignoreCase = true) || prueba.contains("equilibrio", ignoreCase = true) -> {
                if (tieneLesionesPrevias) score += 0.10f
                factores.add("Inestabilidad monopodal y déficit en reactividad propioceptiva de tobillo/cadera")
            }
            prueba.contains("jump", ignoreCase = true) || prueba.contains("salto", ignoreCase = true) -> {
                score += 0.10f
                factores.add("Fuerza plio-reactiva con alta tasa de carga en desaceleración articular")
            }
        }

        val levelRodilla = when {
            tieneLesionesPrevias && imc > 25f -> RiskLevel.ALTO
            tieneLesionesPrevias || imc > 25f || edad > 30 -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }
        zonas.add(
            CriticalZoneRisk(
                zoneName = "Ligamento Cruzado Anterior / Meniscos",
                category = "Rodilla",
                alertLevel = levelRodilla,
                description = "Vulnerabilidad a fuerzas de cizallamiento y valgo dinámico durante regates y frenadas bruscas."
            )
        )

        val levelTobillo = when {
            prueba.contains("monopodal", ignoreCase = true) && (tieneLesionesPrevias || imc > 25f) -> RiskLevel.ALTO
            imc > 25f || tieneLesionesPrevias -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }
        zonas.add(
            CriticalZoneRisk(
                zoneName = "Tobillo Lateral y Complejo Ligamentario",
                category = "Tobillo",
                alertLevel = levelTobillo,
                description = "Susceptibilidad a esguinces por inversión forzada en terreno irregular o apoyos monopodal."
            )
        )

        val levelIsquios = when {
            tieneLesionesPrevias -> RiskLevel.ALTO
            edad > 28 -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }
        zonas.add(
            CriticalZoneRisk(
                zoneName = "Isquiotibiales (Bíceps Femoral)",
                category = "Muslo / Cadera",
                alertLevel = levelIsquios,
                description = "Riesgo de desgarro excéntrico durante el sprint máximo o la fase de desaceleración."
            )
        )

        recomendaciones.add("Entrenamiento de desaceleración y cambio de dirección con énfasis en prevención del valgo dinámico.")
        recomendaciones.add("Trabajo propioceptivo sobre base estable e inestable para refuerzo del complejo ligamentario del tobillo.")

        val percentage = (score * 100f).coerceIn(5f, 98f)
        val overallLevel = when {
            percentage >= 65f -> RiskLevel.ALTO
            percentage >= 40f -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }

        return RiskAnalysisResult(
            sportType = SportType.FUTBOL,
            overallRiskLevel = overallLevel,
            riskPercentage = percentage,
            criticalZones = zonas,
            biomechanicalFactors = factores.distinct(),
            targetedRecommendations = recomendaciones.distinct()
        )
    }

    private fun calculateVoleyRisk(
        prueba: String,
        edad: Int,
        imc: Float,
        tieneLesionesPrevias: Boolean
    ): RiskAnalysisResult {
        var score = 0.20f
        val factores = mutableListOf<String>()
        val recomendaciones = mutableListOf<String>()
        val zonas = mutableListOf<CriticalZoneRisk>()

        if (imc > 25.0f) {
            val extraImc = ((imc - 25.0f) * 0.04f).coerceAtMost(0.25f)
            score += 0.25f + extraImc
            factores.add("Impacto de aterrizaje vertical acelerado por IMC elevado (${String.format(Locale.US, "%.1f", imc)})")
            recomendaciones.add("Control de masa corporal y reemplazo parcial de saltos por trabajo acuático/ergómetro.")
        } else {
            factores.add("Masa corporal adecuada para amortiguación de impacto plio-reactivo")
        }

        if (tieneLesionesPrevias) {
            score += 0.25f
            factores.add("Alteración en la cadena cinética de salto/aterrizaje por antecedente lesional previa")
            recomendaciones.add("Reeducación kinésica de la fase de amortiguación bipodal post-salto.")
        }

        if (edad > 28) {
            score += 0.10f
            factores.add("Disminución en capacidad de absorción elástica tendinosa asociada a la edad")
        }

        val levelRotuliano = when {
            imc > 25f && (prueba.contains("jump", ignoreCase = true) || tieneLesionesPrevias) -> RiskLevel.ALTO
            imc > 24f || tieneLesionesPrevias -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }
        zonas.add(
            CriticalZoneRisk(
                zoneName = "Tendón Rotuliano (Rodilla del Saltador)",
                category = "Rodilla (Miembro Inferior 60%)",
                alertLevel = levelRotuliano,
                description = "Microtraumatismo repetitivo por fuerzas de tracción excéntrica en el despegue y aterrizaje."
            )
        )

        val levelTobilloVoley = when {
            tieneLesionesPrevias -> RiskLevel.ALTO
            imc > 25f || prueba.contains("monopodal", ignoreCase = true) -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }
        zonas.add(
            CriticalZoneRisk(
                zoneName = "Tobillo en Bloqueo / Aterrizaje",
                category = "Tobillo (Miembro Inferior 60%)",
                alertLevel = levelTobilloVoley,
                description = "Riesgo de inversión en aterrizaje sobre pie de compañero o bloqueo en la red."
            )
        )

        val levelHombro = when {
            tieneLesionesPrevias && edad > 25 -> RiskLevel.ALTO
            tieneLesionesPrevias || edad > 28 -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }
        zonas.add(
            CriticalZoneRisk(
                zoneName = "Manguito Rotador y Escápula",
                category = "Hombro Dominante (Miembro Superior 40%)",
                alertLevel = levelHombro,
                description = "Estrés por hiper-extensión y aceleración repetitiva en la fase de remate y saque flotante."
            )
        )

        recomendaciones.add("Fortalecimiento excéntrico del tendón rotuliano con sentadillas declinadas (Decline Squats a 25°).")
        recomendaciones.add("Estabilización escapular y reforzamiento de rotadores externos del hombro para prevención de discinesia.")
        recomendaciones.add("Revisión técnica de la mecánica de aterrizaje con flexión profunda de cadera para disipar fuerzas de impacto.")

        val percentage = (score * 100f).coerceIn(5f, 98f)
        val overallLevel = when {
            percentage >= 65f -> RiskLevel.ALTO
            percentage >= 40f -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }

        return RiskAnalysisResult(
            sportType = SportType.VOLEY,
            overallRiskLevel = overallLevel,
            riskPercentage = percentage,
            criticalZones = zonas,
            biomechanicalFactors = factores.distinct(),
            targetedRecommendations = recomendaciones.distinct()
        )
    }

    private fun calculateGeneralRisk(
        prueba: String,
        edad: Int,
        imc: Float,
        tieneLesionesPrevias: Boolean
    ): RiskAnalysisResult {
        var score = 0.20f
        val factores = mutableListOf<String>()
        val recomendaciones = mutableListOf<String>()
        val zonas = mutableListOf<CriticalZoneRisk>()

        if (tieneLesionesPrevias) {
            score += 0.25f
            factores.add("Antecedente de lesión previa no totalmente rehabilitada")
            recomendaciones.add("Calentamiento dinámico prolongado y readaptación física focalizada.")
        }

        if (imc > 27.0f) {
            score += 0.20f
            factores.add("Sobrecarga mecánica estructural por IMC elevado (${String.format(Locale.US, "%.1f", imc)})")
            recomendaciones.add("Acondicionamiento físico de bajo impacto para protección articular.")
        } else {
            factores.add("Rango antropométrico óptimo para acondicionamiento general")
        }

        if (edad > 35) {
            score += 0.10f
            factores.add("Rigidez articular y miofascial asociada a la edad cronológica")
        }

        val levelRodilla = if (imc > 27f || tieneLesionesPrevias) RiskLevel.MODERADO else RiskLevel.BAJO
        zonas.add(
            CriticalZoneRisk(
                zoneName = "Complejo Articular de Rodilla",
                category = "Rodilla",
                alertLevel = levelRodilla,
                description = "Estrés acumulado por patrones de flexo-extensión en carga."
            )
        )

        val levelZonaLumbar = if (edad > 30 || imc > 27f) RiskLevel.MODERADO else RiskLevel.BAJO
        zonas.add(
            CriticalZoneRisk(
                zoneName = "Columna Lumbar y Núcleo Core",
                category = "Espalda / Core",
                alertLevel = levelZonaLumbar,
                description = "Demanda de estabilización central durante la postura erguida y transferencias de carga."
            )
        )

        recomendaciones.add("Rutina diaria de movilidad articular y fortalecimiento del Core (planchas isométricas).")

        val percentage = (score * 100f).coerceIn(5f, 98f)
        val overallLevel = when {
            percentage >= 65f -> RiskLevel.ALTO
            percentage >= 40f -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }

        return RiskAnalysisResult(
            sportType = SportType.GENERAL,
            overallRiskLevel = overallLevel,
            riskPercentage = percentage,
            criticalZones = zonas,
            biomechanicalFactors = factores.distinct(),
            targetedRecommendations = recomendaciones.distinct()
        )
    }
}

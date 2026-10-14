package com.app.riesgo.data.model

data class CriticalZoneRisk(
    val zoneName: String,
    val category: String,
    val alertLevel: RiskLevel,
    val description: String
)

data class RiskAnalysisResult(
    val sportType: SportType,
    val overallRiskLevel: RiskLevel,
    val riskPercentage: Float,
    val criticalZones: List<CriticalZoneRisk>,
    val biomechanicalFactors: List<String>,
    val targetedRecommendations: List<String>
)

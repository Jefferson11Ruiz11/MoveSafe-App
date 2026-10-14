package com.app.riesgo.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class DynamicSurveyResult(
    val sportName: String = "Fútbol",
    val zonaAnatomica: String = "",
    val mecanismoLesion: String = "",
    val antiguedad: String = "",
    val estadoRecuperacion: String = "",
    val condicionSeguridadO_Dolor: String = "",
    val indiceVulnerabilidad: Float = 0.0f
) {
    // Constructor sin argumentos requerido por Firestore
    constructor() : this("Fútbol", "", "", "", "", "", 0.0f)

    fun toMap(): Map<String, Any> {
        return mapOf(
            "sportName" to sportName,
            "zonaAnatomica" to zonaAnatomica,
            "mecanismoLesion" to mecanismoLesion,
            "antiguedad" to antiguedad,
            "estadoRecuperacion" to estadoRecuperacion,
            "condicionSeguridadO_Dolor" to condicionSeguridadO_Dolor,
            "indiceVulnerabilidad" to indiceVulnerabilidad
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): DynamicSurveyResult {
            if (map == null) return DynamicSurveyResult()
            return DynamicSurveyResult(
                sportName = map["sportName"] as? String ?: "Fútbol",
                zonaAnatomica = map["zonaAnatomica"] as? String ?: "",
                mecanismoLesion = map["mecanismoLesion"] as? String ?: "",
                antiguedad = map["antiguedad"] as? String ?: "",
                estadoRecuperacion = map["estadoRecuperacion"] as? String ?: "",
                condicionSeguridadO_Dolor = map["condicionSeguridadO_Dolor"] as? String ?: "",
                indiceVulnerabilidad = (map["indiceVulnerabilidad"] as? Number)?.toFloat() ?: 0.0f
            )
        }
    }
}

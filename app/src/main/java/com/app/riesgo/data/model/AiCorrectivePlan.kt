package com.app.riesgo.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ExerciseItem(
    val nombre: String = "",
    val dosificacion: String = "",
    val explicacionTecnica: String = ""
) {
    constructor() : this("", "", "")

    fun toMap(): Map<String, Any> {
        return mapOf(
            "nombre" to nombre,
            "dosificacion" to dosificacion,
            "explicacionTecnica" to explicacionTecnica
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): ExerciseItem {
            if (map == null) return ExerciseItem()
            return ExerciseItem(
                nombre = map["nombre"] as? String ?: "",
                dosificacion = map["dosificacion"] as? String ?: "",
                explicacionTecnica = map["explicacionTecnica"] as? String ?: ""
            )
        }
    }
}

@IgnoreExtraProperties
data class AiCorrectivePlan(
    val resumenEnfoque: String = "",
    val ejerciciosRecomendados: List<ExerciseItem> = emptyList(),
    val pautaFrecuencia: String = "",
    val alertaSeguridad: String = ""
) {
    constructor() : this("", emptyList(), "", "")

    fun toMap(): Map<String, Any> {
        return mapOf(
            "resumenEnfoque" to resumenEnfoque,
            "ejerciciosRecomendados" to ejerciciosRecomendados.map { it.toMap() },
            "pautaFrecuencia" to pautaFrecuencia,
            "alertaSeguridad" to alertaSeguridad
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): AiCorrectivePlan {
            if (map == null) return AiCorrectivePlan()
            @Suppress("UNCHECKED_CAST")
            val rawExercises = map["ejerciciosRecomendados"] as? List<Map<String, Any>> ?: emptyList()
            val exercises = rawExercises.map { ExerciseItem.fromMap(it) }

            return AiCorrectivePlan(
                resumenEnfoque = map["resumenEnfoque"] as? String ?: "",
                ejerciciosRecomendados = exercises,
                pautaFrecuencia = map["pautaFrecuencia"] as? String ?: "",
                alertaSeguridad = map["alertaSeguridad"] as? String ?: ""
            )
        }
    }
}

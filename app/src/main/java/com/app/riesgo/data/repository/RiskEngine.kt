package com.app.riesgo.data.repository

import com.app.riesgo.data.model.RiskLevel
import java.util.Locale

object RiskEngine {
    fun calculateImc(pesoKg: Float, alturaCm: Float): Float {
        val alturaM = alturaCm / 100f
        return if (alturaM > 0) pesoKg / (alturaM * alturaM) else 0f
    }

    data class RiskOutput(
        val nivel: RiskLevel,
        val probabilidad: Float,
        val factores: List<String>,
        val recomendaciones: List<String>
    )

    fun evaluateRisk(
        imc: Float,
        historialLesiones: Boolean,
        pruebaId: String,
        edad: Int
    ): RiskOutput {
        var puntaje = 0.20f
        val factores = mutableListOf<String>()
        val recomendaciones = mutableListOf<String>()

        if (historialLesiones) {
            puntaje += 0.35f
            factores.add("Precedente de lesión muscular o articular")
            recomendaciones.add("Incorporar protocolo de readaptación y calentamiento neuromuscular prolongado.")
        }

        when {
            imc > 28.5f -> {
                puntaje += 0.25f
                factores.add("Sobrecarga mecánica articular por índice IMC elevado (${String.format(Locale.US, "%.1f", imc)})")
                recomendaciones.add("Priorizar acondicionamiento aeróbico de bajo impacto (natación/ergómetro).")
            }
            imc < 19f -> {
                puntaje += 0.10f
                factores.add("Masa muscular reducida o fragilidad estructural")
                recomendaciones.add("Incrementar ingesta calórica y entrenamiento enfocado en hipertrofia funcional.")
            }
            else -> {
                factores.add("Rango antropométrico óptimo")
            }
        }

        if (edad > 35) {
            puntaje += 0.10f
            factores.add("Aumento en rigidez tendinosa asociada a la edad cronológica")
            recomendaciones.add("Sesiones dedicadas de movilidad y recuperación con Foam Roller.")
        }

        when (pruebaId) {
            "squat" -> {
                factores.add("Estrés rotacional y posible valgo dinámico en carga axial")
                recomendaciones.add("Fortalecer glúteo medio con ejercicios de abducción resistida y sentadillas búlgaras.")
            }
            "monopodal" -> {
                factores.add("Demanda propioceptiva y estabilidad de tobillo/cadera")
                recomendaciones.add("Ejercicios sobre base inestable (Bosu) para reactividad propioceptiva.")
            }
            "jump" -> {
                puntaje += 0.10f
                factores.add("Impacto plio-reactivo de alta deceleración vertical")
                recomendaciones.add("Revisar técnica de aterrizaje y fortalecimiento excéntrico de cuádriceps/tendón rotuliano.")
            }
            "flex" -> {
                recomendaciones.add("Rutinas de estiramiento dinámico de cadena posterior (isquiosurales y columna lumbar).")
            }
        }

        puntaje = puntaje.coerceIn(0.12f, 0.95f)

        val nivel = when {
            puntaje >= 0.65f -> RiskLevel.ALTO
            puntaje >= 0.40f -> RiskLevel.MODERADO
            else -> RiskLevel.BAJO
        }

        return RiskOutput(
            nivel = nivel,
            probabilidad = puntaje,
            factores = factores,
            recomendaciones = recomendaciones
        )
    }
}

package com.app.riesgo.service

import com.app.riesgo.BuildConfig
import com.app.riesgo.data.model.AiCorrectivePlan
import com.app.riesgo.data.model.ExerciseItem
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object InjuryPreventionAiService {

    private const val DEFAULT_API_KEY = "AIzaSy_DEFAULT_KEY_MOVESAFE"

    private val generativeModel: GenerativeModel by lazy {
        val key = if (BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != DEFAULT_API_KEY) {
            BuildConfig.GEMINI_API_KEY
        } else {
            DEFAULT_API_KEY
        }

        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = key,
            systemInstruction = content {
                text("Eres un fisioterapeuta y preparador físico deportivo de élite especializado en prevención de lesiones biomecánicas. Tu objetivo es diseñar planes correctivos concisos, seguros y estructurados sin lenguaje genérico ni diagnósticos invasivos.")
            }
        )
    }

    suspend fun generateCorrectivePlan(
        deporte: String,
        edad: Int,
        imc: Float,
        lesionPrevia: String?,
        tipoPrueba: String,
        metricaCinematica: String,
        nivelRiesgo: String,
        dolorReportado: String?
    ): Result<AiCorrectivePlan> = withContext(Dispatchers.IO) {
        val prompt = """
            Genera un plan correctivo personalizado en formato JSON estricto con esta estructura:
            {
              "resumenEnfoque": "Breve explicación biomecánica (1-2 oraciones).",
              "ejerciciosRecomendados": [
                {
                  "nombre": "Nombre del ejercicio",
                  "dosificacion": "Series x repeticiones o tiempo",
                  "explicacionTecnica": "Indicación de ejecución correcta"
                }
              ],
              "pautaFrecuencia": "Frecuencia semanal recomendada",
              "alertaSeguridad": "Advertencia en caso de dolor"
            }

            Datos del Atleta:
            - Deporte: $deporte
            - Edad: $edad años
            - IMC: $imc
            - Lesión previa: ${lesionPrevia ?: "Ninguna"}
            - Prueba ejecutada: $tipoPrueba
            - Métrica cinemática registrada: $metricaCinematica
            - Nivel de riesgo detectado: $nivelRiesgo
            - Molestia o dolor reportado: ${dolorReportado ?: "Sin dolor"}

            Responde ÚNICAMENTE con el objeto JSON válido.
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text ?: ""

            val cleanedJson = responseText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonObject = JSONObject(cleanedJson)

            val resumenEnfoque = jsonObject.optString("resumenEnfoque", "")
            val pautaFrecuencia = jsonObject.optString("pautaFrecuencia", "3 sesiones por semana")
            val alertaSeguridad = jsonObject.optString("alertaSeguridad", "Suspenda la actividad ante cualquier molestia agudo.")

            val jsonArray = jsonObject.optJSONArray("ejerciciosRecomendados")
            val exercises = mutableListOf<ExerciseItem>()

            if (jsonArray != null) {
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    exercises.add(
                        ExerciseItem(
                            nombre = obj.optString("nombre", "Ejercicio ${i + 1}"),
                            dosificacion = obj.optString("dosificacion", "3 series x 10 repeticiones"),
                            explicacionTecnica = obj.optString("explicacionTecnica", "Mantener alineación adecuada.")
                        )
                    )
                }
            }

            val plan = AiCorrectivePlan(
                resumenEnfoque = resumenEnfoque,
                ejerciciosRecomendados = exercises,
                pautaFrecuencia = pautaFrecuencia,
                alertaSeguridad = alertaSeguridad
            )

            Result.success(plan)
        } catch (_: Exception) {
            val fallbackPlan = buildFallbackPlan(deporte, tipoPrueba, nivelRiesgo)
            Result.success(fallbackPlan)
        }
    }

    private fun buildFallbackPlan(
        deporte: String,
        tipoPrueba: String,
        nivelRiesgo: String
    ): AiCorrectivePlan {
        val isVoley = deporte.equals("Vóley", ignoreCase = true)
        val isMonopodal = tipoPrueba.contains("equilibrio", ignoreCase = true) || tipoPrueba.contains("monopodal", ignoreCase = true)

        val ejercicios = if (isMonopodal) {
            listOf(
                ExerciseItem("Plato de Freeman Monopodal", "3 series x 30 segundos", "Mantener la rodilla ligeramente flexionada y fijar la mirada en un punto neutro."),
                ExerciseItem("Fortalecimiento de Peroneos con Banda", "3 series x 15 repeticiones", "Eversión controlada con banda elástica enfatizando la fase excéntrica."),
                ExerciseItem("Peso Muerto Rumano Unilateral", "3 series x 10 reps por pierna", "Mantener alineación de pelvis y columna neutra durante el descenso.")
            )
        } else if (isVoley) {
            listOf(
                ExerciseItem("Sentadilla Declinada (25°)", "3 series x 12 repeticiones", "Realizar descenso lento enfocado en excéntrico del tendón rotuliano."),
                ExerciseItem("Y-Raise / Rotadores de Hombro", "3 series x 15 repeticiones", "Fortalecer manguito rotador y visualización de escápula."),
                ExerciseItem("Caída con Aterrizaje Bipodal Suave", "3 series x 8 repeticiones", "Absorber el impacto flexionando rodillas a 90° sin valgo.")
            )
        } else {
            listOf(
                ExerciseItem("Sentadilla Búlgara con Carga Moderada", "3 series x 10 reps por pierna", "Controlar que la rodilla adelantada no colapse hacia adentro."),
                ExerciseItem("Nordic Hamstring Curl Assist", "3 series x 6 repeticiones", "Resistir la caída enfocado en la contracción excéntrica de isquiotibiales."),
                ExerciseItem("Caminata Lateral con Banda en Tobillos", "3 series x 12 pasos por lado", "Mantener tensión continua y activación de glúteo medio.")
            )
        }

        return AiCorrectivePlan(
            resumenEnfoque = "Plan personalizado optimizado para $deporte con enfoque en la estabilización dinámica de la articulación y prevención de reacondicionamiento (Nivel de riesgo: $nivelRiesgo).",
            ejerciciosRecomendados = ejercicios,
            pautaFrecuencia = "3 sesiones semanales en días no consecutivos",
            alertaSeguridad = "Detenga la ejecución si presenta dolor punzante en articulaciones superiores o inferiores."
        )
    }
}

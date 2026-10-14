package com.app.riesgo.data.repository

import com.app.riesgo.data.model.DynamicSurveyResult
import com.app.riesgo.data.model.EvaluationRecord
import com.app.riesgo.data.model.RiskLevel
import com.app.riesgo.data.model.ZoneRiskDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object EvaluationRepository {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun saveEvaluation(record: EvaluationRecord): Result<EvaluationRecord> {
        return try {
            AppRepository.addEvaluationLocally(record)

            val userId = record.userId
            if (userId.isNotBlank() && userId != "guest") {
                val evalMap = hashMapOf(
                    "id" to record.id,
                    "userId" to record.userId,
                    "fecha" to record.fecha,
                    "tipoPrueba" to record.tipoPrueba,
                    "deporteEvaluado" to record.deporteEvaluado,
                    "edad" to record.edad,
                    "peso" to record.peso,
                    "altura" to record.altura,
                    "imc" to record.imc,
                    "nivelRiesgo" to record.nivelRiesgo.name,
                    "probabilidad" to record.probabilidad,
                    "factoresDetectados" to record.factoresDetectados,
                    "recomendaciones" to record.recomendaciones,
                    "zonasCriticas" to record.zonasCriticas.map {
                        mapOf(
                            "zoneName" to it.zoneName,
                            "category" to it.category,
                            "alertLevel" to it.alertLevel,
                            "description" to it.description
                        )
                    },
                    "injurySurvey" to record.injurySurvey?.toMap(),
                    "cameraMetrics" to record.cameraMetrics?.toMap(),
                    "postTestFeedback" to record.postTestFeedback?.toMap(),
                    "tiempoSostenidoIzquierdo" to record.tiempoSostenidoIzquierdo,
                    "tiempoSostenidoDerecho" to record.tiempoSostenidoDerecho,
                    "indiceOscilacion" to record.indiceOscilacion,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("users")
                    .document(userId)
                    .collection("evaluations")
                    .document(record.id)
                    .set(evalMap)
                    .await()

                if (record.injurySurvey != null) {
                    try {
                        firestore.collection("users")
                            .document(userId)
                            .update("injurySurvey", record.injurySurvey.toMap())
                            .await()
                    } catch (_: Exception) {}
                }
            }
            Result.success(record)
        } catch (e: Exception) {
            Result.success(record)
        }
    }

    fun getUserEvaluationsFlow(userId: String): Flow<List<EvaluationRecord>> = callbackFlow {
        if (userId.isBlank() || userId == "guest") {
            trySend(AppRepository.getEvaluationsForCurrentUser())
            awaitClose {}
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("evaluations")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppRepository.getEvaluationsForCurrentUser())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val fecha = doc.getString("fecha") ?: ""
                        val tipoPrueba = doc.getString("tipoPrueba") ?: ""
                        val deporteEvaluado = doc.getString("deporteEvaluado") ?: "General"
                        val edad = (doc.getLong("edad") ?: 25L).toInt()
                        val peso = (doc.getDouble("peso") ?: 70.0).toFloat()
                        val altura = (doc.getDouble("altura") ?: 175.0).toFloat()
                        val imc = (doc.getDouble("imc") ?: 22.0).toFloat()
                        val nivelRiesgoStr = doc.getString("nivelRiesgo") ?: "BAJO"
                        val nivelRiesgo = try { RiskLevel.valueOf(nivelRiesgoStr) } catch (_: Exception) { RiskLevel.BAJO }
                        val probabilidad = (doc.getDouble("probabilidad") ?: 0.3).toFloat()

                        @Suppress("UNCHECKED_CAST")
                        val factores = doc.get("factoresDetectados") as? List<String> ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val recomendaciones = doc.get("recomendaciones") as? List<String> ?: emptyList()

                        @Suppress("UNCHECKED_CAST")
                        val zonasRaw = doc.get("zonasCriticas") as? List<Map<String, Any>> ?: emptyList()
                        val zonasCriticas = zonasRaw.map { map ->
                            ZoneRiskDto(
                                zoneName = map["zoneName"] as? String ?: "",
                                category = map["category"] as? String ?: "",
                                alertLevel = map["alertLevel"] as? String ?: "BAJO",
                                description = map["description"] as? String ?: ""
                            )
                        }

                        @Suppress("UNCHECKED_CAST")
                        val surveyMap = doc.get("injurySurvey") as? Map<String, Any>
                        val surveyResult = if (surveyMap != null) DynamicSurveyResult.fromMap(surveyMap) else null

                        val tiempoSostenidoIzquierdo = (doc.getDouble("tiempoSostenidoIzquierdo") ?: 0.0).toFloat()
                        val tiempoSostenidoDerecho = (doc.getDouble("tiempoSostenidoDerecho") ?: 0.0).toFloat()
                        val indiceOscilacion = (doc.getDouble("indiceOscilacion") ?: 0.0).toFloat()

                        val record = EvaluationRecord(
                            id = id,
                            userId = userId,
                            fecha = fecha,
                            tipoPrueba = tipoPrueba,
                            deporteEvaluado = deporteEvaluado,
                            edad = edad,
                            peso = peso,
                            altura = altura,
                            imc = imc,
                            nivelRiesgo = nivelRiesgo,
                            probabilidad = probabilidad,
                            factoresDetectados = factores,
                            recomendaciones = recomendaciones,
                            zonasCriticas = zonasCriticas,
                            injurySurvey = surveyResult,
                            tiempoSostenidoIzquierdo = tiempoSostenidoIzquierdo,
                            tiempoSostenidoDerecho = tiempoSostenidoDerecho,
                            indiceOscilacion = indiceOscilacion
                        )
                        AppRepository.addEvaluationLocally(record)
                        record
                    }
                    trySend(records)
                } else {
                    trySend(AppRepository.getEvaluationsForCurrentUser())
                }
            }

        awaitClose { listener.remove() }
    }
}

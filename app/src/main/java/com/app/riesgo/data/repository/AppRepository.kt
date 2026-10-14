package com.app.riesgo.data.repository

import com.app.riesgo.data.model.*
import java.text.SimpleDateFormat
import java.util.*

object AppRepository {
    var currentUser: User? = null
    var currentSurveyResult: DynamicSurveyResult? = null

    val tests = listOf(
        PhysicalTest(
            id = "squat",
            nombre = "Sentadilla Profunda Overhead",
            objetivoBiomecanico = "Detección de valgo dinámico y movilidad torácica",
            articulacionesClave = "Rodilla, Cadera, Tobillo",
            dificultad = TestDifficulty.INTERMEDIO
        ),
        PhysicalTest(
            id = "monopodal",
            nombre = "Equilibrio Monopodal (Y-Balance)",
            objetivoBiomecanico = "Estabilidad central y propiocepción unilateral",
            articulacionesClave = "Tobillo, Glúteo Medio",
            dificultad = TestDifficulty.PRINCIPIANTE
        ),
        PhysicalTest(
            id = "jump",
            nombre = "Salto Vertical (Drop Jump)",
            objetivoBiomecanico = "Fuerza reactiva y control neuromuscular en aterrizaje",
            articulacionesClave = "Tendón Rotuliano, LCA",
            dificultad = TestDifficulty.AVANZADO
        ),
        PhysicalTest(
            id = "flex",
            nombre = "Flexibilidad Sit & Reach",
            objetivoBiomecanico = "Extensibilidad de isquiosurales y columna dorso-lumbar",
            articulacionesClave = "Cadena Posterior",
            dificultad = TestDifficulty.PRINCIPIANTE
        )
    )

    private val usersList = mutableListOf(
        User(
            id = "1",
            nombre = "Carlos Mendoza",
            email = "carlos@sports.com",
            deporte = "Crossfit",
            edad = 28,
            peso = 75.0f,
            altura = 178.0f,
            tieneLesiones = true
        )
    )

    private val evaluaciones = mutableListOf<EvaluationRecord>()

    fun login(email: String, pass: String): Boolean {
        val user = usersList.find { it.email.equals(email.trim(), ignoreCase = true) }
        return if (user != null || pass.isNotBlank()) {
            currentUser = user ?: User(
                id = UUID.randomUUID().toString(),
                nombre = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = email,
                deporte = "Acondicionamiento General",
                edad = 25,
                peso = 70.0f,
                altura = 175.0f,
                tieneLesiones = false
            )
            true
        } else false
    }

    fun register(nombre: String, email: String, deporte: String, edad: Int): Boolean {
        val newUser = User(
            id = UUID.randomUUID().toString(),
            nombre = nombre,
            email = email,
            deporte = deporte,
            edad = edad,
            peso = 70.0f,
            altura = 175.0f,
            tieneLesiones = false
        )
        usersList.add(newUser)
        currentUser = newUser
        return true
    }

    fun logout() {
        currentUser = null
    }

    fun updateUserProfile(
        nombre: String,
        deporte: String,
        edad: Int,
        peso: Float,
        altura: Float,
        tieneLesiones: Boolean
    ) {
        currentUser?.let { curr ->
            val updated = curr.copy(
                nombre = nombre,
                deporte = deporte,
                edad = edad,
                peso = peso,
                altura = altura,
                tieneLesiones = tieneLesiones
            )
            currentUser = updated
            val index = usersList.indexOfFirst { it.id == curr.id }
            if (index != -1) {
                usersList[index] = updated
            }
        }
    }

    fun saveEvaluation(
        tipoPrueba: String,
        deporteEvaluado: String = "General",
        edad: Int,
        peso: Float,
        altura: Float,
        imc: Float,
        nivelRiesgo: RiskLevel,
        probabilidad: Float,
        factores: List<String>,
        recomendaciones: List<String>,
        zonasCriticas: List<ZoneRiskDto> = emptyList(),
        surveyResult: DynamicSurveyResult? = currentSurveyResult,
        tiempoSostenidoIzquierdo: Float = 0f,
        tiempoSostenidoDerecho: Float = 0f,
        indiceOscilacion: Float = 0f
    ): EvaluationRecord {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val record = EvaluationRecord(
            id = UUID.randomUUID().toString(),
            userId = currentUser?.id ?: "guest",
            fecha = formatter.format(Date()),
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
        addEvaluationLocally(record)
        return record
    }

    fun addEvaluationLocally(record: EvaluationRecord) {
        val index = evaluaciones.indexOfFirst { it.id == record.id }
        if (index != -1) {
            evaluaciones[index] = record
        } else {
            evaluaciones.add(0, record)
        }
    }

    fun getEvaluationsForCurrentUser(): List<EvaluationRecord> {
        val uid = currentUser?.id ?: return emptyList()
        return evaluaciones.filter { it.userId == uid }
    }

    fun getEvaluationById(id: String): EvaluationRecord? {
        return evaluaciones.find { it.id == id }
    }
}

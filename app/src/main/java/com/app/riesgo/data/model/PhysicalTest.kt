package com.app.riesgo.data.model

enum class TestDifficulty { PRINCIPIANTE, INTERMEDIO, AVANZADO }

data class PhysicalTest(
    val id: String,
    val nombre: String,
    val objetivoBiomecanico: String,
    val articulacionesClave: String,
    val dificultad: TestDifficulty
)

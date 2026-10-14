package com.app.riesgo.data.model

data class User(
    val id: String,
    val nombre: String,
    val email: String,
    val deporte: String,
    val edad: Int,
    val peso: Float = 70.0f,
    val altura: Float = 175.0f,
    val tieneLesiones: Boolean = false
)

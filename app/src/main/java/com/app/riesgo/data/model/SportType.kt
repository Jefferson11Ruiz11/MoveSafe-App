package com.app.riesgo.data.model

enum class SportType(val displayName: String) {
    FUTBOL("Fútbol"),
    VOLEY("Vóley"),
    GENERAL("Acondicionamiento General");

    companion object {
        fun fromString(str: String?): SportType {
            if (str.isNullOrBlank()) return GENERAL
            val s = str.trim().lowercase()
            return when {
                s.contains("fút") || s.contains("fut") || s.contains("soccer") -> FUTBOL
                s.contains("vól") || s.contains("vol") || s.contains("voley") || s.contains("volley") -> VOLEY
                else -> GENERAL
            }
        }
    }
}

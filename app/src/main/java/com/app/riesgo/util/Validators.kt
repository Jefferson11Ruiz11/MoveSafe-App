package com.app.riesgo.util

import android.util.Patterns

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

object Validators {

    // Sanitizadores en tiempo real (al teclear)
    fun sanitizeName(input: String): String {
        return input.filter { it.isLetter() || it.isWhitespace() }.take(40)
    }

    fun sanitizeSport(input: String): String {
        return input.filter { it.isLetter() || it.isWhitespace() }.take(30)
    }

    fun sanitizeAge(input: String): String {
        return input.filter { it.isDigit() }.take(2)
    }

    fun sanitizeDecimal(input: String, maxChars: Int): String {
        var hasDot = false
        val filtered = StringBuilder()
        for (char in input) {
            if (char.isDigit()) {
                filtered.append(char)
            } else if (char == '.' && !hasDot && filtered.isNotEmpty()) {
                hasDot = true
                filtered.append(char)
            }
        }
        return filtered.take(maxChars).toString()
    }

    fun sanitizeWeight(input: String): String = sanitizeDecimal(input, 5)

    fun sanitizeHeight(input: String): String = sanitizeDecimal(input, 5)

    // Validaciones Puras
    fun validateName(name: String): ValidationResult {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult(false, "El nombre es obligatorio")
            trimmed.length < 3 -> ValidationResult(false, "Mínimo 3 caracteres")
            trimmed.length > 40 -> ValidationResult(false, "Máximo 40 caracteres")
            !trimmed.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> ValidationResult(false, "Solo se permiten letras y espacios")
            else -> ValidationResult(true)
        }
    }

    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult(false, "El correo es obligatorio")
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> ValidationResult(false, "Formato de correo electrónico inválido")
            else -> ValidationResult(true)
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult(false, "La contraseña es obligatoria")
            password.length < 6 -> ValidationResult(false, "Mínimo 6 caracteres")
            else -> ValidationResult(true)
        }
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isEmpty() -> ValidationResult(false, "Confirma tu contraseña")
            confirmPassword != password -> ValidationResult(false, "Las contraseñas no coinciden")
            else -> ValidationResult(true)
        }
    }

    fun validateAge(ageStr: String): ValidationResult {
        val ageInt = ageStr.toIntOrNull()
        return when {
            ageStr.isEmpty() -> ValidationResult(false, "Ingresa la edad")
            ageInt == null -> ValidationResult(false, "Debe ser un número entero")
            ageInt < 10 || ageInt > 90 -> ValidationResult(false, "Edad permitida entre 10 y 90 años")
            else -> ValidationResult(true)
        }
    }

    fun validateWeight(weightStr: String): ValidationResult {
        val weightFloat = weightStr.toFloatOrNull()
        return when {
            weightStr.isEmpty() -> ValidationResult(false, "Ingresa el peso")
            weightFloat == null -> ValidationResult(false, "Número decimal inválido")
            weightFloat < 30.0f || weightFloat > 250.0f -> ValidationResult(false, "Peso permitido entre 30.0 y 250.0 kg")
            else -> ValidationResult(true)
        }
    }

    fun validateHeight(heightStr: String): ValidationResult {
        val heightFloat = heightStr.toFloatOrNull()
        return when {
            heightStr.isEmpty() -> ValidationResult(false, "Ingresa la estatura")
            heightFloat == null -> ValidationResult(false, "Número decimal inválido")
            heightFloat < 100.0f || heightFloat > 230.0f -> ValidationResult(false, "Estatura permitida entre 100.0 y 230.0 cm")
            else -> ValidationResult(true)
        }
    }

    fun validateSport(sport: String): ValidationResult {
        val trimmed = sport.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult(false, "El deporte es obligatorio")
            trimmed.length < 2 -> ValidationResult(false, "Mínimo 2 caracteres")
            trimmed.length > 30 -> ValidationResult(false, "Máximo 30 caracteres")
            else -> ValidationResult(true)
        }
    }
}

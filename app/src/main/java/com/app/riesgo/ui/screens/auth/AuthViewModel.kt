package com.app.riesgo.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.riesgo.data.model.User
import com.app.riesgo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            _loginState.value = AuthState.Error("Por favor ingresa tu correo y contraseña.")
            return
        }

        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.loginUser(cleanEmail, password)
            result.fold(
                onSuccess = { user ->
                    _loginState.value = AuthState.Success(user)
                    onSuccess()
                },
                onFailure = { exception ->
                    _loginState.value = AuthState.Error(
                        exception.localizedMessage ?: "Correo o contraseña incorrectos."
                    )
                }
            )
        }
    }

    fun register(
        nombre: String,
        email: String,
        password: String,
        deporte: String,
        edad: Int,
        onSuccess: () -> Unit
    ) {
        val cleanEmail = email.trim()
        _registerState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.registerUser(
                nombre = nombre.trim(),
                email = cleanEmail,
                password = password,
                deporte = deporte.trim(),
                edad = edad
            )
            result.fold(
                onSuccess = { user ->
                    _registerState.value = AuthState.Success(user)
                    onSuccess()
                },
                onFailure = { exception ->
                    _registerState.value = AuthState.Error(
                        exception.localizedMessage ?: "Error al registrar el usuario."
                    )
                }
            )
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            onResult(false, "Ingresa un correo electrónico válido.")
            return
        }

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(cleanEmail)
            result.fold(
                onSuccess = {
                    onResult(true, "Enlace enviado. Revisa tu bandeja de entrada o spam.")
                },
                onFailure = { exception ->
                    onResult(false, exception.localizedMessage ?: "No se pudo enviar el correo de recuperación.")
                }
            )
        }
    }

    fun clearErrors() {
        _loginState.value = AuthState.Idle
        _registerState.value = AuthState.Idle
    }
}

package com.app.riesgo.data.repository

import com.app.riesgo.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun registerUser(
        nombre: String,
        email: String,
        password: String,
        deporte: String,
        edad: Int,
        peso: Float = 70.0f,
        altura: Float = 175.0f,
        tieneLesiones: Boolean = false
    ): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid = authResult.user?.uid ?: throw Exception("No se pudo obtener el ID del usuario")

            val user = User(
                id = uid,
                nombre = nombre.trim(),
                email = email.trim(),
                deporte = deporte.trim(),
                edad = edad,
                peso = peso,
                altura = altura,
                tieneLesiones = tieneLesiones
            )

            val userMap = hashMapOf(
                "id" to user.id,
                "nombre" to user.nombre,
                "email" to user.email,
                "deporte" to user.deporte,
                "edad" to user.edad,
                "peso" to user.peso,
                "altura" to user.altura,
                "tieneLesiones" to user.tieneLesiones
            )

            firestore.collection("users").document(uid).set(userMap).await()
            AppRepository.currentUser = user
            Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Este correo electrónico ya está registrado."))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("La contraseña debe tener al menos 6 caracteres."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("El formato de correo electrónico es inválido."))
        } catch (e: Exception) {
            Result.failure(Exception("Error al registrar: ${e.localizedMessage ?: "Error desconocido"}"))
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val uid = authResult.user?.uid ?: throw Exception("Usuario no encontrado")

            val doc = firestore.collection("users").document(uid).get().await()
            val user = if (doc.exists()) {
                User(
                    id = doc.getString("id") ?: uid,
                    nombre = doc.getString("nombre") ?: email.substringBefore("@"),
                    email = doc.getString("email") ?: email,
                    deporte = doc.getString("deporte") ?: "General",
                    edad = (doc.getLong("edad") ?: 25L).toInt(),
                    peso = (doc.getDouble("peso") ?: 70.0).toFloat(),
                    altura = (doc.getDouble("altura") ?: 175.0).toFloat(),
                    tieneLesiones = doc.getBoolean("tieneLesiones") ?: false
                )
            } else {
                User(
                    id = uid,
                    nombre = email.substringBefore("@"),
                    email = email,
                    deporte = "General",
                    edad = 25
                )
            }

            AppRepository.currentUser = user
            Result.success(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Correo o contraseña incorrectos."))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("El usuario no existe o ha sido deshabilitado."))
        } catch (e: Exception) {
            Result.failure(Exception("Error al iniciar sesión: ${e.localizedMessage ?: "Error de autenticación"}"))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No existe una cuenta registrada con este correo."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("El formato del correo electrónico no es válido."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Error al enviar el correo de recuperación."))
        }
    }

    suspend fun updateUserProfile(
        nombre: String,
        deporte: String,
        edad: Int,
        peso: Float,
        altura: Float,
        tieneLesiones: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: AppRepository.currentUser?.id
        return try {
            AppRepository.updateUserProfile(nombre, deporte, edad, peso, altura, tieneLesiones)
            if (uid != null) {
                val updateMap = mapOf(
                    "nombre" to nombre,
                    "deporte" to deporte,
                    "edad" to edad,
                    "peso" to peso,
                    "altura" to altura,
                    "tieneLesiones" to tieneLesiones
                )
                firestore.collection("users").document(uid).update(updateMap).await()
            }
            Result.success(Unit)
        } catch (_: Exception) {
            Result.success(Unit)
        }
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        AppRepository.logout()
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null || AppRepository.currentUser != null
    }
}

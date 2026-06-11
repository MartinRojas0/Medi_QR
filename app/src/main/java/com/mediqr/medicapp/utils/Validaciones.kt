package com.mediqr.medicapp.utils

import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Validaciones {

    fun validarEmail(email: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult.Error("El email es requerido")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ValidationResult.Error("Email inválido")
            else -> ValidationResult.Success
        }
    }

    fun validarPassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult.Error("La contraseña es requerida")
            password.length < 6 -> ValidationResult.Error("La contraseña debe tener al menos 6 caracteres")
            !password.any { it.isDigit() } -> ValidationResult.Error("La contraseña debe tener al menos un número")
            !password.any { it.isLetter() } -> ValidationResult.Error("La contraseña debe tener al menos una letra")
            else -> ValidationResult.Success
        }
    }

    fun validarConfirmPassword(password: String, confirmPassword: String): ValidationResult {
        return if (password == confirmPassword) {
            ValidationResult.Success
        } else {
            ValidationResult.Error("Las contraseñas no coinciden")
        }
    }

    fun validarDNI(dni: String): ValidationResult {
        val dniRegex = Regex("^[0-9]{8}$")
        return when {
            dni.isEmpty() -> ValidationResult.Error("El DNI es requerido")
            !dniRegex.matches(dni) -> ValidationResult.Error("DNI inválido (8 dígitos)")
            else -> ValidationResult.Success
        }
    }

    fun validarTelefono(telefono: String): ValidationResult {
        val telefonoRegex = Regex("^[0-9]{9}$")
        return when {
            telefono.isEmpty() -> ValidationResult.Error("El teléfono es requerido")
            !telefonoRegex.matches(telefono) -> ValidationResult.Error("Teléfono inválido (9 dígitos)")
            else -> ValidationResult.Success
        }
    }

    fun validarNombre(nombre: String): ValidationResult {
        return when {
            nombre.isEmpty() -> ValidationResult.Error("El nombre es requerido")
            nombre.length < 3 -> ValidationResult.Error("El nombre debe tener al menos 3 caracteres")
            else -> ValidationResult.Success
        }
    }

    fun validarFechaNacimiento(fecha: String): ValidationResult {
        return try {
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formato.isLenient = false
            val fechaNac = formato.parse(fecha) ?: return ValidationResult.Error("Fecha inválida")

            val calendar = Calendar.getInstance()
            val hoy = calendar.time

            if (fechaNac.after(hoy)) {
                return ValidationResult.Error("La fecha no puede ser futura")
            }

            // Calcular edad
            calendar.time = fechaNac
            val anioNac = calendar.get(Calendar.YEAR)
            val mesNac = calendar.get(Calendar.MONTH)
            val diaNac = calendar.get(Calendar.DAY_OF_MONTH)

            val hoyCalendar = Calendar.getInstance()
            val anioActual = hoyCalendar.get(Calendar.YEAR)
            val mesActual = hoyCalendar.get(Calendar.MONTH)
            val diaActual = hoyCalendar.get(Calendar.DAY_OF_MONTH)

            var edad = anioActual - anioNac
            if (mesActual < mesNac || (mesActual == mesNac && diaActual < diaNac)) {
                edad--
            }

            if (edad < 18) {
                ValidationResult.Error("Debes ser mayor de 18 años")
            } else {
                ValidationResult.Success
            }
        } catch (e: Exception) {
            ValidationResult.Error("Formato inválido (YYYY-MM-DD)")
        }
    }

    fun validarCamiosNoVacios(vararg campos: Pair<String, String>): ValidationResult {
        for ((nombre, valor) in campos) {
            if (valor.isBlank()) {
                return ValidationResult.Error("El campo $nombre es requerido")
            }
        }
        return ValidationResult.Success
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val mensaje: String) : ValidationResult()
}
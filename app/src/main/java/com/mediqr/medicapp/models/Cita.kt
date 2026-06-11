package com.mediqr.medicapp.models

data class Cita(
    val id: String,
    val especialidad: String,
    val medico: String,
    val fecha: String,
    val hora: String,
    val estado: EstadoCita,
    val clinica: String,
    val motivo: String? = null
)

enum class EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    CANCELADA,
    COMPLETADA;

    fun getDisplayName(): String = when(this) {
        PENDIENTE -> "Pendiente"
        CONFIRMADA -> "Confirmada"
        CANCELADA -> "Cancelada"
        COMPLETADA -> "Completada"
    }
}
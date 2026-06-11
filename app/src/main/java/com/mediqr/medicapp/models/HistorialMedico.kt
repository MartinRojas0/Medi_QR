package com.mediqr.medicapp.models

data class HistorialMedico(
    val pacienteId: String,
    val tipoSangre: String,
    val alergias: List<Alergia>,
    val condicionesCronicas: List<CondicionCronica>,
    val medicamentosActuales: List<Medicamento>,
    val vacunas: List<Vacuna>,
    val cirugias: List<Cirugia>
)

data class Alergia(
    val nombre: String,
    val severidad: String // "leve", "moderada", "grave"
)

data class CondicionCronica(
    val nombre: String,
    val diagnostico: String,
    val fechaDiagnostico: String
)

data class Medicamento(
    val nombre: String,
    val dosis: String,
    val frecuencia: String,
    val fechaInicio: String,
    val fechaFin: String? = null
)

data class Vacuna(
    val nombre: String,
    val fechaAplicacion: String,
    val dosis: Int
)

data class Cirugia(
    val nombre: String,
    val fecha: String,
    val hospital: String
)
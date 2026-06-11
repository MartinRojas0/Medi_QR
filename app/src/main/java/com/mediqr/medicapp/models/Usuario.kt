package com.mediqr.medicapp.models

data class Usuario(
    val id: String,
    val email: String,
    val nombreCompleto: String,
    val telefono: String,
    val dni: String,
    val fechaNacimiento: String,
    val grupoSanguineo: String? = null,
    val qrCodeUrl: String? = null
)
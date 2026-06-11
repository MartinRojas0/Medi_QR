package com.mediqr.medicapp.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("mediqr_prefs", Context.MODE_PRIVATE)

    fun guardarToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    fun obtenerToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    fun guardarUsuario(usuarioJson: String) {
        prefs.edit().putString("usuario_data", usuarioJson).apply()
    }

    fun obtenerUsuarioJson(): String? {
        return prefs.getString("usuario_data", null)
    }

    fun guardarUsuarioId(id: String) {
        prefs.edit().putString("usuario_id", id).apply()
    }

    fun obtenerUsuarioId(): String? {
        return prefs.getString("usuario_id", null)
    }

    fun limpiar() {
        prefs.edit().clear().apply()
    }

    fun estaLogueado(): Boolean {
        return obtenerToken() != null
    }
}
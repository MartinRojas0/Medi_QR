package com.mediqr.medicapp.utils

import android.app.ProgressDialog
import android.content.Context

class ProgressHelper(private val context: Context) {

    private var progressDialog: ProgressDialog? = null

    fun mostrar(mensaje: String = "Cargando...", cancelable: Boolean = false) {
        if (progressDialog?.isShowing == true) {
            return
        }
        progressDialog = ProgressDialog(context).apply {
            setMessage(mensaje)
            setCancelable(cancelable)
            show()
        }
    }

    fun ocultar() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    fun estaMostrando(): Boolean = progressDialog?.isShowing == true
}
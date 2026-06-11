package com.mediqr.medicapp.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseAuthHelper(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val tokenManager: TokenManager = TokenManager(context)

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun login(email: String, password: String, callback: (Boolean, String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    tokenManager.guardarToken(userId)
                    tokenManager.guardarUsuarioId(userId)
                    callback(true, "Éxito")
                } else {
                    val errorMsg = task.exception?.message ?: "Error al iniciar sesión"
                    callback(false, errorMsg)
                }
            }
    }

    fun register(email: String, password: String, nombre: String, callback: (Boolean, String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""

                    val usuarioData = hashMapOf(
                        "nombre" to nombre,
                        "email" to email,
                        "fechaRegistro" to System.currentTimeMillis()
                    )

                    firestore.collection("usuarios")
                        .document(userId)
                        .set(usuarioData)
                        .addOnSuccessListener {
                            tokenManager.guardarToken(userId)
                            tokenManager.guardarUsuarioId(userId)
                            callback(true, "Éxito")
                        }
                        .addOnFailureListener { e ->
                            callback(true, "Usuario creado pero error: ${e.message}")
                        }
                } else {
                    val errorMsg = task.exception?.message ?: "Error al registrarse"
                    callback(false, errorMsg)
                }
            }
    }

    fun getUserData(callback: (Boolean, Map<String, Any>?, String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false, null, "Usuario no autenticado")
            return
        }

        firestore.collection("usuarios")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    callback(true, document.data, null)
                } else {
                    callback(false, null, "No se encontraron datos")
                }
            }
            .addOnFailureListener { e ->
                callback(false, null, e.message)
            }
    }

    fun logout() {
        auth.signOut()
        tokenManager.limpiar()
    }
}
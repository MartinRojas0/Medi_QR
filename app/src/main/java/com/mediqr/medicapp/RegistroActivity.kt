package com.mediqr.medicapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mediqr.medicapp.datos.RepositorioMedico
import com.mediqr.medicapp.utils.FirebaseAuthHelper
import com.mediqr.medicapp.utils.ProgressHelper
import com.mediqr.medicapp.utils.Validaciones
import com.mediqr.medicapp.utils.ValidationResult

class RegistroActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginTextView: TextView
    private lateinit var progressHelper: ProgressHelper
    private lateinit var authHelper: FirebaseAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializar vistas
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        registerButton = findViewById(R.id.registerButton)
        loginTextView = findViewById(R.id.loginTextView)

        progressHelper = ProgressHelper(this)
        authHelper = FirebaseAuthHelper(this)

        setupListeners()
    }

    private fun setupListeners() {
        registerButton.setOnClickListener {
            validarYRegistrar()
        }

        loginTextView.setOnClickListener {
            val intent = Intent(this, InicioSesionActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validarYRegistrar() {
        val nombre = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        if (nombre.isEmpty()) {
            nameEditText.error = "El nombre es requerido"
            nameEditText.requestFocus()
            return
        }

        if (nombre.length < 3) {
            nameEditText.error = "El nombre debe tener al menos 3 caracteres"
            nameEditText.requestFocus()
            return
        }

        val emailValidation = Validaciones.validarEmail(email)
        if (emailValidation is ValidationResult.Error) {
            emailEditText.error = emailValidation.mensaje
            emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "La contraseña es requerida"
            passwordEditText.requestFocus()
            return
        }

        if (password.length < 6) {
            passwordEditText.error = "La contraseña debe tener al menos 6 caracteres"
            passwordEditText.requestFocus()
            return
        }

        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Las contraseñas no coinciden"
            confirmPasswordEditText.requestFocus()
            return
        }

        realizarRegistro(nombre, email, password)
    }

    private fun realizarRegistro(nombre: String, email: String, password: String) {
        progressHelper.mostrar("Registrando usuario...")
        registerButton.isEnabled = false

        authHelper.register(email, password, nombre) { success, message ->
            if (success) {
                val userId = authHelper.getCurrentUser()?.uid ?: ""
                val repositorio = RepositorioMedico.getInstancia(this)
                repositorio.setCurrentUserId(userId)

                val perfil = repositorio.getPerfil()
                val nombrePartes = nombre.split(" ", limit = 2)
                perfil.setNombre(nombrePartes[0])
                if (nombrePartes.size > 1) {
                    perfil.setApellido(nombrePartes[1])
                } else {
                    perfil.setApellido("")
                }
                perfil.setCorreo(email)

                perfil.setDni("")
                perfil.setTelefono("")
                perfil.setCalle("")
                perfil.setDistrito("")
                perfil.setNumeroCasa("")

                repositorio.guardarPerfil(perfil)

                repositorio.guardarPerfilEnFirestore(perfil, object : RepositorioMedico.OnPerfilGuardadoListener {
                    override fun onSuccess() {
                        progressHelper.ocultar()
                        registerButton.isEnabled = true
                        Toast.makeText(this@RegistroActivity, "¡Bienvenido $nombre!", Toast.LENGTH_LONG).show()
                        irAPrincipal()
                    }

                    override fun onError(error: String) {
                        progressHelper.ocultar()
                        registerButton.isEnabled = true
                        // Fallback: Aunque falle el internet de subida, se le permite ingresar con el estado local inicializado
                        Toast.makeText(this@RegistroActivity, "Perfil local listo. Nube pendiente: $error", Toast.LENGTH_SHORT).show()
                        irAPrincipal()
                    }
                })
            } else {
                progressHelper.ocultar()
                registerButton.isEnabled = true
                Toast.makeText(this@RegistroActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun irAPrincipal() {
        val intent = Intent(this, PrincipalActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
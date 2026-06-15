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
import com.mediqr.medicapp.utils.TokenManager
import com.mediqr.medicapp.utils.Validaciones
import com.mediqr.medicapp.utils.ValidationResult

class InicioSesionActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvRegistrar: TextView
    private lateinit var tokenManager: TokenManager
    private lateinit var progressHelper: ProgressHelper
    private lateinit var authHelper: FirebaseAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar vistas
        etEmail = findViewById(R.id.emailEditText)
        etPassword = findViewById(R.id.passwordEditText)
        btnIniciarSesion = findViewById(R.id.loginButton)
        tvRegistrar = findViewById(R.id.registerTextView)

        tokenManager = TokenManager(this)
        progressHelper = ProgressHelper(this)
        authHelper = FirebaseAuthHelper(this)

        // Corregido: Si ya está logueado, descarga la data de la nube antes de ir al Home
        if (authHelper.isUserLoggedIn()) {
            val userId = authHelper.getCurrentUser()?.uid ?: ""
            val repositorio = RepositorioMedico.getInstancia(this)
            repositorio.setCurrentUserId(userId)

            progressHelper.mostrar("Sincronizando datos...")
            repositorio.cargarPerfilDesdeFirestore(object : RepositorioMedico.OnPerfilCargadoListener {
                override fun onSuccess() {
                    progressHelper.ocultar()
                    irAPrincipal()
                }
                override fun onError(error: String) {
                    progressHelper.ocultar()
                    // Si falla la red, abre igual para permitir el uso con la caché local
                    irAPrincipal()
                }
            })
            return;
        }

        setupListeners()
    }

    private fun setupListeners() {
        btnIniciarSesion.setOnClickListener {
            validarYLogin()
        }

        tvRegistrar.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun validarYLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        val emailValidation = Validaciones.validarEmail(email)
        if (emailValidation is ValidationResult.Error) {
            etEmail.error = emailValidation.mensaje
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "La contraseña es requerida"
            etPassword.requestFocus()
            return
        }

        realizarLogin(email, password)
    }

    private fun realizarLogin(email: String, password: String) {
        progressHelper.mostrar("Iniciando sesión...")
        btnIniciarSesion.isEnabled = false

        authHelper.login(email, password) { success, message ->
            if (success) {
                val userId = authHelper.getCurrentUser()?.uid ?: ""
                val repositorio = RepositorioMedico.getInstancia(this)
                repositorio.setCurrentUserId(userId)

                repositorio.cargarPerfilDesdeFirestore(object : RepositorioMedico.OnPerfilCargadoListener {
                    override fun onSuccess() {
                        progressHelper.ocultar()
                        btnIniciarSesion.isEnabled = true
                        Toast.makeText(this@InicioSesionActivity, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                        irAPrincipal()
                    }

                    override fun onError(error: String) {
                        progressHelper.ocultar()
                        btnIniciarSesion.isEnabled = true
                        Toast.makeText(this@InicioSesionActivity, "Error al cargar perfil: $error", Toast.LENGTH_SHORT).show()
                        irAPrincipal()
                    }
                })
            } else {
                progressHelper.ocultar()
                btnIniciarSesion.isEnabled = true
                Toast.makeText(this@InicioSesionActivity, message, Toast.LENGTH_LONG).show()
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
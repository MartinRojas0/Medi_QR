package com.mediqr.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Actividad de Inicio de Sesión.
 */
public class InicioSesionActivity extends AppCompatActivity {

    private EditText correoEditText;
    private EditText passEditText;
    private Button botonIngresar;
    private TextView textoRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        correoEditText = findViewById(R.id.emailEditText);
        passEditText = findViewById(R.id.passwordEditText);
        botonIngresar = findViewById(R.id.loginButton);
        textoRegistro = findViewById(R.id.registerTextView);

        botonIngresar.setOnClickListener(v -> {
            // Navegación a la actividad principal
            Intent intent = new Intent(InicioSesionActivity.this, PrincipalActivity.class);
            startActivity(intent);
            finish();
        });

        textoRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(InicioSesionActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }
}
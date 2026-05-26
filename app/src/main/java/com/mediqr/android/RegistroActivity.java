package com.mediqr.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Actividad de Registro.
 */
public class RegistroActivity extends AppCompatActivity {

    private EditText nombreEditText;
    private EditText correoEditText;
    private EditText passEditText;
    private EditText confirmaPassEditText;
    private Button botonRegistro;
    private TextView textoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nombreEditText = findViewById(R.id.nameEditText);
        correoEditText = findViewById(R.id.emailEditText);
        passEditText = findViewById(R.id.passwordEditText);
        confirmaPassEditText = findViewById(R.id.confirmPasswordEditText);
        botonRegistro = findViewById(R.id.registerButton);
        textoLogin = findViewById(R.id.loginTextView);

        botonRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(RegistroActivity.this, PrincipalActivity.class);
            startActivity(intent);
            finish();
        });

        textoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegistroActivity.this, InicioSesionActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
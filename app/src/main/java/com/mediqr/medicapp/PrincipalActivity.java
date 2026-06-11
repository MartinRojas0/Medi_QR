package com.mediqr.medicapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Actividad Principal: Contenedor de fragmentos (Inicio, Salud, Citas, Perfil).
 * Implementa navegación fluida y manejo del botón de retroceso.
 */
public class PrincipalActivity extends AppCompatActivity {

    private BottomNavigationView navegacionInferior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navegacionInferior = findViewById(R.id.bottomNavigationView);

        // Carga del fragmento inicial (Dashboard)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new FragmentoInicio())
                    .commit();
        }

        navegacionInferior.setOnNavigationItemSelectedListener(item -> {
            Fragment fragmentoSeleccionado = null;

            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                fragmentoSeleccionado = new FragmentoInicio();
            } else if (id == R.id.navigation_health) {
                fragmentoSeleccionado = new FragmentoSalud();
            } else if (id == R.id.navigation_appointments) {
                fragmentoSeleccionado = new FragmentoCitas();
            } else if (id == R.id.navigation_profile) {
                fragmentoSeleccionado = new FragmentoPerfil();
            }

            if (fragmentoSeleccionado != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, fragmentoSeleccionado)
                        .commit();
                return true;
            }
            return false;
        });
    }

    /**
     * Permite a otros fragmentos cambiar la pestaña activa (ej. desde accesos rápidos).
     */
    public void navegarATab(int idItem) {
        navegacionInferior.setSelectedItemId(idItem);
    }

    /**
     * Maneja el botón de retroceso del sistema para evitar cierres accidentales.
     * Si el usuario no está en Inicio, lo redirige allí antes de salir.
     */
    @Override
    public void onBackPressed() {
        if (navegacionInferior.getSelectedItemId() != R.id.navigation_home) {
            navegarATab(R.id.navigation_home);
        } else {
            super.onBackPressed();
        }
    }
}
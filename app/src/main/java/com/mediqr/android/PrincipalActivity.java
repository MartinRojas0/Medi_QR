package com.mediqr.android;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Actividad Principal: Contenedor de fragmentos.
 */
public class PrincipalActivity extends AppCompatActivity {

    private BottomNavigationView navegacionInferior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navegacionInferior = findViewById(R.id.bottomNavigationView);

        // Carga del fragmento inicial
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new InicioFragment())
                    .commit();
        }

        navegacionInferior.setOnNavigationItemSelectedListener(item -> {
            Fragment fragmentoSeleccionado = null;

            if (item.getItemId() == R.id.navigation_home) {
                fragmentoSeleccionado = new InicioFragment();
            } else if (item.getItemId() == R.id.navigation_health) {
                fragmentoSeleccionado = new SaludFragment();
            } else if (item.getItemId() == R.id.navigation_appointments) {
                fragmentoSeleccionado = new CitasFragment();
            } else if (item.getItemId() == R.id.navigation_profile) {
                fragmentoSeleccionado = new PerfilFragment();
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

    public void navegarATab(int idItem) {
        navegacionInferior.setSelectedItemId(idItem);
    }
}
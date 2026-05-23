package com.mediqr.android;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new InicioFragment())
                    .commit();
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.navigation_home) {
                    selectedFragment = new InicioFragment();
                } else if (item.getItemId() == R.id.navigation_health) {
                    selectedFragment = new SaludFragment();
                } else if (item.getItemId() == R.id.navigation_appointments) {
                    selectedFragment = new CitasFragment();
                } else if (item.getItemId() == R.id.navigation_profile) {
                    selectedFragment = new PerfilFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            }
        });
    }

    public void navigateToTab(int itemId) {
        bottomNavigationView.setSelectedItemId(itemId);
    }
}
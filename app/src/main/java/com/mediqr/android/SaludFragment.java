package com.mediqr.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;

public class SaludFragment extends Fragment {

    private TextView allergiesText, bloodTypeText, chronicConditionsText, currentMedicationsText;
    private SharedPreferences sharedPrefs;
    private static final String PREFS_NAME = "MediQR_Salud";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_salud, container, false);

        sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        allergiesText = view.findViewById(R.id.allergiesText);
        bloodTypeText = view.findViewById(R.id.bloodTypeText);
        chronicConditionsText = view.findViewById(R.id.chronicConditionsText);
        currentMedicationsText = view.findViewById(R.id.currentMedicationsText);
        TextView documentsText = view.findViewById(R.id.documentsText);

        loadHealthData();

        allergiesText.setOnClickListener(v -> showEditDialog("Alergias", "user_allergies", allergiesText));
        bloodTypeText.setOnClickListener(v -> showBloodTypeDialog());
        chronicConditionsText.setOnClickListener(v -> showEditDialog("Condiciones Crónicas", "user_chronic", chronicConditionsText));
        currentMedicationsText.setOnClickListener(v -> showEditDialog("Medicamentos Actuales", "user_meds", currentMedicationsText));

        documentsText.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), DocumentosActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadHealthData() {
        allergiesText.setText(sharedPrefs.getString("user_allergies", "Ninguna conocida"));
        bloodTypeText.setText(sharedPrefs.getString("user_blood_type", "A+ (Positivo)"));
        chronicConditionsText.setText(sharedPrefs.getString("user_chronic", "Hipertensión leve"));
        currentMedicationsText.setText(sharedPrefs.getString("user_meds", "Enalapril 10mg / día"));
    }

    private void showEditDialog(String title, String prefKey, TextView targetTextView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Editar " + title);

        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_simple, null);
        final TextInputEditText input = viewInflated.findViewById(R.id.editSimpleValue);
        input.setText(sharedPrefs.getString(prefKey, targetTextView.getText().toString()));
        builder.setView(viewInflated);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                showSaveConfirmation(prefKey, newValue, targetTextView);
            }
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showBloodTypeDialog() {
        String[] types = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Seleccione Tipo de Sangre");
        
        builder.setItems(types, (dialog, which) -> {
            showSaveConfirmation("user_blood_type", types[which], bloodTypeText);
        });
        
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void showSaveConfirmation(String prefKey, String newValue, TextView targetTextView) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Guardar cambios")
                .setMessage("¿Estás seguro de guardar estos cambios?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    sharedPrefs.edit().putString(prefKey, newValue).apply();
                    targetTextView.setText(newValue);
                    Toast.makeText(getContext(), "Cambios guardados correctamente", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
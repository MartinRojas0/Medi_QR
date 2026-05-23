package com.mediqr.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.mediqr.android.data.MedicalRepository;
import com.mediqr.android.models.PatientProfile;

/**
 * SaludFragment: Ficha médica del paciente.
 * Lógica desacoplada mediante MedicalRepository.
 */
public class SaludFragment extends Fragment {

    private TextView allergiesText, bloodTypeText, chronicConditionsText, currentMedicationsText;
    private MedicalRepository repository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_salud, container, false);

        repository = MedicalRepository.getInstance(requireContext());

        allergiesText = view.findViewById(R.id.allergiesText);
        bloodTypeText = view.findViewById(R.id.bloodTypeText);
        chronicConditionsText = view.findViewById(R.id.chronicConditionsText);
        currentMedicationsText = view.findViewById(R.id.currentMedicationsText);
        TextView documentsText = view.findViewById(R.id.documentsText);

        loadHealthData();

        // UI Interactions
        allergiesText.setOnClickListener(v -> showEditDialog("Alergias", "allergies"));
        bloodTypeText.setOnClickListener(v -> showBloodTypeDialog());
        chronicConditionsText.setOnClickListener(v -> showEditDialog("Condiciones Crónicas", "chronic"));
        currentMedicationsText.setOnClickListener(v -> showEditDialog("Medicamentos Actuales", "meds"));

        documentsText.setOnClickListener(v -> startActivity(new Intent(getContext(), DocumentosActivity.class)));

        return view;
    }

    private void loadHealthData() {
        PatientProfile profile = repository.getProfile();
        allergiesText.setText(profile.getAllergies());
        bloodTypeText.setText(profile.getBloodType());
        chronicConditionsText.setText(profile.getChronicConditions());
        currentMedicationsText.setText(profile.getCurrentMedications());
    }

    private void showEditDialog(String title, String fieldType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Editar " + title);

        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_simple, null);
        final TextInputEditText input = viewInflated.findViewById(R.id.editSimpleValue);
        
        PatientProfile current = repository.getProfile();
        if (fieldType.equals("allergies")) input.setText(current.getAllergies());
        else if (fieldType.equals("chronic")) input.setText(current.getChronicConditions());
        else if (fieldType.equals("meds")) input.setText(current.getCurrentMedications());

        builder.setView(viewInflated);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                showSaveConfirmation(fieldType, newValue);
            }
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showBloodTypeDialog() {
        String[] types = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccione Tipo de Sangre")
                .setItems(types, (dialog, which) -> showSaveConfirmation("blood", types[which]))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showSaveConfirmation(String fieldType, String newValue) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Guardar cambios")
                .setMessage("¿Estás seguro de guardar estos cambios?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    PatientProfile profile = repository.getProfile();
                    if (fieldType.equals("allergies")) profile.setAllergies(newValue);
                    else if (fieldType.equals("blood")) profile.setBloodType(newValue);
                    else if (fieldType.equals("chronic")) profile.setChronicConditions(newValue);
                    else if (fieldType.equals("meds")) profile.setCurrentMedications(newValue);
                    
                    repository.saveHealthData(profile.getBloodType(), profile.getAllergies(), 
                            profile.getChronicConditions(), profile.getCurrentMedications());
                    
                    loadHealthData();
                    Toast.makeText(getContext(), "Cambios guardados correctamente", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
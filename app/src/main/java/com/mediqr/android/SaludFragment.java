package com.mediqr.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.mediqr.android.data.MedicalRepository;
import com.mediqr.android.models.PatientProfile;

/**
 * SaludFragment: Deeply integrated and crash-resistant clinical record.
 */
public class SaludFragment extends Fragment {

    private MedicalRepository repository;
    private View rowBlood, rowAllergies, rowMeds, rowChronic, rowVaccines, rowSurgeries;
    private MaterialCardView cardAlert;
    private TextView tvAlertText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_salud, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            repository = MedicalRepository.getInstance(requireContext());

            // Safe binding with null checks
            cardAlert = view.findViewById(R.id.cardCriticalAlert);
            tvAlertText = view.findViewById(R.id.tvCriticalAllergies);

            rowBlood = view.findViewById(R.id.rowBlood);
            rowAllergies = view.findViewById(R.id.rowAllergies);
            rowMeds = view.findViewById(R.id.rowMeds);
            rowChronic = view.findViewById(R.id.rowChronic);
            rowVaccines = view.findViewById(R.id.rowVaccines);
            rowSurgeries = view.findViewById(R.id.rowSurgeries);

            initUI();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al cargar expediente", Toast.LENGTH_SHORT).show();
        }
    }

    private void initUI() {
        setupRow(rowBlood, "Grupo Sanguíneo", R.drawable.ic_heart, v -> showBloodTypeDialog());
        setupRow(rowAllergies, "Alergias", R.drawable.ic_health, v -> showEditDialog("Alergias", "allergies"));
        setupRow(rowMeds, "Medicamentos", R.drawable.ic_medication, v -> showEditDialog("Medicamentos", "meds"));
        setupRow(rowChronic, "Condiciones Crónicas", R.drawable.ic_history, v -> showEditDialog("Condiciones", "chronic"));
        
        setupRow(rowVaccines, "Vacunas", R.drawable.ic_appointments, v -> 
                Toast.makeText(getContext(), "Historial de vacunas en desarrollo", Toast.LENGTH_SHORT).show());
        setupRow(rowSurgeries, "Cirugías", R.drawable.ic_documents, v -> 
                Toast.makeText(getContext(), "Módulo de cirugías en desarrollo", Toast.LENGTH_SHORT).show());

        loadData();
    }

    private void setupRow(View row, String label, int iconRes, View.OnClickListener listener) {
        if (row != null) {
            TextView tvLabel = row.findViewById(R.id.rowLabel);
            ImageView ivIcon = row.findViewById(R.id.rowIcon);
            if (tvLabel != null) tvLabel.setText(label);
            if (ivIcon != null) ivIcon.setImageResource(iconRes);
            row.setOnClickListener(listener);
        }
    }

    private void loadData() {
        if (!isAdded() || repository == null) return;
        
        PatientProfile profile = repository.getProfile();
        if (profile == null) return;
        
        updateRowValue(rowBlood, profile.getBloodType());
        updateRowValue(rowAllergies, profile.getAllergies());
        updateRowValue(rowMeds, profile.getCurrentMedications());
        updateRowValue(rowChronic, profile.getChronicConditions());
        updateRowValue(rowVaccines, "Esquema completo");
        updateRowValue(rowSurgeries, "Sin registros");

        // UI Feedback Connection
        if (cardAlert != null) {
            String allergies = profile.getAllergies().toLowerCase();
            if (allergies.contains("grave") || allergies.contains("penicilina") || allergies.contains("anafil")) {
                cardAlert.setVisibility(View.VISIBLE);
                if (tvAlertText != null) tvAlertText.setText("RIESGO MÉDICO: " + profile.getAllergies());
            } else {
                cardAlert.setVisibility(View.GONE);
            }
        }
    }

    private void updateRowValue(View row, String value) {
        if (row != null) {
            TextView tvValue = row.findViewById(R.id.rowValue);
            if (tvValue != null) tvValue.setText(value != null ? value : "No especificado");
        }
    }

    private void showEditDialog(String title, String fieldType) {
        if (!isAdded()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Actualizar " + title);
        
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_simple, null);
        final TextInputEditText input = view.findViewById(R.id.editSimpleValue);
        
        PatientProfile p = repository.getProfile();
        if (fieldType.equals("allergies")) input.setText(p.getAllergies());
        else if (fieldType.equals("chronic")) input.setText(p.getChronicConditions());
        else if (fieldType.equals("meds")) input.setText(p.getCurrentMedications());

        builder.setView(view);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (!val.isEmpty()) confirmSave(fieldType, val);
        });
        builder.setNegativeButton("Cerrar", null);
        builder.show();
    }

    private void showBloodTypeDialog() {
        if (!isAdded()) return;
        String[] types = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar Tipo de Sangre")
                .setItems(types, (dialog, which) -> confirmSave("blood", types[which]))
                .show();
    }

    private void confirmSave(String field, String value) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Cambios")
                .setMessage("¿Estás seguro de actualizar tu información médica?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    PatientProfile p = repository.getProfile();
                    if (field.equals("allergies")) p.setAllergies(value);
                    else if (field.equals("blood")) p.setBloodType(value);
                    else if (field.equals("chronic")) p.setChronicConditions(value);
                    else if (field.equals("meds")) p.setCurrentMedications(value);
                    
                    repository.saveHealthData(p.getBloodType(), p.getAllergies(), p.getChronicConditions(), p.getCurrentMedications());
                    loadData();
                    Toast.makeText(getContext(), "Información actualizada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
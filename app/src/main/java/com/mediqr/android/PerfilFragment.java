package com.mediqr.android;

import android.os.Bundle;
import android.text.InputFilter;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.mediqr.android.data.MedicalRepository;
import com.mediqr.android.models.Cita;
import com.mediqr.android.models.PatientProfile;

import java.util.List;

public class PerfilFragment extends Fragment {

    private TextView nameText, ageText, dniText, emailText, phoneText, addressText, profileNameHeader;
    private MedicalRepository repository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        repository = MedicalRepository.getInstance(requireContext());

        profileNameHeader = view.findViewById(R.id.profileNameHeader);
        nameText = view.findViewById(R.id.nameText);
        ageText = view.findViewById(R.id.ageText);
        dniText = view.findViewById(R.id.dniText);
        emailText = view.findViewById(R.id.emailText);
        phoneText = view.findViewById(R.id.phoneText);
        addressText = view.findViewById(R.id.addressText);
        Button editProfileButton = view.findViewById(R.id.editProfileButton);
        Button btnExportPDF = view.findViewById(R.id.btnExportPDF);

        loadProfileData();

        editProfileButton.setOnClickListener(v -> showEditProfileDialog());
        btnExportPDF.setOnClickListener(v -> exportMedicalRecord());

        return view;
    }

    private void loadProfileData() {
        PatientProfile profile = repository.getProfile();
        nameText.setText(profile.getFullName());
        profileNameHeader.setText(profile.getFullName());
        ageText.setText(profile.getAge() + " años");
        dniText.setText(profile.getDni());
        emailText.setText(profile.getEmail());
        phoneText.setText(profile.getPhone());
        addressText.setText(profile.getFullAddress());
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(view);

        TextInputEditText editFirst = view.findViewById(R.id.editFirstName);
        TextInputEditText editLast = view.findViewById(R.id.editLastName);
        TextInputEditText editAge = view.findViewById(R.id.editAge);
        TextInputEditText editDNI = view.findViewById(R.id.editDNI);
        TextInputEditText editEmail = view.findViewById(R.id.editEmail);
        TextInputEditText editPhone = view.findViewById(R.id.editPhone);
        TextInputEditText editStreet = view.findViewById(R.id.editStreet);
        TextInputEditText editDistrict = view.findViewById(R.id.editDistrict);
        TextInputEditText editHouse = view.findViewById(R.id.editHouseNum);
        
        PatientProfile current = repository.getProfile();
        editFirst.setText(current.getFirstName());
        editLast.setText(current.getLastName());
        editAge.setText(String.valueOf(current.getAge()));
        editDNI.setText(current.getDni());
        editEmail.setText(current.getEmail());
        editPhone.setText(current.getPhone());
        editStreet.setText(current.getStreet());
        editDistrict.setText(current.getDistrict());
        editHouse.setText(current.getHouseNum());

        InputFilter letterFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.isLetter(source.charAt(i)) && !Character.isSpaceChar(source.charAt(i))) return "";
            }
            return null;
        };
        editFirst.setFilters(new InputFilter[]{letterFilter});
        editLast.setFilters(new InputFilter[]{letterFilter});

        AlertDialog dialog = builder.create();

        view.findViewById(R.id.btnSaveProfile).setOnClickListener(v -> {
            String fName = editFirst.getText().toString().trim();
            String lName = editLast.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            
            if (fName.isEmpty() || lName.isEmpty()) {
                Toast.makeText(getContext(), "Nombre y Apellido son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getContext(), "Formato de email inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            PatientProfile newP = repository.getProfile();
            newP.setFirstName(fName);
            newP.setLastName(lName);
            newP.setAge(Integer.parseInt(editAge.getText().toString()));
            newP.setDni(editDNI.getText().toString().trim());
            newP.setEmail(email);
            newP.setPhone(editPhone.getText().toString().trim());
            newP.setStreet(editStreet.getText().toString().trim());
            newP.setDistrict(editDistrict.getText().toString().trim());
            newP.setHouseNum(editHouse.getText().toString().trim());

            repository.saveProfile(newP);
            loadProfileData();
            Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        view.findViewById(R.id.btnCancelEdit).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void exportMedicalRecord() {
        PatientProfile p = repository.getProfile();
        List<Cita> history = repository.getAppointments(false);
        
        StringBuilder sb = new StringBuilder();
        sb.append("📄 HISTORIAL MÉDICO CONSOLIDADO\n");
        sb.append("---------------------------------\n\n");
        sb.append("👤 PACIENTE: ").append(p.getFullName()).append("\n");
        sb.append("DNI: ").append(p.getDni()).append("\n");
        sb.append("SANGRE: ").append(p.getBloodType()).append("\n");
        sb.append("ALERGIAS: ").append(p.getAllergies()).append("\n\n");
        
        sb.append("🏥 REGISTROS DE CONSULTA:\n");
        int count = 0;
        for (Cita c : history) {
            if (c.isAtendida()) {
                sb.append("• ").append(c.getFecha()).append(" - ").append(c.getEspecialidad()).append("\n");
                sb.append("  Doctor: ").append(c.getDoctor()).append("\n");
                count++;
            }
        }
        if (count == 0) sb.append("- No hay registros históricos disponibles.\n");
        
        sb.append("\n---------------------------------\n");
        sb.append("Generado por MediQR App Premium");

        new AlertDialog.Builder(requireContext())
                .setTitle("Reporte Médico PDF")
                .setMessage(sb.toString())
                .setPositiveButton("Compartir", (dialog, which) -> Toast.makeText(getContext(), "Enviando archivo...", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Cerrar", null)
                .show();
    }
}
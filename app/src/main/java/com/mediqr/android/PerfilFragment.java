package com.mediqr.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;

public class PerfilFragment extends Fragment {

    private TextView nameText, ageText, dniText, emailText, phoneText, addressText, profileNameHeader;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MediQR_Prefs";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

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
        btnExportPDF.setOnClickListener(v -> {
            String fullName = sharedPreferences.getString("user_name", "Martin Rojas");
            String dni = sharedPreferences.getString("user_dni", "12.345.678");
            
            SharedPreferences saludPrefs = requireActivity().getSharedPreferences("MediQR_Salud", Context.MODE_PRIVATE);
            String allergies = saludPrefs.getString("user_allergies", "Ninguna conocida");
            String blood = saludPrefs.getString("user_blood_type", "A+ (Positivo)");
            String chronic = saludPrefs.getString("user_chronic", "Hipertensión leve");
            String meds = saludPrefs.getString("user_meds", "Enalapril 10mg / día");

            String report = "📄 HISTORIAL MÉDICO - MEDIQR\n\n" +
                    "👤 PACIENTE:\n" +
                    "Nombre: " + fullName + "\n" +
                    "DNI: " + dni + "\n\n" +
                    "🏥 DATOS DE SALUD:\n" +
                    "Tipo Sangre: " + blood + "\n" +
                    "Alergias: " + allergies + "\n" +
                    "Condiciones: " + chronic + "\n" +
                    "Medicación: " + meds + "\n\n" +
                    "✅ Estado: Exportado correctamente";

            AlertDialog.Builder reportBuilder = new AlertDialog.Builder(requireContext());
            reportBuilder.setTitle("Reporte Generado");
            reportBuilder.setMessage(report);
            reportBuilder.setPositiveButton("Cerrar", null);
            reportBuilder.show();
        });

        return view;
    }

    private void loadProfileData() {
        String fullName = sharedPreferences.getString("user_name", "Martin Rojas");
        String age = sharedPreferences.getString("user_age", "20 años");
        String dni = sharedPreferences.getString("user_dni", "12.345.678");
        String email = sharedPreferences.getString("user_email", "martin.rojas@email.com");
        String phone = sharedPreferences.getString("user_phone", "+54 9 11 1234-5678");
        
        // Cargar dirección compuesta
        String street = sharedPreferences.getString("user_street", "Av. Siempreviva");
        String district = sharedPreferences.getString("user_district", "Springfield");
        String houseNum = sharedPreferences.getString("user_houseNum", "742");
        String fullAddress = street + " " + houseNum + ", " + district;

        nameText.setText(fullName);
        profileNameHeader.setText(fullName);
        ageText.setText(age);
        dniText.setText(dni);
        emailText.setText(email);
        phoneText.setText(phone);
        addressText.setText(fullAddress);
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        TextInputEditText editFirstName = dialogView.findViewById(R.id.editFirstName);
        TextInputEditText editLastName = dialogView.findViewById(R.id.editLastName);
        TextInputEditText editAge = dialogView.findViewById(R.id.editAge);
        TextInputEditText editDNI = dialogView.findViewById(R.id.editDNI);
        TextInputEditText editEmail = dialogView.findViewById(R.id.editEmail);
        TextInputEditText editPhone = dialogView.findViewById(R.id.editPhone);
        TextInputEditText editStreet = dialogView.findViewById(R.id.editStreet);
        TextInputEditText editDistrict = dialogView.findViewById(R.id.editDistrict);
        TextInputEditText editHouseNum = dialogView.findViewById(R.id.editHouseNum);
        
        Button btnSave = dialogView.findViewById(R.id.btnSaveProfile);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelEdit);

        // Filtro para permitir solo letras y espacios
        InputFilter letterFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.isLetter(source.charAt(i)) && !Character.isSpaceChar(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        };
        editFirstName.setFilters(new InputFilter[]{letterFilter});
        editLastName.setFilters(new InputFilter[]{letterFilter});

        // Pre-cargar datos actuales
        String currentFullName = sharedPreferences.getString("user_name", "Martin Rojas");
        String[] parts = currentFullName.split(" ", 2);
        if (parts.length > 0) editFirstName.setText(parts[0]);
        if (parts.length > 1) editLastName.setText(parts[1]);

        editAge.setText(sharedPreferences.getString("user_age", "20").replace(" años", ""));
        editDNI.setText(sharedPreferences.getString("user_dni", "12.345.678"));
        editEmail.setText(sharedPreferences.getString("user_email", "martin.rojas@email.com"));
        editPhone.setText(sharedPreferences.getString("user_phone", "123456789"));
        editStreet.setText(sharedPreferences.getString("user_street", "Av. Siempreviva"));
        editDistrict.setText(sharedPreferences.getString("user_district", "Springfield"));
        editHouseNum.setText(sharedPreferences.getString("user_houseNum", "742"));

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String firstName = editFirstName.getText().toString().trim();
            String lastName = editLastName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String ageVal = editAge.getText().toString().trim();

            // Validaciones fuertes
            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(getContext(), "Nombre y Apellido son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getContext(), "Formato de correo inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (phone.length() < 9) {
                Toast.makeText(getContext(), "El teléfono debe tener al menos 9 dígitos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Guardar en SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("user_name", firstName + " " + lastName);
            editor.putString("user_age", ageVal + " años");
            editor.putString("user_dni", editDNI.getText().toString().trim());
            editor.putString("user_email", email);
            editor.putString("user_phone", phone);
            editor.putString("user_street", editStreet.getText().toString().trim());
            editor.putString("user_district", editDistrict.getText().toString().trim());
            editor.putString("user_houseNum", editHouseNum.getText().toString().trim());
            editor.apply();

            loadProfileData();
            Toast.makeText(getContext(), "Datos guardados correctamente", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
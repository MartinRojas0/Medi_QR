package com.mediqr.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InicioFragment extends Fragment {

    private TextView lastConsultationText, nextAppointmentText;
    private ImageView qrImageView;
    private Button btnCopyQR;
    private SharedPreferences sharedPrefs;
    private String lastGeneratedQRContent = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inicio, container, false);

        sharedPrefs = requireActivity().getSharedPreferences("MediQR_Prefs", Context.MODE_PRIVATE);

        TextView greetingText = view.findViewById(R.id.greetingText);
        qrImageView = view.findViewById(R.id.qrImageView);
        btnCopyQR = view.findViewById(R.id.btnCopyQRInfo);
        Button sosButton = view.findViewById(R.id.sosButton);
        lastConsultationText = view.findViewById(R.id.lastConsultationText);
        nextAppointmentText = view.findViewById(R.id.nextAppointmentText);

        // Accesos rápidos
        MaterialCardView historyLayout = view.findViewById(R.id.historyLayout);
        MaterialCardView medicationLayout = view.findViewById(R.id.medicationLayout);
        MaterialCardView appointmentsLayout = view.findViewById(R.id.appointmentsLayout);
        MaterialCardView documentsLayout = view.findViewById(R.id.documentsLayout);

        // Cargar nombre (Solo el primer nombre)
        String fullName = sharedPrefs.getString("user_name", "Martin Rojas");
        String firstName = fullName.split(" ")[0];
        greetingText.setText("Hola " + firstName + "!!!");

        generateEmergencyQR(fullName);
        updateAppointmentsSummary();

        sosButton.setOnClickListener(v -> showSOSDialog());

        btnCopyQR.setOnClickListener(v -> {
            if (!lastGeneratedQRContent.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("MediQR Info", lastGeneratedQRContent);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Información copiada al portapapeles", Toast.LENGTH_SHORT).show();
            }
        });

        // Navegación a módulos
        historyLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), HistorialActivity.class);
            startActivity(intent);
        });

        medicationLayout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.navigation_health);
            }
        });

        appointmentsLayout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.navigation_appointments);
            }
        });

        documentsLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), DocumentosActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void generateEmergencyQR(String fullName) {
        String dni = sharedPrefs.getString("user_dni", "12.345.678");
        String age = sharedPrefs.getString("user_age", "20 años");
        
        SharedPreferences saludPrefs = requireActivity().getSharedPreferences("MediQR_Salud", Context.MODE_PRIVATE);
        String blood = saludPrefs.getString("user_blood_type", "A+");
        String allergies = saludPrefs.getString("user_allergies", "Ninguna");

        // Formato de texto claro y profesional para cualquier escáner
        lastGeneratedQRContent = "MEDIQR - FICHA DE EMERGENCIA\n" +
                "---------------------------\n" +
                "PACIENTE: " + fullName + "\n" +
                "DNI: " + dni + "\n" +
                "EDAD: " + age + "\n" +
                "SANGRE: " + blood + "\n" +
                "ALERGIAS: " + allergies + "\n" +
                "---------------------------\n" +
                "Escaneado desde MediQR App";

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            // Aumentamos resolución del Bitmap para mejor lectura
            Bitmap bitmap = barcodeEncoder.encodeBitmap(lastGeneratedQRContent, BarcodeFormat.QR_CODE, 800, 800);
            qrImageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAppointmentsSummary() {
        SharedPreferences citaPrefs = requireActivity().getSharedPreferences("MediQR_Citas", Context.MODE_PRIVATE);
        Set<String> set = citaPrefs.getStringSet("citas_set", new HashSet<>());
        
        List<Cita> upcoming = new ArrayList<>();
        List<Cita> attended = new ArrayList<>();
        
        for (String s : set) {
            Cita c = Cita.fromString(s);
            if (c != null) {
                if (c.isAtendida()) attended.add(c);
                else upcoming.add(c);
            }
        }
        
        if (!upcoming.isEmpty()) {
            Collections.sort(upcoming, Cita.DateComparator);
            Cita next = upcoming.get(0);
            nextAppointmentText.setText("Próxima cita: " + next.getFecha() + " - " + next.getEspecialidad());
        } else {
            nextAppointmentText.setText("No hay citas programadas");
        }
        
        if (!attended.isEmpty()) {
            Collections.sort(attended, Cita.DateComparator);
            Collections.reverse(attended);
            Cita last = attended.get(0);
            lastConsultationText.setText("Última: " + last.getFecha() + " - Dr. " + last.getDoctor());
        } else {
            lastConsultationText.setText("Sin consultas previas registradas");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String fullName = sharedPrefs.getString("user_name", "Martin Rojas");
        generateEmergencyQR(fullName);
        updateAppointmentsSummary();
    }

    private void showSOSDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sos, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button cancelButton = dialogView.findViewById(R.id.cancelSOSButton);
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
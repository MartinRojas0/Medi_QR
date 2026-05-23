package com.mediqr.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import com.mediqr.android.data.MedicalRepository;
import com.mediqr.android.models.Cita;
import com.mediqr.android.models.PatientProfile;

import java.util.Collections;
import java.util.List;

/**
 * InicioFragment: Dashboard central.
 * Preparado para conexión dinámica vía MedicalRepository.
 */
public class InicioFragment extends Fragment {

    private TextView lastConsultationText, nextAppointmentText;
    private ImageView qrImageView;
    private MedicalRepository repository;
    private String lastGeneratedQRContent = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inicio, container, false);

        repository = MedicalRepository.getInstance(requireContext());

        TextView greetingText = view.findViewById(R.id.greetingText);
        qrImageView = view.findViewById(R.id.qrImageView);
        Button btnCopyQR = view.findViewById(R.id.btnCopyQRInfo);
        Button sosButton = view.findViewById(R.id.sosButton);
        lastConsultationText = view.findViewById(R.id.lastConsultationText);
        nextAppointmentText = view.findViewById(R.id.nextAppointmentText);

        // Accesos rápidos
        MaterialCardView historyLayout = view.findViewById(R.id.historyLayout);
        MaterialCardView medicationLayout = view.findViewById(R.id.medicationLayout);
        MaterialCardView appointmentsLayout = view.findViewById(R.id.appointmentsLayout);
        MaterialCardView documentsLayout = view.findViewById(R.id.documentsLayout);

        // UI Initialization from Repository
        PatientProfile profile = repository.getProfile();
        greetingText.setText("Hola " + profile.getFirstName() + "!!!");

        generateEmergencyQR(profile);
        updateAppointmentsSummary();

        // Listeners
        sosButton.setOnClickListener(v -> showSOSDialog());

        btnCopyQR.setOnClickListener(v -> {
            if (!lastGeneratedQRContent.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("MediQR Info", lastGeneratedQRContent);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Información copiada al portapapeles", Toast.LENGTH_SHORT).show();
            }
        });

        historyLayout.setOnClickListener(v -> startActivity(new Intent(getContext(), HistorialActivity.class)));
        
        medicationLayout.setOnClickListener(v -> navigateToTab(R.id.navigation_health));
        appointmentsLayout.setOnClickListener(v -> navigateToTab(R.id.navigation_appointments));
        
        documentsLayout.setOnClickListener(v -> startActivity(new Intent(getContext(), DocumentosActivity.class)));

        return view;
    }

    private void navigateToTab(int tabId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToTab(tabId);
        }
    }

    private void generateEmergencyQR(PatientProfile profile) {
        String patientId = "martin" + profile.getDni().substring(profile.getDni().length() - 3);
        String publicUrl = "https://mediqr.app/paciente/" + patientId;

        lastGeneratedQRContent = "MEDIQR - EMERGENCIA\n" +
                "URL: " + publicUrl + "\n" +
                "---------------------------\n" +
                "PACIENTE: " + profile.getFullName() + "\n" +
                "DNI: " + profile.getDni() + "\n" +
                "SANGRE: " + profile.getBloodType() + "\n" +
                "ALERGIAS: " + profile.getAllergies() + "\n" +
                "---------------------------\n" +
                "Escaneado desde MediQR App";

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(lastGeneratedQRContent, BarcodeFormat.QR_CODE, 800, 800);
            qrImageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAppointmentsSummary() {
        List<Cita> allCitas = repository.getAppointments(false);
        
        Cita next = null;
        Cita last = null;
        
        for (Cita c : allCitas) {
            if (!c.isAtendida()) {
                if (next == null) next = c; // Since it's sorted by date
            } else {
                last = c; // Will end up with the most recent attended because of sorting and loop
            }
        }
        
        if (next != null) {
            nextAppointmentText.setText("Próxima cita: " + next.getFecha() + " - " + next.getEspecialidad());
        } else {
            nextAppointmentText.setText("No hay citas programadas");
        }
        
        if (last != null) {
            lastConsultationText.setText("Última: " + last.getFecha() + " - Dr. " + last.getDoctor());
        } else {
            lastConsultationText.setText("Sin consultas previas");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PatientProfile profile = repository.getProfile();
        generateEmergencyQR(profile);
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
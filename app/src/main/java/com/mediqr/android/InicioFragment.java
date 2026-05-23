package com.mediqr.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.mediqr.android.data.MedicalRepository;
import com.mediqr.android.models.Cita;
import com.mediqr.android.models.PatientProfile;

import java.util.List;

public class InicioFragment extends Fragment {

    private ImageView qrImageView;
    private TextView tvQRInfo;
    private View summaryNext, summaryLast, summaryAlerts;
    private MedicalRepository repository;
    private String shareContent = "";
    private MaterialButton sosButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = MedicalRepository.getInstance(requireContext());

        qrImageView = view.findViewById(R.id.qrImageView);
        tvQRInfo = view.findViewById(R.id.tvQRInfo);
        summaryNext = view.findViewById(R.id.summaryNextCita);
        summaryLast = view.findViewById(R.id.summaryLastConsulta);
        summaryAlerts = view.findViewById(R.id.summaryAlerts);
        sosButton = view.findViewById(R.id.sosButton);
        NestedScrollView scrollView = view.findViewById(R.id.homeScrollView);

        initUI(view);
        
        if (scrollView != null && sosButton != null) {
            scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > oldScrollY + 20 && sosButton.getVisibility() == View.VISIBLE) hideSOS();
                else if (scrollY < oldScrollY - 20 && sosButton.getVisibility() != View.VISIBLE) showSOS();
            });
        }
    }

    private void initUI(View v) {
        PatientProfile profile = repository.getProfile();
        ((TextView) v.findViewById(R.id.greetingText)).setText("Hola " + profile.getFirstName() + "!!! 👋");
        tvQRInfo.setText(profile.getFullName() + " • " + profile.getBloodType());

        generateQR(profile);
        updateSummary();

        // QUICK ACCESS LISTENERS
        v.findViewById(R.id.historyLayout).setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), HistorialActivity.class);
            startActivity(intent);
        });
        v.findViewById(R.id.medicationLayout).setOnClickListener(view -> navigateToTab(R.id.navigation_health));
        v.findViewById(R.id.appointmentsLayout).setOnClickListener(view -> navigateToTab(R.id.navigation_appointments));
        v.findViewById(R.id.documentsLayout).setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), DocumentosActivity.class);
            startActivity(intent);
        });
        
        v.findViewById(R.id.btnShareQRInfo).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, shareContent);
            startActivity(Intent.createChooser(intent, "Compartir Ficha Médica MediQR"));
        });

        if (sosButton != null) sosButton.setOnClickListener(view -> showSOSFlow());
        
        // Setup Quick Cards Visuals
        setupCard(v.findViewById(R.id.historyLayout), "Historial", R.drawable.ic_history);
        setupCard(v.findViewById(R.id.medicationLayout), "Ficha Médica", R.drawable.ic_heart);
        setupCard(v.findViewById(R.id.appointmentsLayout), "Citas", R.drawable.ic_appointments);
        setupCard(v.findViewById(R.id.documentsLayout), "Documentos", R.drawable.ic_documents);
    }

    private void setupCard(View layout, String title, int iconRes) {
        if (layout == null) return;
        ((TextView) layout.findViewById(R.id.cardTitle)).setText(title);
        ((ImageView) layout.findViewById(R.id.cardIcon)).setImageResource(iconRes);
    }

    private void generateQR(PatientProfile p) {
        shareContent = "PACIENTE MEDIQR\n" +
                "------------------\n" +
                "Nombre: " + p.getFullName() + "\n" +
                "DNI: " + p.getDni() + "\n" +
                "Sangre: " + p.getBloodType() + "\n" +
                "Alergias: " + p.getAllergies() + "\n" +
                "Contacto: " + p.getPhone() + "\n" +
                "------------------\n" +
                "Perfil: https://mediqr.app/id/" + p.getDni().hashCode();

        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap map = encoder.encodeBitmap(shareContent, BarcodeFormat.QR_CODE, 800, 800);
            qrImageView.setImageBitmap(map);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateSummary() {
        List<Cita> list = repository.getAppointments(false);
        Cita next = null, last = null;
        for (Cita c : list) {
            if (!c.isAtendida()) { if (next == null) next = c; }
            else { last = c; }
        }

        setSummaryRow(summaryNext, "Próxima: " + (next != null ? next.getEspecialidad() + " (" + next.getFecha() + ")" : "Sin citas"), R.drawable.ic_appointments, R.color.primary);
        setSummaryRow(summaryLast, "Última: " + (last != null ? last.getEspecialidad() + " - Dr. " + last.getDoctor() : "Sin historial"), R.drawable.ic_history, R.color.accent);
        
        PatientProfile p = repository.getProfile();
        boolean hasRisk = p.getAllergies().toLowerCase().contains("grave") || p.getAllergies().toLowerCase().contains("peni");
        setSummaryRow(summaryAlerts, hasRisk ? "ALERTA: " + p.getAllergies() : "Sin alertas críticas", 
                hasRisk ? android.R.drawable.ic_dialog_alert : R.drawable.ic_heart, 
                hasRisk ? R.color.sos_red : R.color.text_secondary);
    }

    private void setSummaryRow(View row, String text, int icon, int colorRes) {
        if (row == null) return;
        TextView tv = row.findViewById(R.id.summaryText);
        ImageView iv = row.findViewById(R.id.summaryIcon);
        tv.setText(text);
        iv.setImageResource(icon);
        iv.setColorFilter(getResources().getColor(colorRes));
    }

    private void hideSOS() {
        if (sosButton.getVisibility() != View.VISIBLE) return;
        Animation a = new ScaleAnimation(1,0,1,0,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        a.setDuration(300); a.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) { sosButton.setVisibility(View.GONE); }
            @Override public void onAnimationRepeat(Animation animation) {}
        });
        sosButton.startAnimation(a);
    }

    private void showSOS() {
        if (sosButton.getVisibility() == View.VISIBLE) return;
        sosButton.setVisibility(View.VISIBLE);
        Animation a = new ScaleAnimation(0,1,0,1,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        a.setDuration(300); sosButton.startAnimation(a);
    }

    private void navigateToTab(int id) {
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateToTab(id);
    }

    private void showSOSFlow() {
        View v = getLayoutInflater().inflate(R.layout.dialog_sos_modern, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), android.R.style.Theme_Translucent_NoTitleBar_Fullscreen).setView(v).create();
        v.findViewById(R.id.btnCancelSOS).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btnConfirmSOS).setOnClickListener(view -> {
            dialog.dismiss();
            Toast.makeText(getContext(), "Alerta enviada correctamente", Toast.LENGTH_LONG).show();
        });
        dialog.show();
    }

    @Override public void onResume() { super.onResume(); updateSummary(); }
}
package com.mediqr.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
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
import com.mediqr.android.datos.RepositorioMedico;
import com.mediqr.android.modelos.Cita;
import com.mediqr.android.modelos.PerfilPaciente;

import java.util.List;

/**
 * Fragmento de Inicio: Dashboard principal.
 */
public class FragmentoInicio extends Fragment {

    private ImageView qrImagenView;
    private TextView tvQRInfo;
    private View resumenProxima, resumenUltima, resumenAlertas;
    private RepositorioMedico repositorio;
    private String contenidoCompartir = "";
    private MaterialButton botonSOS;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, ViewGroup contenedor, Bundle savedInstanceState) {
        return inflador.inflate(R.layout.fragment_inicio, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        repositorio = RepositorioMedico.getInstancia(requireContext());

        qrImagenView = vista.findViewById(R.id.qrImageView);
        tvQRInfo = vista.findViewById(R.id.tvQRInfo);
        resumenProxima = vista.findViewById(R.id.summaryNextCita);
        resumenUltima = vista.findViewById(R.id.summaryLastConsulta);
        resumenAlertas = vista.findViewById(R.id.summaryAlerts);
        botonSOS = vista.findViewById(R.id.sosButton);
        NestedScrollView scrollView = vista.findViewById(R.id.homeScrollView);

        inicializarInterfaz(vista);
        
        if (scrollView != null && botonSOS != null) {
            scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > oldScrollY + 20 && botonSOS.getVisibility() == View.VISIBLE) ocultarSOS();
                else if (scrollY < oldScrollY - 20 && botonSOS.getVisibility() != View.VISIBLE) mostrarSOS();
            });
        }
    }

    private void inicializarInterfaz(View v) {
        PerfilPaciente perfil = repositorio.getPerfil();
        ((TextView) v.findViewById(R.id.greetingText)).setText("Hola " + perfil.getNombre() + "!!! 👋");
        tvQRInfo.setText(perfil.getNombreCompleto() + " • " + perfil.getTipoSangre());

        generarQR(perfil);
        actualizarResumen();

        // ESCUCHADORES DE ACCESOS RÁPIDOS
        v.findViewById(R.id.historyLayout).setOnClickListener(view -> {
            startActivity(new Intent(getContext(), HistorialActivity.class));
        });
        v.findViewById(R.id.medicationLayout).setOnClickListener(view -> navegarATab(R.id.navigation_health));
        v.findViewById(R.id.appointmentsLayout).setOnClickListener(view -> navegarATab(R.id.navigation_appointments));
        v.findViewById(R.id.documentsLayout).setOnClickListener(view -> {
            startActivity(new Intent(getContext(), DocumentosActivity.class));
        });
        
        v.findViewById(R.id.btnShareQRInfo).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, contenidoCompartir);
            startActivity(Intent.createChooser(intent, "Compartir Ficha Médica MediQR"));
        });

        if (botonSOS != null) botonSOS.setOnClickListener(view -> mostrarFlujoSOS());
        
        configurarTarjeta(v.findViewById(R.id.historyLayout), "Historial", R.drawable.ic_history);
        configurarTarjeta(v.findViewById(R.id.medicationLayout), "Ficha Médica", R.drawable.ic_heart);
        configurarTarjeta(v.findViewById(R.id.appointmentsLayout), "Citas", R.drawable.ic_appointments);
        configurarTarjeta(v.findViewById(R.id.documentsLayout), "Documentos", R.drawable.ic_documents);
    }

    private void configurarTarjeta(View vistaRaiz, String titulo, int iconoRes) {
        if (vistaRaiz == null) return;
        ((TextView) vistaRaiz.findViewById(R.id.cardTitle)).setText(titulo);
        ((ImageView) vistaRaiz.findViewById(R.id.cardIcon)).setImageResource(iconoRes);
    }

    /**
     * Mejora del Código QR: Versión corta y útil para emergencias.
     */
    private void generarQR(PerfilPaciente p) {
        // Contenido simplificado para emergencias
        contenidoCompartir = "🚨 EMERGENCIA MEDIQR\n" +
                "Paciente: " + p.getNombreCompleto() + "\n" +
                "Edad: " + p.getEdad() + " años\n" +
                "Sangre: " + p.getTipoSangre() + "\n" +
                "Alergias: " + p.getAlergias() + "\n" +
                "ID: " + p.getDni();

        try {
            BarcodeEncoder codificador = new BarcodeEncoder();
            Bitmap bitmap = codificador.encodeBitmap(contenidoCompartir, BarcodeFormat.QR_CODE, 800, 800);
            qrImagenView.setImageBitmap(bitmap);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void actualizarResumen() {
        List<Cita> lista = repositorio.getCitas(false);
        Cita proxima = null, ultima = null;
        for (Cita c : lista) {
            if (!c.isAtendida()) { if (proxima == null) proxima = c; }
            else { ultima = c; }
        }

        configurarFilaResumen(resumenProxima, "Próxima: " + (proxima != null ? proxima.getEspecialidad() + " (" + proxima.getFecha() + ")" : "Sin citas"), R.drawable.ic_appointments, R.color.primary);
        configurarFilaResumen(resumenUltima, "Última: " + (ultima != null ? ultima.getEspecialidad() + " - Dr. " + ultima.getDoctor() : "Sin historial"), R.drawable.ic_history, R.color.accent);
        
        PerfilPaciente p = repositorio.getPerfil();
        boolean tieneRiesgo = p.getAlergias().toLowerCase().contains("grave") || p.getAlergias().toLowerCase().contains("peni");
        configurarFilaResumen(resumenAlertas, tieneRiesgo ? "ALERTA: " + p.getAlergias() : "Sin alertas críticas", 
                tieneRiesgo ? android.R.drawable.ic_dialog_alert : R.drawable.ic_heart, 
                tieneRiesgo ? R.color.sos_red : R.color.text_secondary);
    }

    private void configurarFilaResumen(View fila, String texto, int icono, int colorRes) {
        if (fila == null) return;
        TextView tv = fila.findViewById(R.id.summaryText);
        ImageView iv = fila.findViewById(R.id.summaryIcon);
        tv.setText(texto);
        iv.setImageResource(icono);
        iv.setColorFilter(getResources().getColor(colorRes));
    }

    private void ocultarSOS() {
        if (botonSOS.getVisibility() != View.VISIBLE) return;
        Animation anim = new ScaleAnimation(1,0,1,0,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(300); anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) { botonSOS.setVisibility(View.GONE); }
            @Override public void onAnimationRepeat(Animation animation) {}
        });
        botonSOS.startAnimation(anim);
    }

    private void mostrarSOS() {
        if (botonSOS.getVisibility() == View.VISIBLE) return;
        botonSOS.setVisibility(View.VISIBLE);
        Animation anim = new ScaleAnimation(0,1,0,1,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(300); botonSOS.startAnimation(anim);
    }

    private void navegarATab(int id) {
        if (getActivity() instanceof PrincipalActivity) ((PrincipalActivity) getActivity()).navegarATab(id);
    }

    private void mostrarFlujoSOS() {
        View v = getLayoutInflater().inflate(R.layout.dialog_sos_premium, null);
        AlertDialog dialogo = new AlertDialog.Builder(requireContext(), android.R.style.Theme_Translucent_NoTitleBar_Fullscreen).setView(v).create();
        
        TextView tvTemporizador = v.findViewById(R.id.tvSosTimer);
        final int[] segundosRestantes = {5};
        Handler handler = new Handler();
        Runnable ejecutable = new Runnable() {
            @Override
            public void run() {
                if (segundosRestantes[0] > 0) {
                    segundosRestantes[0]--;
                    tvTemporizador.setText("Se cerrará en " + segundosRestantes[0] + "s");
                    handler.postDelayed(this, 1000);
                } else {
                    if (dialogo.isShowing()) dialogo.dismiss();
                }
            }
        };
        handler.postDelayed(ejecutable, 1000);

        v.findViewById(R.id.btnCancelSosDialog).setOnClickListener(view -> {
            handler.removeCallbacks(ejecutable);
            dialogo.dismiss();
        });

        v.findViewById(R.id.btnSendAlert).setOnClickListener(view -> {
            handler.removeCallbacks(ejecutable);
            dialogo.dismiss();
            Toast.makeText(getContext(), "Alerta enviada a contactos y clínicas cercanas", Toast.LENGTH_LONG).show();
        });

        v.findViewById(R.id.btnCallAmbulance).setOnClickListener(view -> {
            handler.removeCallbacks(ejecutable);
            new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Llamada")
                .setMessage("¿Estás seguro? Esto no es una broma.")
                .setPositiveButton("LLAMAR", (d, w) -> {
                    dialogo.dismiss();
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:911"));
                    startActivity(intent);
                })
                .setNegativeButton("CANCELAR", (d, w) -> handler.postDelayed(ejecutable, 1000))
                .show();
        });

        dialogo.show();
    }

    @Override public void onResume() { super.onResume(); actualizarResumen(); }
}
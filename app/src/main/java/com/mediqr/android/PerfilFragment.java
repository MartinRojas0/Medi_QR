package com.mediqr.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.InputFilter;
import android.util.Base64;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.mediqr.android.datos.RepositorioMedico;
import com.mediqr.android.modelos.Cita;
import com.mediqr.android.modelos.Documento;
import com.mediqr.android.modelos.PerfilPaciente;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

/**
 * Fragmento de Perfil: Información del usuario y exportación avanzada a PDF mediante HTML.
 */
public class PerfilFragment extends Fragment {

    private TextView tvNombre, tvEdad, tvDni, tvCorreo, tvTelefono, tvDireccion, tvCabeceraNombre;
    private RepositorioMedico repositorio;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, ViewGroup contenedor, Bundle savedInstanceState) {
        return inflador.inflate(R.layout.fragment_perfil, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);
        repositorio = RepositorioMedico.getInstancia(requireContext());

        tvCabeceraNombre = vista.findViewById(R.id.profileNameHeader);
        tvNombre = vista.findViewById(R.id.nameText);
        tvEdad = vista.findViewById(R.id.ageText);
        tvDni = vista.findViewById(R.id.dniText);
        tvCorreo = vista.findViewById(R.id.emailText);
        tvTelefono = vista.findViewById(R.id.phoneText);
        tvDireccion = vista.findViewById(R.id.addressText);
        
        Button btnEditarPerfil = vista.findViewById(R.id.editProfileButton);
        Button btnExportarPDF = vista.findViewById(R.id.btnExportPDF);

        cargarDatosPerfil();

        btnEditarPerfil.setOnClickListener(v -> mostrarDialogoEdicionPerfil());
        btnExportarPDF.setOnClickListener(v -> exportarExpedienteHTML());
    }

    private void cargarDatosPerfil() {
        PerfilPaciente p = repositorio.getPerfil();
        tvNombre.setText(p.getNombreCompleto());
        tvCabeceraNombre.setText(p.getNombreCompleto());
        tvEdad.setText(p.getEdad() + " años");
        tvDni.setText(p.getDni());
        tvCorreo.setText(p.getCorreo());
        tvTelefono.setText(p.getTelefono());
        tvDireccion.setText(p.getDireccionCompleta());
    }

    private void mostrarDialogoEdicionPerfil() {
        AlertDialog.Builder constructor = new AlertDialog.Builder(requireContext());
        View v = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        constructor.setView(v);

        TextInputEditText eNombre = v.findViewById(R.id.editFirstName);
        TextInputEditText eApellido = v.findViewById(R.id.editLastName);
        TextInputEditText eEdad = v.findViewById(R.id.editAge);
        TextInputEditText eDNI = v.findViewById(R.id.editDNI);
        TextInputEditText eCorreo = v.findViewById(R.id.editEmail);
        TextInputEditText eTelf = v.findViewById(R.id.editPhone);
        TextInputEditText eCalle = v.findViewById(R.id.editStreet);
        TextInputEditText eDistrito = v.findViewById(R.id.editDistrict);
        TextInputEditText eCasa = v.findViewById(R.id.editHouseNum);
        
        PerfilPaciente actual = repositorio.getPerfil();
        eNombre.setText(actual.getNombre());
        eApellido.setText(actual.getApellido());
        eEdad.setText(String.valueOf(actual.getEdad()));
        eDNI.setText(actual.getDni());
        eCorreo.setText(actual.getCorreo());
        eTelf.setText(actual.getTelefono());
        eCalle.setText(actual.getCalle());
        eDistrito.setText(actual.getDistrito());
        eCasa.setText(actual.getNumeroCasa());

        InputFilter filtroLetras = (src, s, e, dst, ds, de) -> {
            for (int i = s; i < e; i++) if (!Character.isLetter(src.charAt(i)) && !Character.isSpaceChar(src.charAt(i))) return "";
            return null;
        };
        eNombre.setFilters(new InputFilter[]{filtroLetras});
        eApellido.setFilters(new InputFilter[]{filtroLetras});

        AlertDialog dialogo = constructor.create();

        v.findViewById(R.id.btnSaveProfile).setOnClickListener(view -> {
            String nom = eNombre.getText().toString().trim();
            String ape = eApellido.getText().toString().trim();
            String corr = eCorreo.getText().toString().trim();
            
            if (nom.isEmpty() || ape.isEmpty()) { Toast.makeText(getContext(), "Campos obligatorios vacíos", Toast.LENGTH_SHORT).show(); return; }
            if (!Patterns.EMAIL_ADDRESS.matcher(corr).matches()) { Toast.makeText(getContext(), "Email inválido", Toast.LENGTH_SHORT).show(); return; }

            PerfilPaciente nuevo = repositorio.getPerfil();
            nuevo.setNombre(nom);
            nuevo.setApellido(ape);
            try { nuevo.setEdad(Integer.parseInt(eEdad.getText().toString())); } catch (Exception ex) { nuevo.setEdad(20); }
            nuevo.setDni(eDNI.getText().toString().trim());
            nuevo.setCorreo(corr);
            nuevo.setTelefono(eTelf.getText().toString().trim());
            nuevo.setCalle(eCalle.getText().toString().trim());
            nuevo.setDistrito(eDistrito.getText().toString().trim());
            nuevo.setNumeroCasa(eCasa.getText().toString().trim());

            repositorio.guardarPerfil(nuevo);
            cargarDatosPerfil();
            Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
            dialogo.dismiss();
        });

        v.findViewById(R.id.btnCancelEdit).setOnClickListener(view -> dialogo.dismiss());
        dialogo.show();
    }

    /**
     * Nueva Estrategia: Generar HTML dinámico y usar el motor de impresión de Android para PDF.
     * Esto permite previsualización, imágenes estables y diseño profesional.
     */
    private void exportarExpedienteHTML() {
        PerfilPaciente p = repositorio.getPerfil();
        List<Cita> citas = repositorio.getCitas(false);
        List<Documento> docs = repositorio.getDocumentos();

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
            .append("body { font-family: sans-serif; color: #333; padding: 20px; }")
            .append(".header { text-align: center; border-bottom: 2px solid #1E88E5; padding-bottom: 10px; margin-bottom: 20px; }")
            .append("h1 { color: #1E88E5; margin: 0; }")
            .append("h2 { color: #EF4444; border-left: 4px solid #EF4444; padding-left: 10px; margin-top: 30px; font-size: 18px; }")
            .append(".section { margin-bottom: 20px; }")
            .append(".field { font-weight: bold; color: #555; }")
            .append(".item { background: #F8FAFC; padding: 15px; border-radius: 8px; margin-bottom: 10px; border: 1px solid #E2E8F0; }")
            .append(".img-container { text-align: center; margin-top: 10px; }")
            .append("img { max-width: 100%; border-radius: 8px; height: auto; margin-top: 10px; border: 1px solid #DDD; }")
            .append("</style></head><body>");

        // Cabecera
        html.append("<div class='header'><h1>EXPEDIENTE MÉDICO MEDIQR</h1>")
            .append("<p>Generado oficialmente el ").append(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date())).append("</p></div>");

        // Datos Personales
        html.append("<h2>1. INFORMACIÓN PERSONAL</h2><div class='section'>")
            .append("<p><span class='field'>Nombre:</span> ").append(p.getNombreCompleto()).append("</p>")
            .append("<p><span class='field'>DNI:</span> ").append(p.getDni()).append(" | <span class='field'>Edad:</span> ").append(p.getEdad()).append(" años</p>")
            .append("<p><span class='field'>Correo:</span> ").append(p.getCorreo()).append(" | <span class='field'>Teléfono:</span> ").append(p.getTelefono()).append("</p>")
            .append("<p><span class='field'>Dirección:</span> ").append(p.getDireccionCompleta()).append("</p></div>");

        // Salud
        html.append("<h2>2. INFORMACIÓN CLÍNICA CRÍTICA</h2><div class='section'>")
            .append("<p><span class='field'>Tipo de Sangre:</span> ").append(p.getTipoSangre()).append("</p>")
            .append("<p><span class='field'>Alergias:</span> ").append(p.getAlergias()).append("</p>")
            .append("<p><span class='field'>Condiciones:</span> ").append(p.getCondicionesCronicas()).append("</p>")
            .append("<p><span class='field'>Medicamentos:</span> ").append(p.getMedicamentosActuales()).append("</p></div>");

        // Antecedentes
        html.append("<h2>3. ANTECEDENTES Y VACUNAS</h2><div class='section'>")
            .append("<p><span class='field'>Vacunas:</span> ").append(p.getVacunas()).append("</p>")
            .append("<p><span class='field'>Cirugías:</span> ").append(p.getCirugias()).append("</p></div>");

        // Historial
        html.append("<h2>4. HISTORIAL DE CONSULTAS</h2><div class='section'>");
        for (Cita c : citas) {
            if (c.isAtendida()) {
                html.append("<div class='item'>")
                    .append("<p><span class='field'>[").append(c.getFecha()).append("] ").append(c.getEspecialidad()).append("</span></p>")
                    .append("<p>Médico: Dr. ").append(c.getDoctor()).append(" | Lugar: ").append(c.getLugar()).append("</p>")
                    .append("<p><i>Diagnóstico: ").append(c.getDiagnostico()).append("</i></p></div>");
            }
        }
        html.append("</div>");

        // Documentos con Imágenes Reales (Base64 para incrustar en PDF)
        html.append("<h2>5. ARCHIVOS Y EVIDENCIAS</h2><div class='section'>");
        for (Documento d : docs) {
            html.append("<div class='item'><p><span class='field'>Archivo:</span> ").append(d.getNombre()).append(" (").append(d.getTipo()).append(")</p>")
                .append("<p>Fecha: ").append(d.getFecha()).append("</p>");
            
            // Intentar cargar e incrustar la imagen
            String base64 = getBase64Image(d.getRuta());
            if (base64 != null) {
                html.append("<div class='img-container'><img src='data:image/jpeg;base64,").append(base64).append("' /></div>");
            } else {
                html.append("<p style='color: #888;'>[Evidencia no disponible como imagen]</p>");
            }
            html.append("</div>");
        }
        html.append("</div></body></html>");

        // Usamos WebView para procesar el HTML y lanzamos el diálogo de impresión de Android
        WebView webView = new WebView(requireContext());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                crearTrabajoImpresion(view);
            }
        });
        webView.loadDataWithBaseURL(null, html.toString(), "text/HTML", "UTF-8", null);
    }

    private String getBase64Image(String ruta) {
        try {
            File file = new File(ruta);
            if (!file.exists()) return null;
            Bitmap bitmap = BitmapFactory.decodeFile(ruta);
            if (bitmap == null) return null;
            
            // Escalar para reducir peso del PDF
            int maxDim = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxDim || height > maxDim) {
                float factor = maxDim / (float) Math.max(width, height);
                bitmap = Bitmap.createScaledBitmap(bitmap, (int)(width*factor), (int)(height*factor), true);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            byte[] bytes = outputStream.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) { return null; }
    }

    private void crearTrabajoImpresion(WebView webView) {
        PrintManager printManager = (PrintManager) requireContext().getSystemService(Context.PRINT_SERVICE);
        String jobName = "Expediente_MediQR_" + System.currentTimeMillis();
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
        
        if (printManager != null) {
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
        }
    }
}
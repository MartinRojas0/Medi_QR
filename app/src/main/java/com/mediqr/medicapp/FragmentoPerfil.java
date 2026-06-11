package com.mediqr.medicapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.InputFilter;
import android.text.Spanned;
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
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.mediqr.medicapp.datos.RepositorioMedico;
import com.mediqr.medicapp.modelos.Cita;
import com.mediqr.medicapp.modelos.PerfilPaciente;
import com.mediqr.medicapp.utils.FirebaseAuthHelper;

import java.util.List;

public class FragmentoPerfil extends Fragment {

    private TextView tvNombre, tvEdad, tvDni, tvCorreo, tvTelefono, tvDireccion, tvCabeceraNombre;
    private RepositorioMedico repositorio;
    private FirebaseAuthHelper authHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, ViewGroup contenedor, Bundle savedInstanceState) {
        return inflador.inflate(R.layout.fragment_perfil, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);
        repositorio = RepositorioMedico.getInstancia(requireContext());
        authHelper = new FirebaseAuthHelper(requireContext());

        tvCabeceraNombre = vista.findViewById(R.id.profileNameHeader);
        tvNombre = vista.findViewById(R.id.nameText);
        tvEdad = vista.findViewById(R.id.ageText);
        tvDni = vista.findViewById(R.id.dniText);
        tvCorreo = vista.findViewById(R.id.emailText);
        tvTelefono = vista.findViewById(R.id.phoneText);
        tvDireccion = vista.findViewById(R.id.addressText);

        Button btnEditarPerfil = vista.findViewById(R.id.editProfileButton);
        Button btnExportarPDF = vista.findViewById(R.id.btnExportPDF);
        Button btnCerrarSesion = vista.findViewById(R.id.btnCerrarSesion);

        cargarDatosPerfil();

        btnEditarPerfil.setOnClickListener(v -> mostrarDialogoEdicionPerfil());
        btnExportarPDF.setOnClickListener(v -> exportarExpedienteHTML());

        if (btnCerrarSesion != null) {
            btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
        }
    }

    private void cargarDatosPerfil() {
        PerfilPaciente p = repositorio.getPerfil();

        // Mostrar nombre, si está vacío mostrar "Completa tu perfil"
        String nombreCompleto = p.getNombreCompleto();
        if (nombreCompleto.isEmpty()) {
            tvNombre.setText("Completa tu perfil");
            tvCabeceraNombre.setText("Mi Perfil");
        } else {
            tvNombre.setText(nombreCompleto);
            tvCabeceraNombre.setText(nombreCompleto);
        }

        // Edad
        if (p.getEdad() > 0) {
            tvEdad.setText(p.getEdad() + " años");
        } else {
            tvEdad.setText("No especificado");
        }

        // DNI
        tvDni.setText(p.getDni().isEmpty() ? "No especificado" : p.getDni());

        // Correo
        tvCorreo.setText(p.getCorreo().isEmpty() ? "No especificado" : p.getCorreo());

        // Teléfono
        tvTelefono.setText(p.getTelefono().isEmpty() ? "No especificado" : p.getTelefono());

        // Dirección
        tvDireccion.setText(p.getDireccionCompleta().isEmpty() ? "No especificado" : p.getDireccionCompleta());
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

        // Deshabilitar edición del correo (no se puede modificar)
        eCorreo.setEnabled(false);
        eCorreo.setFocusable(false);
        eCorreo.setClickable(false);

        PerfilPaciente actual = repositorio.getPerfil();
        eNombre.setText(actual.getNombre());
        eApellido.setText(actual.getApellido());
        eEdad.setText(actual.getEdad() > 0 ? String.valueOf(actual.getEdad()) : "");
        eDNI.setText(actual.getDni());
        eCorreo.setText(actual.getCorreo());
        eTelf.setText(actual.getTelefono());
        eCalle.setText(actual.getCalle());
        eDistrito.setText(actual.getDistrito());
        eCasa.setText(actual.getNumeroCasa());

        // Filtro: Solo letras y espacios para nombre y apellido
        InputFilter filtroSoloLetras = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    if (!Character.isLetter(c) && !Character.isSpaceChar(c)) {
                        return "";
                    }
                }
                return null;
            }
        };

        // Filtro: Solo números para DNI, edad y teléfono
        InputFilter filtroSoloNumeros = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isDigit(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };

        eNombre.setFilters(new InputFilter[]{filtroSoloLetras});
        eApellido.setFilters(new InputFilter[]{filtroSoloLetras});
        eEdad.setFilters(new InputFilter[]{filtroSoloNumeros});
        eDNI.setFilters(new InputFilter[]{filtroSoloNumeros});
        eTelf.setFilters(new InputFilter[]{filtroSoloNumeros});

        AlertDialog dialogo = constructor.create();

        v.findViewById(R.id.btnSaveProfile).setOnClickListener(view -> {
            String nom = eNombre.getText().toString().trim();
            String ape = eApellido.getText().toString().trim();
            String edadStr = eEdad.getText().toString().trim();
            String dni = eDNI.getText().toString().trim();
            String telf = eTelf.getText().toString().trim();

            // Validar nombre
            if (nom.isEmpty()) {
                eNombre.setError("El nombre es requerido");
                eNombre.requestFocus();
                return;
            }

            // Validar DNI (8 dígitos exactos)
            if (!dni.isEmpty()) {
                if (dni.length() != 8) {
                    eDNI.setError("El DNI debe tener 8 dígitos");
                    eDNI.requestFocus();
                    return;
                }
            }

            // Validar teléfono (9 dígitos exactos)
            if (!telf.isEmpty()) {
                if (telf.length() != 9) {
                    eTelf.setError("El teléfono debe tener 9 dígitos");
                    eTelf.requestFocus();
                    return;
                }
            }

            // Validar edad (entre 0 y 120)
            int edad = 0;
            if (!edadStr.isEmpty()) {
                try {
                    edad = Integer.parseInt(edadStr);
                    if (edad < 0 || edad > 120) {
                        eEdad.setError("Edad inválida (0-120)");
                        eEdad.requestFocus();
                        return;
                    }
                } catch (NumberFormatException ex) {
                    eEdad.setError("Edad inválida");
                    eEdad.requestFocus();
                    return;
                }
            }

            PerfilPaciente nuevo = repositorio.getPerfil();
            nuevo.setNombre(nom);
            nuevo.setApellido(ape);
            nuevo.setEdad(edad);
            nuevo.setDni(dni);
            nuevo.setTelefono(telf);
            nuevo.setCalle(eCalle.getText().toString().trim());
            nuevo.setDistrito(eDistrito.getText().toString().trim());
            nuevo.setNumeroCasa(eCasa.getText().toString().trim());
            // El correo NO se modifica

            repositorio.guardarPerfil(nuevo);
            repositorio.guardarPerfilEnFirestore(nuevo, new RepositorioMedico.OnPerfilGuardadoListener() {
                @Override
                public void onSuccess() {
                    cargarDatosPerfil();
                    Toast.makeText(getContext(), "Perfil actualizado y sincronizado", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    cargarDatosPerfil();
                    Toast.makeText(getContext(), "Perfil actualizado localmente", Toast.LENGTH_SHORT).show();
                }
            });
            dialogo.dismiss();
        });

        v.findViewById(R.id.btnCancelEdit).setOnClickListener(view -> dialogo.dismiss());
        dialogo.show();
    }

    private void exportarExpedienteHTML() {
        PerfilPaciente p = repositorio.getPerfil();
        List<Cita> citas = repositorio.getCitas(false);

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
                .append("body { font-family: sans-serif; color: #333; padding: 20px; }")
                .append(".header { text-align: center; border-bottom: 2px solid #1E88E5; padding-bottom: 10px; margin-bottom: 20px; }")
                .append("h1 { color: #1E88E5; margin: 0; }")
                .append("h2 { color: #EF4444; border-left: 4px solid #EF4444; padding-left: 10px; margin-top: 30px; font-size: 18px; }")
                .append(".section { margin-bottom: 20px; }")
                .append(".field { font-weight: bold; color: #555; }")
                .append(".item { background: #F8FAFC; padding: 15px; border-radius: 8px; margin-bottom: 10px; border: 1px solid #E2E8F0; }")
                .append("</style></head><body>");

        html.append("<div class='header'><h1>EXPEDIENTE MÉDICO MEDIQR</h1>")
                .append("<p>Generado oficialmente el ").append(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date())).append("</p></div>");

        html.append("<h2>1. INFORMACIÓN PERSONAL</h2><div class='section'>")
                .append("<p><span class='field'>Nombre:</span> ").append(p.getNombreCompleto()).append("</p>")
                .append("<p><span class='field'>DNI:</span> ").append(p.getDni()).append(" | <span class='field'>Edad:</span> ").append(p.getEdad()).append(" años</p>")
                .append("<p><span class='field'>Correo:</span> ").append(p.getCorreo()).append(" | <span class='field'>Teléfono:</span> ").append(p.getTelefono()).append("</p>")
                .append("<p><span class='field'>Dirección:</span> ").append(p.getDireccionCompleta()).append("</p></div>");

        html.append("<h2>2. INFORMACIÓN CLÍNICA CRÍTICA</h2><div class='section'>")
                .append("<p><span class='field'>Tipo de Sangre:</span> ").append(p.getTipoSangre()).append("</p>")
                .append("<p><span class='field'>Alergias:</span> ").append(p.getAlergias()).append("</p>")
                .append("<p><span class='field'>Condiciones:</span> ").append(p.getCondicionesCronicas()).append("</p>")
                .append("<p><span class='field'>Medicamentos:</span> ").append(p.getMedicamentosActuales()).append("</p></div>");

        html.append("<h2>3. HISTORIAL DE CONSULTAS</h2><div class='section'>");
        for (Cita c : citas) {
            if (c.isAtendida()) {
                html.append("<div class='item'>")
                        .append("<p><span class='field'>[").append(c.getFecha()).append("] ").append(c.getEspecialidad()).append("</span></p>")
                        .append("<p>Médico: Dr. ").append(c.getDoctor()).append(" | Lugar: ").append(c.getLugar()).append("</p>")
                        .append("<p><i>Diagnóstico: ").append(c.getDiagnostico()).append("</i></p></div>");
            }
        }
        html.append("</div></body></html>");

        WebView webView = new WebView(requireContext());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                crearTrabajoImpresion(view);
            }
        });
        webView.loadDataWithBaseURL(null, html.toString(), "text/HTML", "UTF-8", null);
    }

    private void crearTrabajoImpresion(WebView webView) {
        PrintManager printManager = (PrintManager) requireContext().getSystemService(Context.PRINT_SERVICE);
        String jobName = "Expediente_MediQR_" + System.currentTimeMillis();
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);

        if (printManager != null) {
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
        }
    }

    private void cerrarSesion() {
        new AlertDialog.Builder(getContext())
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    authHelper.logout();
                    Intent intent = new Intent(getContext(), InicioSesionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarDatosPerfil();
    }
}
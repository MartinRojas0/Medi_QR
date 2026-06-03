package com.mediqr.android;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mediqr.android.datos.RepositorioMedico;
import com.mediqr.android.modelos.Cita;
import com.mediqr.android.modelos.Documento;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Actividad del Historial Médico: Línea de tiempo interactiva potente y moderna.
 * Incluye buscador integrado y sincronización con documentos vinculados.
 */
public class HistorialActivity extends AppCompatActivity {

    private RecyclerView rvHistorial;
    private TextView tvVacio;
    private RepositorioMedico repositorio;
    private ActivityResultLauncher<Intent> lanzadorSelectorArchivos;
    private String idCitaVinculandoActual = "";
    private String especCitaVinculandoActual = "";
    
    private List<Cita> todasLasCitas = new ArrayList<>();
    private List<Cita> citasFiltradas = new ArrayList<>();
    private List<Documento> todosLosDocs = new ArrayList<>();
    private AdaptadorHistorial adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        repositorio = RepositorioMedico.getInstancia(this);

        Toolbar barraHerramientas = findViewById(R.id.toolbarHistorial);
        setSupportActionBar(barraHerramientas);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        barraHerramientas.setNavigationOnClickListener(v -> finish());

        rvHistorial = findViewById(R.id.rvHistorial);
        tvVacio = findViewById(R.id.tvEmptyHistorial);
        SearchView searchView = findViewById(R.id.searchHistorial);

        lanzadorSelectorArchivos = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                resultado -> {
                    if (resultado.getResultCode() == Activity.RESULT_OK && resultado.getData() != null) {
                        manejarArchivoSeleccionado(resultado.getData().getData());
                    }
                }
        );

        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) { filtrarHistorial(query); return true; }
                @Override
                public boolean onQueryTextChange(String newText) { filtrarHistorial(newText); return true; }
            });
        }

        cargarHistorial();
    }

    private void cargarHistorial() {
        try {
            todasLasCitas = repositorio.getCitas(false);
            Collections.sort(todasLasCitas, Cita.ComparadorFecha);
            Collections.reverse(todasLasCitas);
            todosLosDocs = repositorio.getDocumentos();

            citasFiltradas.clear();
            citasFiltradas.addAll(todasLasCitas);

            adaptador = new AdaptadorHistorial(citasFiltradas, todosLosDocs);
            rvHistorial.setLayoutManager(new LinearLayoutManager(this));
            rvHistorial.setAdapter(adaptador);

            actualizarEstadoVacio();
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar historial", Toast.LENGTH_SHORT).show();
        }
    }

    private void filtrarHistorial(String texto) {
        citasFiltradas.clear();
        if (texto.isEmpty()) {
            citasFiltradas.addAll(todasLasCitas);
        } else {
            String lower = texto.toLowerCase();
            for (Cita c : todasLasCitas) {
                if (c.getEspecialidad().toLowerCase().contains(lower) || 
                    c.getDoctor().toLowerCase().contains(lower) ||
                    c.getDiagnostico().toLowerCase().contains(lower)) {
                    citasFiltradas.add(c);
                }
            }
        }
        if (adaptador != null) adaptador.notifyDataSetChanged();
        actualizarEstadoVacio();
    }

    private void actualizarEstadoVacio() {
        tvVacio.setVisibility(citasFiltradas.isEmpty() ? View.VISIBLE : View.GONE);
        rvHistorial.setVisibility(citasFiltradas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void manejarArchivoSeleccionado(Uri uri) {
        try {
            String nombreOrig = obtenerNombreArchivo(uri);
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            File archivoInterno = new File(getFilesDir(), "hist_" + System.currentTimeMillis() + "_" + (nombreOrig != null ? nombreOrig : "adjunto"));
            
            InputStream entrada = getContentResolver().openInputStream(uri);
            FileOutputStream salida = new FileOutputStream(archivoInterno);
            byte[] bufer = new byte[1024];
            int leido;
            if (entrada != null) {
                while ((leido = entrada.read(bufer)) != -1) salida.write(bufer, 0, leido);
                entrada.close();
            }
            salida.close();

            Documento nuevoDoc = new Documento(UUID.randomUUID().toString(), nombreOrig != null ? nombreOrig : "Adjunto", fechaActual, archivoInterno.getAbsolutePath(), especCitaVinculandoActual, "Consulta", "MediQR", "Evidencia");
            nuevoDoc.setIdCitaVinculada(idCitaVinculandoActual);
            
            repositorio.agregarDocumento(nuevoDoc);
            cargarHistorial();
            Toast.makeText(this, "Documento vinculado", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar archivo", Toast.LENGTH_SHORT).show();
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String r = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (i != -1) r = c.getString(i);
                }
            }
        }
        if (r == null && uri != null) {
            r = uri.getPath();
            if (r != null) {
                int corte = r.lastIndexOf('/');
                if (corte != -1) r = r.substring(corte + 1);
            }
        }
        return r;
    }

    private void mostrarFormularioRapido(Cita cita) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_subir_archivo_rapido, null);
        b.setView(v);

        Spinner sEspec = v.findViewById(R.id.spinnerQuickEspec);
        String[] especs = {"Cardiología", "Odontología", "Dermatología", "Medicina General", "Oftalmología", "Ginecología", "Pediatría", "Traumatología", "Otros"};
        sEspec.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, especs));
        
        for(int i=0; i<especs.length; i++) if(especs[i].equalsIgnoreCase(cita.getEspecialidad())) sEspec.setSelection(i);

        AlertDialog d = b.create();
        v.findViewById(R.id.btnQuickSeleccionar).setOnClickListener(view -> {
            idCitaVinculandoActual = cita.getId();
            especCitaVinculandoActual = sEspec.getSelectedItem().toString();
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            lanzadorSelectorArchivos.launch(i);
            d.dismiss();
        });
        v.findViewById(R.id.btnQuickCancelar).setOnClickListener(view -> d.dismiss());
        d.show();
    }

    private void mostrarDialogoEdicionPotente(Cita cita) {
        AlertDialog.Builder constructor = new AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_editar_historial, null);
        constructor.setView(v);

        Spinner sEspec = v.findViewById(R.id.spinnerHistEspecialidad);
        TextInputEditText eDoc = v.findViewById(R.id.editHistDoctor);
        TextInputEditText eFecha = v.findViewById(R.id.editHistFecha);
        TextInputEditText eHora = v.findViewById(R.id.editHistHora);
        TextInputEditText eLugar = v.findViewById(R.id.editHistLugar);
        TextInputEditText eDiag = v.findViewById(R.id.editHistDiagnostico);
        TextInputEditText eIndic = v.findViewById(R.id.editHistIndicaciones);

        String[] especialidades = {"Cardiología", "Odontología", "Dermatología", "Medicina General", "Oftalmología", "Ginecología", "Pediatría", "Traumatología", "Otros"};
        sEspec.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, especialidades));
        for (int i = 0; i < especialidades.length; i++) if (especialidades[i].equalsIgnoreCase(cita.getEspecialidad())) sEspec.setSelection(i);

        eDoc.setText(cita.getDoctor());
        eFecha.setText(cita.getFecha());
        eHora.setText(cita.getHora());
        eLugar.setText(cita.getLugar());
        eDiag.setText(cita.getDiagnostico());
        eIndic.setText(cita.getTratamiento());

        eFecha.setOnClickListener(view -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view1, anio, mes, dia) -> eFecha.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", anio, mes + 1, dia)), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        eHora.setOnClickListener(view -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view1, h, m) -> eHora.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });

        AlertDialog dialogo = constructor.create();
        v.findViewById(R.id.btnActualizarHistorial).setOnClickListener(view -> {
            cita.setEspecialidad(sEspec.getSelectedItem().toString());
            cita.setDoctor(eDoc.getText().toString());
            cita.setFecha(eFecha.getText().toString());
            cita.setHora(eHora.getText().toString());
            cita.setLugar(eLugar.getText().toString());
            cita.setDiagnostico(eDiag.getText().toString());
            cita.setTratamiento(eIndic.getText().toString());
            repositorio.guardarCita(cita);
            cargarHistorial();
            dialogo.dismiss();
            Toast.makeText(this, "Registro actualizado", Toast.LENGTH_SHORT).show();
        });

        v.findViewById(R.id.btnEliminarHistorial).setOnClickListener(view -> {
            new AlertDialog.Builder(this).setTitle("¿Eliminar registro?").setMessage("Se perderá del historial permanentemente.").setPositiveButton("Eliminar", (dialog, cual) -> {
                repositorio.eliminarCita(cita.getId());
                cargarHistorial();
                dialogo.dismiss();
            }).setNegativeButton("Cancelar", null).show();
        });

        v.findViewById(R.id.btnCancelarEdicionHist).setOnClickListener(view -> dialogo.dismiss());
        dialogo.show();
    }

    private class AdaptadorHistorial extends RecyclerView.Adapter<AdaptadorHistorial.ViewHolder> {
        private final List<Cita> listaCitas;
        private final List<Documento> listaDocs;

        public AdaptadorHistorial(List<Cita> citas, List<Documento> docs) {
            this.listaCitas = citas; this.listaDocs = docs;
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
            return new ViewHolder(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_historial, padre, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            Cita cita = listaCitas.get(pos);
            h.tvEspecialidad.setText(cita.getEspecialidad());
            h.tvDoctor.setText("Dr. " + cita.getDoctor());
            h.tvFechaHora.setText(cita.getFecha() + " " + (cita.getHora() != null ? cita.getHora() : ""));
            h.tvLugar.setText(cita.getLugar());
            h.tvDiagnostico.setText(cita.getDiagnostico());

            h.vLineaArriba.setVisibility(pos == 0 ? View.INVISIBLE : View.VISIBLE);
            h.vLineaAbajo.setVisibility(pos == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);

            if (cita.isAtendida()) h.tvStatus.setVisibility(View.GONE);
            else h.tvStatus.setVisibility(View.VISIBLE);

            h.contenedorDocs.removeAllViews();
            for (Documento d : listaDocs) {
                if (d != null && cita.getId().equals(d.getIdCitaVinculada())) agregarVistaDocumentoAnidado(h.contenedorDocs, d);
            }
            h.cabeceraDoc.setVisibility(h.contenedorDocs.getChildCount() > 0 ? View.VISIBLE : View.GONE);

            h.btnAttach.setOnClickListener(v -> mostrarFormularioRapido(cita));
            h.btnMenu.setOnClickListener(v -> mostrarDialogoEdicionPotente(cita));
        }

        private void agregarVistaDocumentoAnidado(LinearLayout contenedor, Documento doc) {
            View vistaDoc = getLayoutInflater().inflate(R.layout.item_related_doc, contenedor, false);
            ((TextView) vistaDoc.findViewById(R.id.tvSmallDocName)).setText(doc.getNombre());
            
            vistaDoc.findViewById(R.id.btnDeleteRelatedDoc).setOnClickListener(v -> {
                new AlertDialog.Builder(HistorialActivity.this).setTitle("Eliminar archivo").setMessage("¿Deseas desvincularlo?").setPositiveButton("Eliminar", (dialog, which) -> {
                    repositorio.eliminarDocumento(doc);
                    cargarHistorial();
                }).setNegativeButton("Cancelar", null).show();
            });

            vistaDoc.setOnClickListener(v -> abrirArchivoClinico(doc));
            contenedor.addView(vistaDoc);
        }

        @Override public int getItemCount() { return listaCitas.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEspecialidad, tvDoctor, tvFechaHora, tvLugar, tvDiagnostico, tvStatus;
            LinearLayout contenedorDocs, cabeceraDoc;
            View vLineaArriba, vLineaAbajo;
            ImageButton btnMenu; MaterialButton btnAttach;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvEspecialidad = v.findViewById(R.id.tvHistSpecialty);
                tvDoctor = v.findViewById(R.id.tvHistDoctor);
                tvFechaHora = v.findViewById(R.id.tvHistDateTime);
                tvLugar = v.findViewById(R.id.tvHistLocation);
                tvDiagnostico = v.findViewById(R.id.tvHistDiagnosis);
                tvStatus = v.findViewById(R.id.tvHistStatus);
                contenedorDocs = v.findViewById(R.id.layoutRelatedDocs);
                cabeceraDoc = v.findViewById(R.id.layoutDocHeader);
                vLineaArriba = v.findViewById(R.id.timeline_top);
                vLineaAbajo = v.findViewById(R.id.timeline_bottom);
                btnMenu = v.findViewById(R.id.btnHistMenu);
                btnAttach = v.findViewById(R.id.btnAttachDoc);
            }
        }
    }

    private void abrirArchivoClinico(Documento doc) {
        try {
            File archivo = new File(doc.getRuta());
            if (!archivo.exists()) return;
            Uri uri = FileProvider.getUriForFile(this, "com.mediqr.android.fileprovider", archivo);
            Intent intencion = new Intent(Intent.ACTION_VIEW);
            intencion.setDataAndType(uri, getContentResolver().getType(uri));
            intencion.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intencion);
        } catch (Exception e) {
            Toast.makeText(this, "Sin visor compatible", Toast.LENGTH_SHORT).show();
        }
    }
}
package com.mediqr.android;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.mediqr.android.datos.RepositorioMedico;
import com.mediqr.android.modelos.Documento;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Centro de Documentos Médicos: Gestión unificada de archivos.
 * Incluye buscador en tiempo real y filtrado por especialidades.
 */
public class DocumentosActivity extends AppCompatActivity {

    private RecyclerView rvDocumentos, rvEspecialidades;
    private TextView tvEstadoVacio, tvTituloReciente;
    private final List<Documento> todosLosDocs = new ArrayList<>();
    private final List<Documento> documentosAMostrar = new ArrayList<>();
    private final List<String> listaEspecialidades = new ArrayList<>();
    private AdaptadorDocs adaptadorDocs;
    private AdaptadorEspec adaptadorEspec;
    private RepositorioMedico repositorio;
    private ActivityResultLauncher<Intent> selectorArchivos;
    
    private String filtroEspecialidad = null;
    private String textoBusqueda = "";
    private String especSeleccionadaParaSubir = "";
    private String notaOpcionalParaSubir = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documentos);

        repositorio = RepositorioMedico.getInstancia(this);

        Toolbar t = findViewById(R.id.toolbar);
        setSupportActionBar(t);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        t.setNavigationOnClickListener(v -> finish());

        rvDocumentos = findViewById(R.id.rvDocuments);
        rvEspecialidades = findViewById(R.id.rvSpecialties);
        tvEstadoVacio = findViewById(R.id.tvEmptyState);
        tvTituloReciente = findViewById(R.id.tvRecentTitle);
        SearchView searchView = findViewById(R.id.searchDocs);
        View btnAgregar = findViewById(R.id.btnUploadDocument);

        adaptadorDocs = new AdaptadorDocs(documentosAMostrar, this::confirmarEliminacion, this::abrirArchivo);
        rvDocumentos.setLayoutManager(new LinearLayoutManager(this));
        rvDocumentos.setAdapter(adaptadorDocs);

        adaptadorEspec = new AdaptadorEspec(listaEspecialidades, this::alHacerClickEspec);
        rvEspecialidades.setLayoutManager(new GridLayoutManager(this, 3));
        rvEspecialidades.setAdapter(adaptadorEspec);

        selectorArchivos = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
            if (resultado.getResultCode() == Activity.RESULT_OK && resultado.getData() != null) {
                manejarArchivoSeleccionado(resultado.getData().getData());
            }
        });

        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) { 
                    textoBusqueda = query;
                    aplicarFiltros();
                    return true; 
                }
                @Override
                public boolean onQueryTextChange(String newText) { 
                    textoBusqueda = newText;
                    aplicarFiltros();
                    return true; 
                }
            });
        }

        btnAgregar.setOnClickListener(v -> mostrarFormularioSimplificado());
        
        cargarDatos();
    }

    private void cargarDatos() {
        List<Documento> actualList = repositorio.getDocumentos();
        todosLosDocs.clear();
        todosLosDocs.addAll(actualList);
        
        Set<String> especs = new HashSet<>();
        for (Documento d : todosLosDocs) especs.add(d.getEspecialidad());
        listaEspecialidades.clear();
        listaEspecialidades.addAll(especs);
        Collections.sort(listaEspecialidades);
        adaptadorEspec.notifyDataSetChanged();
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        documentosAMostrar.clear();
        String lowerBusqueda = textoBusqueda.toLowerCase();

        for (Documento d : todosLosDocs) {
            boolean cumpleEspec = (filtroEspecialidad == null || d.getEspecialidad().equals(filtroEspecialidad));
            boolean cumpleTexto = (textoBusqueda.isEmpty() || 
                                   d.getNombre().toLowerCase().contains(lowerBusqueda) || 
                                   d.getEspecialidad().toLowerCase().contains(lowerBusqueda));
            
            if (cumpleEspec && cumpleTexto) {
                documentosAMostrar.add(d);
            }
        }

        adaptadorDocs.notifyDataSetChanged();
        
        if (tvTituloReciente != null) {
            String titulo = (filtroEspecialidad == null) ? "Todos los Documentos" : "Especialidad: " + filtroEspecialidad;
            if (!textoBusqueda.isEmpty()) titulo += " (Filtrado)";
            tvTituloReciente.setText(titulo);
        }
        
        if (tvEstadoVacio != null) {
            tvEstadoVacio.setVisibility(documentosAMostrar.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void alHacerClickEspec(String s) {
        if (s != null && s.equals(filtroEspecialidad)) filtroEspecialidad = null;
        else filtroEspecialidad = s;
        aplicarFiltros();
    }

    private void mostrarFormularioSimplificado() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_subir_archivo_rapido, null);
        b.setView(v);

        Spinner sEspec = v.findViewById(R.id.spinnerQuickEspec);
        TextInputEditText eNota = v.findViewById(R.id.editQuickNota);

        String[] especs = {"Cardiología", "Odontología", "Dermatología", "General", "Oftalmología", "Ginecología", "Pediatría", "Traumatología", "Otros"};
        sEspec.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, especs));

        AlertDialog d = b.create();
        v.findViewById(R.id.btnQuickSeleccionar).setOnClickListener(view -> {
            especSeleccionadaParaSubir = sEspec.getSelectedItem().toString();
            notaOpcionalParaSubir = eNota.getText().toString().trim();
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            selectorArchivos.launch(i);
            d.dismiss();
        });
        v.findViewById(R.id.btnQuickCancelar).setOnClickListener(view -> d.dismiss());
        d.show();
    }

    private void manejarArchivoSeleccionado(Uri uri) {
        try {
            String nombreOrig = obtenerNombreArchivo(uri);
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new java.util.Date());
            File archivo = new File(getFilesDir(), "doc_" + System.currentTimeMillis() + "_" + (nombreOrig != null ? nombreOrig : "adjunto"));
            
            InputStream is = getContentResolver().openInputStream(uri);
            FileOutputStream os = new FileOutputStream(archivo);
            byte[] buf = new byte[1024];
            int r;
            if (is != null) {
                while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
                is.close();
            }
            os.close();

            String nombreFinal = (notaOpcionalParaSubir != null && !notaOpcionalParaSubir.isEmpty()) ? notaOpcionalParaSubir : "Documento " + especSeleccionadaParaSubir;
            Documento d = new Documento(UUID.randomUUID().toString(), nombreFinal, fechaActual, archivo.getAbsolutePath(), especSeleccionadaParaSubir, "Usuario", "MediQR", "Archivo");
            repositorio.agregarDocumento(d);
            cargarDatos();
            Toast.makeText(this, "Documento guardado", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            e.printStackTrace();
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

    private void confirmarEliminacion(int pos) {
        new AlertDialog.Builder(this).setTitle("Eliminar").setMessage("¿Borrar este documento?").setPositiveButton("Sí", (d, w) -> {
            repositorio.eliminarDocumento(documentosAMostrar.get(pos));
            cargarDatos();
        }).setNegativeButton("No", null).show();
    }

    private void abrirArchivo(int pos) {
        Documento d = documentosAMostrar.get(pos);
        File f = new File(d.getRuta());
        if (!f.exists()) { Toast.makeText(this, "Archivo no encontrado", Toast.LENGTH_SHORT).show(); return; }
        Uri u = FileProvider.getUriForFile(this, "com.mediqr.android.fileprovider", f);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(u, getContentResolver().getType(u));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(i); } catch (Exception e) { Toast.makeText(this, "Sin visor", Toast.LENGTH_SHORT).show(); }
    }

    private static class AdaptadorEspec extends RecyclerView.Adapter<AdaptadorEspec.VH> {
        private final List<String> lista;
        private final OnClick escuchador;
        public interface OnClick { void onClick(String s); }
        public AdaptadorEspec(List<String> l, OnClick click) { this.lista = l; this.escuchador = click; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_especialidad, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            h.t.setText(lista.get(p));
            h.itemView.setOnClickListener(v -> escuchador.onClick(lista.get(p)));
        }
        @Override public int getItemCount() { return lista.size(); }
        static class VH extends RecyclerView.ViewHolder { TextView t; VH(View v) { super(v); t = v.findViewById(R.id.tvSpecName); } }
    }

    private static class AdaptadorDocs extends RecyclerView.Adapter<AdaptadorDocs.VH> {
        private final List<Documento> lista;
        private final OnAct eliminador, abridor;
        public interface OnAct { void onAct(int p); }
        public AdaptadorDocs(List<Documento> l, OnAct d, OnAct o) { this.lista = l; this.eliminador = d; this.abridor = o; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_documento, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            Documento d = lista.get(p);
            h.n.setText(d.getNombre());
            h.d.setText(d.getEspecialidad() + " • " + d.getFecha());
            
            if (d.getIdCitaVinculada() != null && !d.getIdCitaVinculada().isEmpty()) {
                h.vinc.setVisibility(View.VISIBLE);
            } else {
                h.vinc.setVisibility(View.GONE);
            }

            h.i.setImageResource(d.getNombre().toLowerCase().endsWith(".pdf") ? android.R.drawable.ic_menu_agenda : android.R.drawable.ic_menu_gallery);
            h.b.setOnClickListener(v -> eliminador.onAct(p));
            h.itemView.setOnClickListener(v -> abridor.onAct(p));
        }
        @Override public int getItemCount() { return lista.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView n, d, vinc; ImageView i; ImageButton b;
            VH(View v) {
                super(v);
                n = v.findViewById(R.id.tvDocName);
                d = v.findViewById(R.id.tvDocDate);
                vinc = v.findViewById(R.id.tvDocLinked);
                i = v.findViewById(R.id.ivDocIcon);
                b = v.findViewById(R.id.btnDeleteDoc);
            }
        }
    }
}
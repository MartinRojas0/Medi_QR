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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.mediqr.android.data.MedicalRepository;
import com.mediqr.android.models.Documento;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class DocumentosActivity extends AppCompatActivity {

    private RecyclerView rvDocuments, rvSpecialties;
    private TextView tvEmptyState, tvRecentTitle;
    private List<Documento> allDocs, displayDocs;
    private List<String> specList;
    private DocAdapter docAdapter;
    private SpecAdapter specAdapter;
    private MedicalRepository repository;
    private ActivityResultLauncher<Intent> picker;
    private Documento temp;
    private String currentFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documentos);

        repository = MedicalRepository.getInstance(this);

        Toolbar t = findViewById(R.id.toolbar);
        setSupportActionBar(t);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        t.setNavigationOnClickListener(v -> finish());

        rvDocuments = findViewById(R.id.rvDocuments);
        rvSpecialties = findViewById(R.id.rvSpecialties);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvRecentTitle = findViewById(R.id.tvRecentTitle);
        View btnAdd = findViewById(R.id.btnUploadDocument);

        allDocs = new ArrayList<>();
        displayDocs = new ArrayList<>();
        specList = new ArrayList<>();

        docAdapter = new DocAdapter(displayDocs, this::confirmDelete, this::openFile);
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        rvDocuments.setAdapter(docAdapter);

        specAdapter = new SpecAdapter(specList, this::onSpecClick);
        rvSpecialties.setLayoutManager(new GridLayoutManager(this, 3));
        rvSpecialties.setAdapter(specAdapter);

        picker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), res -> {
            if (res.getResultCode() == Activity.RESULT_OK && res.getData() != null) handle(res.getData().getData());
        });

        btnAdd.setOnClickListener(v -> showForm());
        
        load();
    }

    private void load() {
        allDocs = repository.getDocuments();
        Set<String> specs = new HashSet<>();
        for (Documento d : allDocs) specs.add(d.getEspecialidad());
        specList.clear();
        specList.addAll(specs);
        Collections.sort(specList);
        specAdapter.notifyDataSetChanged();
        filter(currentFilter);
    }

    private void filter(String s) {
        currentFilter = s;
        displayDocs.clear();
        if (s == null) {
            displayDocs.addAll(allDocs);
        } else {
            for (Documento d : allDocs) {
                if (d.getEspecialidad().equals(s)) displayDocs.add(d);
            }
        }
        docAdapter.notifyDataSetChanged();
        if (tvRecentTitle != null) {
            tvRecentTitle.setText(s == null ? "Todos los Documentos" : "Especialidad: " + s);
        }
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(displayDocs.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void onSpecClick(String s) {
        if (s != null && s.equals(currentFilter)) {
            filter(null);
        } else {
            filter(s);
        }
    }

    private void showForm() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_subir_documento, null);
        b.setView(v);

        Spinner sSpec = v.findViewById(R.id.spinnerDocSpecialty);
        Spinner sType = v.findViewById(R.id.spinnerDocType);
        TextInputEditText eDoc = v.findViewById(R.id.editDocDoctor);
        TextInputEditText eLoc = v.findViewById(R.id.editDocLocation);
        TextInputEditText eDate = v.findViewById(R.id.editDocDate);

        String[] specs = {"Cardiología", "Odontología", "Dermatología", "General", "Oftalmología", "Ginecología", "Pediatría", "Traumatología", "Otros"};
        sSpec.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, specs));
        String[] types = {"Receta", "Análisis", "Informe", "Vacuna", "Imagen"};
        sType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));

        eDate.setOnClickListener(view -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view1, year, month, day) -> eDate.setText(day+"/"+(month+1)+"/"+year), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog d = b.create();
        v.findViewById(R.id.btnSelectAndUpload).setOnClickListener(view -> {
            if (eDoc.getText().toString().isEmpty()) return;
            temp = new Documento(UUID.randomUUID().toString(), "", eDate.getText().toString(), "", sSpec.getSelectedItem().toString(), eDoc.getText().toString(), eLoc.getText().toString(), sType.getSelectedItem().toString());
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            picker.launch(i);
            d.dismiss();
        });
        d.show();
    }

    private void handle(Uri uri) {
        try {
            String name = getFileName(uri);
            File file = new File(getFilesDir(), "doc_" + System.currentTimeMillis() + "_" + name);
            InputStream is = getContentResolver().openInputStream(uri);
            FileOutputStream os = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int r;
            while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
            is.close();
            os.close();

            if (temp != null) {
                Documento d = new Documento(temp.getId(), name, temp.getFecha(), file.getAbsolutePath(), temp.getEspecialidad(), temp.getDoctor(), temp.getLugar(), temp.getTipo());
                repository.addDocument(d);
                load();
                Toast.makeText(this, "Documento guardado", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFileName(Uri uri) {
        String r = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (i != -1) r = c.getString(i);
                }
            }
        }
        if (r == null) {
            r = uri.getPath();
            if (r != null) {
                int cut = r.lastIndexOf('/');
                if (cut != -1) r = r.substring(cut + 1);
            }
        }
        return r;
    }

    private void confirmDelete(int pos) {
        new AlertDialog.Builder(this).setTitle("Eliminar").setMessage("¿Borrar este documento?").setPositiveButton("Sí", (d, w) -> {
            repository.deleteDocument(displayDocs.get(pos));
            load();
        }).setNegativeButton("No", null).show();
    }

    private void openFile(int pos) {
        Documento d = displayDocs.get(pos);
        File f = new File(d.getRuta());
        Uri u = FileProvider.getUriForFile(this, "com.mediqr.android.fileprovider", f);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(u, getContentResolver().getType(u));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Sin visor", Toast.LENGTH_SHORT).show();
        }
    }

    private static class SpecAdapter extends RecyclerView.Adapter<SpecAdapter.VH> {
        private List<String> list;
        private OnClick listener;
        public interface OnClick { void onClick(String s); }
        public SpecAdapter(List<String> l, OnClick click) { this.list = l; this.listener = click; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_especialidad, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            h.t.setText(list.get(p));
            h.itemView.setOnClickListener(v -> listener.onClick(list.get(p)));
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView t;
            VH(View v) { super(v); t = v.findViewById(R.id.tvSpecName); }
        }
    }

    private static class DocAdapter extends RecyclerView.Adapter<DocAdapter.VH> {
        private List<Documento> list;
        private OnAct del, open;
        public interface OnAct { void onAct(int p); }
        public DocAdapter(List<Documento> l, OnAct d, OnAct o) { this.list = l; this.del = d; this.open = o; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_documento, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            Documento d = list.get(p);
            h.n.setText(d.getTipo() + " - " + d.getEspecialidad());
            h.d.setText(d.getFecha());
            h.i.setImageResource(d.getNombre().toLowerCase().endsWith(".pdf") ? android.R.drawable.ic_menu_agenda : android.R.drawable.ic_menu_gallery);
            h.b.setOnClickListener(v -> del.onAct(p));
            h.itemView.setOnClickListener(v -> open.onAct(p));
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView n, d;
            ImageView i;
            ImageButton b;
            VH(View v) {
                super(v);
                n = v.findViewById(R.id.tvDocName);
                d = v.findViewById(R.id.tvDocDate);
                i = v.findViewById(R.id.ivDocIcon);
                b = v.findViewById(R.id.btnDeleteDoc);
            }
        }
    }
}
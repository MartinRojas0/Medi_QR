package com.mediqr.android;

import android.app.Activity;
import android.app.DatePickerDialog;
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

/**
 * DocumentosActivity: Repositorio documental profesional.
 * Preparado para conectarse a Cloud Storage vía MedicalRepository.
 */
public class DocumentosActivity extends AppCompatActivity {

    private RecyclerView rvDocuments, rvSpecialties;
    private TextView tvEmptyState, tvSpecTitle, tvRecentTitle;
    private List<Documento> allDocuments;
    private List<Documento> displayedDocuments;
    private List<String> specialtyList;
    private DocumentAdapter docAdapter;
    private SpecialtyAdapter specAdapter;
    private MedicalRepository repository;
    
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Documento tempDocInfo;
    private String selectedSpecialtyFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documentos);

        repository = MedicalRepository.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvDocuments = findViewById(R.id.rvDocuments);
        rvSpecialties = findViewById(R.id.rvSpecialties);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvSpecTitle = findViewById(R.id.tvSpecTitle);
        tvRecentTitle = findViewById(R.id.tvRecentTitle);
        Button btnUpload = findViewById(R.id.btnUploadDocument);

        allDocuments = new ArrayList<>();
        displayedDocuments = new ArrayList<>();
        specialtyList = new ArrayList<>();
        
        loadData();

        docAdapter = new DocumentAdapter(displayedDocuments, this::showDeleteConfirmation, this::openDocument);
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        rvDocuments.setAdapter(docAdapter);

        specAdapter = new SpecialtyAdapter(specialtyList, this::filterBySpecialty);
        rvSpecialties.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSpecialties.setAdapter(specAdapter);

        updateUI();

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        handleFileSelection(uri);
                    }
                }
        );

        btnUpload.setOnClickListener(v -> showUploadDialog());
    }

    private void loadData() {
        allDocuments = repository.getDocuments();
        
        // Extraer especialidades únicas
        Set<String> specs = new HashSet<>();
        for (Documento d : allDocuments) specs.add(d.getEspecialidad());
        specialtyList.clear();
        specialtyList.addAll(specs);
        Collections.sort(specialtyList);
        
        filterBySpecialty(selectedSpecialtyFilter);
    }

    private void updateUI() {
        boolean isEmpty = allDocuments.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvDocuments.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        rvSpecialties.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvSpecTitle.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvRecentTitle.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (!isEmpty) {
            tvRecentTitle.setText(selectedSpecialtyFilter != null ? 
                    "Documentos de " + selectedSpecialtyFilter : "Documentos Recientes");
        }
    }

    private void filterBySpecialty(String specialty) {
        if (specialty != null && specialty.equals(selectedSpecialtyFilter)) {
            selectedSpecialtyFilter = null; 
        } else {
            selectedSpecialtyFilter = specialty;
        }
        
        displayedDocuments.clear();
        if (selectedSpecialtyFilter == null) {
            displayedDocuments.addAll(allDocuments);
        } else {
            for (Documento d : allDocuments) {
                if (d.getEspecialidad().equals(selectedSpecialtyFilter)) displayedDocuments.add(d);
            }
        }
        if (docAdapter != null) docAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void showUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_subir_documento, null);
        builder.setView(view);

        Spinner spinnerSpecialty = view.findViewById(R.id.spinnerDocSpecialty);
        Spinner spinnerType = view.findViewById(R.id.spinnerDocType);
        TextInputEditText editDoctor = view.findViewById(R.id.editDocDoctor);
        TextInputEditText editLocation = view.findViewById(R.id.editDocLocation);
        TextInputEditText editDate = view.findViewById(R.id.editDocDate);
        Button btnCancel = view.findViewById(R.id.btnCancelDocUpload);
        Button btnSelect = view.findViewById(R.id.btnSelectAndUpload);

        String[] specialties = {"Cardiología", "Odontología", "Dermatología", "Medicina General", "Oftalmología", "Ginecología", "Pediatría", "Traumatología", "Otros"};
        spinnerSpecialty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, specialties));

        String[] types = {"Receta", "Análisis", "Informe", "Vacunación", "Otro"};
        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));

        editDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (dView, year, month, dayOfMonth) -> {
                String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                editDate.setText(date);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog dialog = builder.create();

        btnSelect.setOnClickListener(v -> {
            String doctor = editDoctor.getText().toString().trim();
            String date = editDate.getText().toString().trim();
            if (doctor.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            tempDocInfo = new Documento(UUID.randomUUID().toString(), "", date, "",
                    spinnerSpecialty.getSelectedItem().toString(), doctor,
                    editLocation.getText().toString().trim(), spinnerType.getSelectedItem().toString());

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
            filePickerLauncher.launch(intent);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleFileSelection(Uri uri) {
        try {
            String fileName = getFileName(uri);
            File internalFile = new File(getFilesDir(), "doc_" + System.currentTimeMillis() + "_" + fileName);
            
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(internalFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.close();

            if (tempDocInfo != null) {
                Documento newDoc = new Documento(tempDocInfo.getId(), fileName, tempDocInfo.getFecha(),
                        internalFile.getAbsolutePath(), tempDocInfo.getEspecialidad(),
                        tempDocInfo.getDoctor(), tempDocInfo.getLugar(), tempDocInfo.getTipo());
                
                repository.addDocument(newDoc);
                loadData();
                specAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Documento subido con éxito", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar archivo", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDocument(int position) {
        Documento doc = displayedDocuments.get(position);
        File file = new File(doc.getRuta());
        if (!file.exists()) {
            Toast.makeText(this, "El archivo no existe", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, "com.mediqr.android.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, getContentResolver().getType(uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No hay aplicación para abrir este archivo", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void showDeleteConfirmation(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar documento")
                .setMessage("¿Estás seguro de eliminar este documento?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    repository.deleteDocument(displayedDocuments.get(position));
                    loadData();
                    specAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Documento eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    // --- ADAPTERS ---
    private static class SpecialtyAdapter extends RecyclerView.Adapter<SpecialtyAdapter.ViewHolder> {
        private List<String> list;
        private OnSpecClickListener listener;
        public interface OnSpecClickListener { void onClick(String spec); }
        public SpecialtyAdapter(List<String> list, OnSpecClickListener listener) {
            this.list = list; this.listener = listener;
        }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_especialidad, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String spec = list.get(position);
            holder.tvName.setText(spec);
            holder.itemView.setOnClickListener(v -> listener.onClick(spec));
        }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            public ViewHolder(@NonNull View v) { super(v); tvName = v.findViewById(R.id.tvSpecName); }
        }
    }

    private static class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {
        private List<Documento> docs;
        private OnActionCallback deleteListener, openListener;
        public interface OnActionCallback { void onAction(int pos); }
        public DocumentAdapter(List<Documento> docs, OnActionCallback del, OnActionCallback open) {
            this.docs = docs; this.deleteListener = del; this.openListener = open;
        }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_documento, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Documento doc = docs.get(position);
            holder.tvName.setText(doc.getTipo() + " - " + doc.getEspecialidad());
            holder.tvDate.setText(doc.getFecha() + " | Dr. " + doc.getDoctor());
            holder.ivIcon.setImageResource(doc.getNombre().toLowerCase().endsWith(".pdf") ? 
                    android.R.drawable.ic_menu_agenda : android.R.drawable.ic_menu_gallery);
            holder.btnDelete.setOnClickListener(v -> deleteListener.onAction(position));
            holder.itemView.setOnClickListener(v -> openListener.onAction(position));
        }
        @Override public int getItemCount() { return docs.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDate; ImageView ivIcon; ImageButton btnDelete;
            public ViewHolder(@NonNull View v) {
                super(v); tvName = v.findViewById(R.id.tvDocName); tvDate = v.findViewById(R.id.tvDocDate);
                ivIcon = v.findViewById(R.id.ivDocIcon); btnDelete = v.findViewById(R.id.btnDeleteDoc);
            }
        }
    }
}
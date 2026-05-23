package com.mediqr.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.mediqr.android.data.MedicalRepository;
import com.mediqr.android.models.Cita;
import com.mediqr.android.models.Documento;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * HistorialActivity: Premium Timeline with integrated Document linking.
 */
public class HistorialActivity extends AppCompatActivity {

    private RecyclerView rvHistorial;
    private TextView tvEmpty;
    private MedicalRepository repository;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private String currentLinkingCitaId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        repository = MedicalRepository.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbarHistorial);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvHistorial = findViewById(R.id.rvHistorial);
        tvEmpty = findViewById(R.id.tvEmptyHistorial);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleFileSelection(result.getData().getData());
                    }
                }
        );

        loadHistorial();
    }

    private void loadHistorial() {
        List<Cita> allCitas = repository.getAppointments(false);
        List<Cita> attendedCitas = new ArrayList<>();
        for (Cita c : allCitas) if (c.isAtendida()) attendedCitas.add(c);
        Collections.reverse(attendedCitas);

        List<Documento> allDocs = repository.getDocuments();

        HistorialAdapter adapter = new HistorialAdapter(attendedCitas, allDocs);
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        rvHistorial.setAdapter(adapter);

        tvEmpty.setVisibility(attendedCitas.isEmpty() ? View.VISIBLE : View.GONE);
        rvHistorial.setVisibility(attendedCitas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void handleFileSelection(Uri uri) {
        try {
            String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            File internalFile = new File(getFilesDir(), "hist_" + System.currentTimeMillis());
            
            InputStream is = getContentResolver().openInputStream(uri);
            FileOutputStream os = new FileOutputStream(internalFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
            is.close(); os.close();

            Documento newDoc = new Documento(UUID.randomUUID().toString(), "Adjunto Historial", date, internalFile.getAbsolutePath(), "General", "Dr. Adjunto", "Clínica", "Informe");
            newDoc.setLinkedCitaId(currentLinkingCitaId);
            
            repository.addDocument(newDoc);
            loadHistorial();
            Toast.makeText(this, "Documento vinculado", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al vincular", Toast.LENGTH_SHORT).show();
        }
    }

    private class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {
        private List<Cita> citas;
        private List<Documento> docs;

        public HistorialAdapter(List<Cita> citas, List<Documento> docs) {
            this.citas = citas; this.docs = docs;
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Cita cita = citas.get(position);
            holder.tvSpecialty.setText(cita.getEspecialidad());
            holder.tvDoctor.setText("Dr. " + cita.getDoctor());
            holder.tvDateTime.setText(cita.getFecha());
            holder.tvLocation.setText(cita.getLugar());
            holder.tvDiagnosis.setText(cita.getDiagnostico());

            holder.vLineTop.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
            holder.vLineBottom.setVisibility(position == citas.size() - 1 ? View.INVISIBLE : View.VISIBLE);

            // Integrated Documents
            holder.layoutDocsContainer.removeAllViews();
            boolean hasDocs = false;
            for (Documento d : docs) {
                if (d.getLinkedCitaId().equals(cita.getId()) || d.getEspecialidad().equalsIgnoreCase(cita.getEspecialidad())) {
                    addDocToRow(holder.layoutDocsContainer, d);
                    hasDocs = true;
                }
            }
            holder.layoutDocHeader.setVisibility(hasDocs ? View.VISIBLE : View.GONE);

            // Action Buttons
            holder.btnAttach.setOnClickListener(v -> {
                currentLinkingCitaId = cita.getId();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                filePickerLauncher.launch(intent);
            });
        }

        private void addDocToRow(LinearLayout container, Documento doc) {
            View v = getLayoutInflater().inflate(R.layout.item_related_doc, container, false);
            ((TextView) v.findViewById(R.id.tvSmallDocName)).setText(doc.getTipo() + ": " + doc.getNombre());
            v.setOnClickListener(view -> openFile(doc));
            container.addView(v);
        }

        @Override public int getItemCount() { return citas.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSpecialty, tvDoctor, tvDateTime, tvLocation, tvDiagnosis;
            LinearLayout layoutDocsContainer, layoutDocHeader;
            View vLineTop, vLineBottom;
            Button btnAttach;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvSpecialty = v.findViewById(R.id.tvHistSpecialty);
                tvDoctor = v.findViewById(R.id.tvHistDoctor);
                tvDateTime = v.findViewById(R.id.tvHistDateTime);
                tvLocation = v.findViewById(R.id.tvHistLocation);
                tvDiagnosis = v.findViewById(R.id.tvHistDiagnosis);
                layoutDocsContainer = v.findViewById(R.id.layoutRelatedDocs);
                layoutDocHeader = v.findViewById(R.id.layoutDocHeader);
                vLineTop = v.findViewById(R.id.timeline_top);
                vLineBottom = v.findViewById(R.id.timeline_bottom);
                btnAttach = v.findViewById(R.id.btnAttachDoc);
            }
        }
    }

    private void openFile(Documento doc) {
        File file = new File(doc.getRuta());
        if (!file.exists()) { Toast.makeText(this, "Archivo no disponible", Toast.LENGTH_SHORT).show(); return; }
        Uri uri = FileProvider.getUriForFile(this, "com.mediqr.android.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, getContentResolver().getType(uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(intent); } catch (Exception e) { Toast.makeText(this, "Sin visor compatible", Toast.LENGTH_SHORT).show(); }
    }
}
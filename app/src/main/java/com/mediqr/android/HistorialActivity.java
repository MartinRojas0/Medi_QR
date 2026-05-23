package com.mediqr.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mediqr.android.data.MedicalRepository;
import com.mediqr.android.models.Cita;
import com.mediqr.android.models.Documento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HistorialActivity: Registro cronológico de atenciones.
 * Muestra la relación entre consultas y evidencias médicas.
 */
public class HistorialActivity extends AppCompatActivity {

    private RecyclerView rvHistorial;
    private TextView tvEmpty;
    private MedicalRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        repository = MedicalRepository.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbarHistorial);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvHistorial = findViewById(R.id.rvHistorial);
        tvEmpty = findViewById(R.id.tvEmptyHistorial);

        loadHistorial();
    }

    private void loadHistorial() {
        List<Cita> allCitas = repository.getAppointments(false);
        List<Cita> attendedCitas = new ArrayList<>();
        for (Cita c : allCitas) {
            if (c.isAtendida()) attendedCitas.add(c);
        }
        Collections.reverse(attendedCitas);

        List<Documento> allDocs = repository.getDocuments();

        HistorialAdapter adapter = new HistorialAdapter(attendedCitas, allDocs);
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        rvHistorial.setAdapter(adapter);

        if (attendedCitas.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvHistorial.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvHistorial.setVisibility(View.VISIBLE);
        }
    }

    private static class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {
        private List<Cita> citas;
        private List<Documento> docs;

        public HistorialAdapter(List<Cita> citas, List<Documento> docs) {
            this.citas = citas;
            this.docs = docs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Cita cita = citas.get(position);
            holder.tvSpecialty.setText(cita.getEspecialidad());
            holder.tvDoctor.setText("Dr. " + cita.getDoctor());
            holder.tvDateTime.setText(cita.getFecha());
            holder.tvLocation.setText(cita.getLugar());

            holder.layoutDocs.removeAllViews();
            boolean hasDocs = false;
            for (Documento d : docs) {
                if (d.getEspecialidad().equalsIgnoreCase(cita.getEspecialidad())) {
                    TextView tvDoc = new TextView(holder.itemView.getContext());
                    tvDoc.setText("• " + d.getTipo() + ": " + d.getNombre());
                    tvDoc.setTextSize(13);
                    tvDoc.setPadding(0, 4, 0, 4);
                    holder.layoutDocs.addView(tvDoc);
                    hasDocs = true;
                }
            }

            holder.divider.setVisibility(hasDocs ? View.VISIBLE : View.GONE);
            holder.tvDocTitle.setVisibility(hasDocs ? View.VISIBLE : View.GONE);
            holder.layoutDocs.setVisibility(hasDocs ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() { return citas.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSpecialty, tvDoctor, tvDateTime, tvLocation, tvDocTitle;
            LinearLayout layoutDocs;
            View divider;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvSpecialty = v.findViewById(R.id.tvHistSpecialty);
                tvDoctor = v.findViewById(R.id.tvHistDoctor);
                tvDateTime = v.findViewById(R.id.tvHistDateTime);
                tvLocation = v.findViewById(R.id.tvHistLocation);
                tvDocTitle = v.findViewById(R.id.tvDocTitle);
                layoutDocs = v.findViewById(R.id.layoutRelatedDocs);
                divider = v.findViewById(R.id.dividerHist);
            }
        }
    }
}
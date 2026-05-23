package com.mediqr.android;

import android.content.Context;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistorialActivity extends AppCompatActivity {

    private RecyclerView rvHistorial;
    private TextView tvEmpty;
    private List<Cita> attendedCitas;
    private List<Documento> allDocs;
    private HistorialAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        Toolbar toolbar = findViewById(R.id.toolbarHistorial);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvHistorial = findViewById(R.id.rvHistorial);
        tvEmpty = findViewById(R.id.tvEmptyHistorial);

        loadData();

        adapter = new HistorialAdapter(attendedCitas, allDocs);
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        rvHistorial.setAdapter(adapter);

        if (attendedCitas.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvHistorial.setVisibility(View.GONE);
        }
    }

    private void loadData() {
        // Cargar citas atendidas
        SharedPreferences citaPrefs = getSharedPreferences("MediQR_Citas", Context.MODE_PRIVATE);
        Set<String> citaSet = citaPrefs.getStringSet("citas_set", new HashSet<>());
        attendedCitas = new ArrayList<>();
        for (String s : citaSet) {
            Cita c = Cita.fromString(s);
            if (c != null && c.isAtendida()) attendedCitas.add(c);
        }
        Collections.sort(attendedCitas, Cita.DateComparator);
        Collections.reverse(attendedCitas); // Más recientes primero

        // Cargar todos los documentos para relacionar
        SharedPreferences docPrefs = getSharedPreferences("MediQR_Docs", Context.MODE_PRIVATE);
        Set<String> docSet = docPrefs.getStringSet("docs_list", new HashSet<>());
        allDocs = new ArrayList<>();
        for (String s : docSet) {
            Documento d = Documento.fromString(s);
            if (d != null) allDocs.add(d);
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Cita cita = citas.get(position);
            holder.tvSpecialty.setText(cita.getEspecialidad());
            holder.tvDoctor.setText("Dr. " + cita.getDoctor());
            holder.tvDateTime.setText(cita.getFecha());
            holder.tvLocation.setText(cita.getLugar());

            // Relacionar documentos por especialidad
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

            if (hasDocs) {
                holder.divider.setVisibility(View.VISIBLE);
                holder.tvDocTitle.setVisibility(View.VISIBLE);
                holder.layoutDocs.setVisibility(View.VISIBLE);
            } else {
                holder.divider.setVisibility(View.GONE);
                holder.tvDocTitle.setVisibility(View.GONE);
                holder.layoutDocs.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return citas.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSpecialty, tvDoctor, tvDateTime, tvLocation, tvDocTitle;
            LinearLayout layoutDocs;
            View divider;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSpecialty = itemView.findViewById(R.id.tvHistSpecialty);
                tvDoctor = itemView.findViewById(R.id.tvHistDoctor);
                tvDateTime = itemView.findViewById(R.id.tvHistDateTime);
                tvLocation = itemView.findViewById(R.id.tvHistLocation);
                tvDocTitle = itemView.findViewById(R.id.tvDocTitle);
                layoutDocs = itemView.findViewById(R.id.layoutRelatedDocs);
                divider = itemView.findViewById(R.id.dividerHist);
            }
        }
    }
}
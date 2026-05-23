package com.mediqr.android;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.util.ArrayList;

public class EstadisticasFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_estadisticas, container, false);

        TextView totalConsultationsText = view.findViewById(R.id.totalConsultationsText);
        TextView upcomingAppointmentsText = view.findViewById(R.id.upcomingAppointmentsText);
        BarChart barChart = view.findViewById(R.id.barChart);
        PieChart pieChart = view.findViewById(R.id.pieChart);

        // TODO: Cargar datos reales desde backend
        totalConsultationsText.setText("Consultas totales: 24");
        upcomingAppointmentsText.setText("Citas próximas: 3");

        // Configurar gráfico de barras (consultas por mes)
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        barEntries.add(new BarEntry(1, 4));
        barEntries.add(new BarEntry(2, 6));
        barEntries.add(new BarEntry(3, 8));
        barEntries.add(new BarEntry(4, 3));
        barEntries.add(new BarEntry(5, 7));
        barEntries.add(new BarEntry(6, 5));

        BarDataSet barDataSet = new BarDataSet(barEntries, "Consultas por mes");
        barDataSet.setColor(Color.parseColor("#1976D2"));
        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.invalidate();

        // Configurar gráfico circular (distribución de especialidades)
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(40, "Cardiología"));
        pieEntries.add(new PieEntry(30, "Medicina General"));
        pieEntries.add(new PieEntry(20, "Dermatología"));
        pieEntries.add(new PieEntry(10, "Oftalmología"));

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "Especialidades");
        pieDataSet.setColors(Color.parseColor("#1976D2"), Color.parseColor("#42A5F5"),
                Color.parseColor("#90CAF9"), Color.parseColor("#BBDEFB"));
        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();

        return view;
    }
}
package com.mediqr.android;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.mediqr.android.data.MedicalRepository;
import com.mediqr.android.models.Cita;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CitasFragment extends Fragment {

    private RecyclerView rvAppointments;
    private TextView tvEmpty;
    private List<Cita> citaList;
    private CitaAdapter adapter;
    private MedicalRepository repository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_citas, container, false);

        repository = MedicalRepository.getInstance(requireContext());
        rvAppointments = view.findViewById(R.id.rvAppointments);
        tvEmpty = view.findViewById(R.id.tvEmptyAppointments);
        Button btnNewCita = view.findViewById(R.id.newAppointmentButton);

        refreshData();

        adapter = new CitaAdapter(citaList, this::showCancelConfirmation, this::showRescheduleDialog, this::askToAttend);
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAppointments.setAdapter(adapter);

        btnNewCita.setOnClickListener(v -> showNewCitaDialog(null));

        return view;
    }

    private void refreshData() {
        citaList = repository.getAppointments(true);
        updateEmptyState();
        if (adapter != null) adapter.updateData(citaList);
    }

    private void updateEmptyState() {
        if (citaList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvAppointments.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvAppointments.setVisibility(View.VISIBLE);
        }
    }

    private void showNewCitaDialog(Cita existingCita) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nueva_cita, null);
        builder.setView(dialogView);

        TextInputEditText editSpecialtyText = dialogView.findViewById(R.id.editCitaSpecialty);
        TextInputEditText editDoctor = dialogView.findViewById(R.id.editCitaDoctor);
        TextInputEditText editDate = dialogView.findViewById(R.id.editCitaDate);
        TextInputEditText editTime = dialogView.findViewById(R.id.editCitaTime);
        TextInputEditText editLocation = dialogView.findViewById(R.id.editCitaLocation);
        Button btnSave = dialogView.findViewById(R.id.btnSaveNewCita);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelNewCita);

        String[] specialties = {"Cardiología", "Odontología", "Dermatología", "Medicina General", "Oftalmología", "Ginecología", "Pediatría", "Traumatología", "Otros"};
        editSpecialtyText.setFocusable(false);
        editSpecialtyText.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext()).setTitle("Seleccione Especialidad").setItems(specialties, (dialog, which) -> editSpecialtyText.setText(specialties[which])).show();
        });

        if (existingCita != null) {
            editSpecialtyText.setText(existingCita.getEspecialidad());
            editDoctor.setText(existingCita.getDoctor());
            editDate.setText(existingCita.getFecha());
            editTime.setText(existingCita.getHora());
            editLocation.setText(existingCita.getLugar());
            btnSave.setText("ACTUALIZAR");
        }

        editDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                editDate.setText(selectedDate);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        editTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
                String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                editTime.setText(selectedTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String specialty = editSpecialtyText.getText().toString().trim();
            String doctor = editDoctor.getText().toString().trim();
            String date = editDate.getText().toString().trim();
            String time = editTime.getText().toString().trim();
            String location = editLocation.getText().toString().trim();

            if (specialty.isEmpty() || doctor.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(getContext(), "Por favor rellene los campos principales", Toast.LENGTH_SHORT).show();
                return;
            }

            Cita newCita = new Cita(existingCita != null ? existingCita.getId() : UUID.randomUUID().toString(), specialty, doctor, date, time, location, false);
            repository.saveAppointment(newCita);
            refreshData();
            Toast.makeText(getContext(), "Cita guardada correctamente", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void askToAttend(int position) {
        Cita cita = citaList.get(position);
        new AlertDialog.Builder(requireContext())
                .setTitle("Finalizar Cita")
                .setMessage("¿Deseas marcar esta cita como ATENDIDA y moverla al Historial Médico?")
                .setPositiveButton("Sí, Mover", (dialog, which) -> {
                    cita.setAtendida(true);
                    repository.saveAppointment(cita);
                    refreshData();
                    Toast.makeText(getContext(), "Cita movida al historial", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showCancelConfirmation(int position) {
        Cita cita = citaList.get(position);
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancelar Cita")
                .setMessage("¿Estás seguro de que deseas cancelar esta cita?")
                .setPositiveButton("Sí, Cancelar", (dialog, which) -> {
                    repository.deleteAppointment(cita.getId());
                    refreshData();
                    Toast.makeText(getContext(), "Cita cancelada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showRescheduleDialog(int position) {
        showNewCitaDialog(citaList.get(position));
    }

    private static class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.ViewHolder> {
        private List<Cita> list;
        private OnActionClickListener cancel, edit, attend;

        public interface OnActionClickListener { void onAction(int position); }

        public CitaAdapter(List<Cita> list, OnActionClickListener cancel, OnActionClickListener edit, OnActionClickListener attend) {
            this.list = list; this.cancel = cancel; this.edit = edit; this.attend = attend;
        }

        public void updateData(List<Cita> newList) { this.list = newList; notifyDataSetChanged(); }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cita, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            Cita c = list.get(pos);
            h.tvSpecialty.setText(c.getEspecialidad());
            h.tvDoctor.setText("Dr. " + c.getDoctor());
            h.tvDateTime.setText(c.getFecha() + " - " + c.getHora());
            h.tvLocation.setText(c.getLugar());

            h.btnCancel.setOnClickListener(v -> cancel.onAction(pos));
            h.btnEdit.setText("Editar");
            h.btnEdit.setOnClickListener(v -> edit.onAction(pos));
            h.btnAttend.setOnClickListener(v -> attend.onAction(pos));
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSpecialty, tvDoctor, tvDateTime, tvLocation;
            Button btnCancel, btnEdit, btnAttend;
            public ViewHolder(@NonNull View v) {
                super(v);
                tvSpecialty = v.findViewById(R.id.tvCitaSpecialty);
                tvDoctor = v.findViewById(R.id.tvCitaDoctor);
                tvDateTime = v.findViewById(R.id.tvCitaDateTime);
                tvLocation = v.findViewById(R.id.tvCitaLocation);
                btnCancel = v.findViewById(R.id.btnCancelCita);
                btnEdit = v.findViewById(R.id.btnRescheduleCita);
                btnAttend = v.findViewById(R.id.btnAttendCita);
            }
        }
    }
}
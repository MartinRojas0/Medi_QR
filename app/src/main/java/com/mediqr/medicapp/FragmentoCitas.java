package com.mediqr.medicapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
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
import com.mediqr.medicapp.datos.RepositorioMedico;
import com.mediqr.medicapp.modelos.Cita;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Fragmento de Citas: Gestión de consultas médicas programadas.
 */
public class FragmentoCitas extends Fragment {

    private RecyclerView rvCitas;
    private TextView tvVacio;
    private List<Cita> listaCitas;
    private AdaptadorCitas adaptador;
    private RepositorioMedico repositorio;

    @Override
    public View onCreateView(LayoutInflater inflador, ViewGroup contenedor, Bundle savedInstanceState) {
        View vista = inflador.inflate(R.layout.fragment_citas, contenedor, false);

        repositorio = RepositorioMedico.getInstancia(requireContext());
        rvCitas = vista.findViewById(R.id.rvAppointments);
        tvVacio = vista.findViewById(R.id.tvEmptyAppointments);
        Button btnNuevaCita = vista.findViewById(R.id.newAppointmentButton);

        refrescarDatos();

        adaptador = new AdaptadorCitas(listaCitas, this::confirmarCancelacion, this::mostrarDialogoReprogramacion, this::preguntarPorAtencion);
        rvCitas.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCitas.setAdapter(adaptador);

        btnNuevaCita.setOnClickListener(v -> mostrarDialogoNuevaCita(null));

        return vista;
    }

    private void refrescarDatos() {
        listaCitas = repositorio.getCitas(true);
        actualizarEstadoVacio();
        if (adaptador != null) adaptador.actualizarDatos(listaCitas);
    }

    private void actualizarEstadoVacio() {
        if (listaCitas == null || listaCitas.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
            rvCitas.setVisibility(View.GONE);
        } else {
            tvVacio.setVisibility(View.GONE);
            rvCitas.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Validar que la fecha no sea anterior a hoy
     */
    private boolean validarFechaNoAnterior(String fecha) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setLenient(false);
            Date fechaCita = sdf.parse(fecha);
            Date fechaHoy = new Date();

            Calendar calCita = Calendar.getInstance();
            calCita.setTime(fechaCita);
            calCita.set(Calendar.HOUR_OF_DAY, 0);
            calCita.set(Calendar.MINUTE, 0);
            calCita.set(Calendar.SECOND, 0);

            Calendar calHoy = Calendar.getInstance();
            calHoy.set(Calendar.HOUR_OF_DAY, 0);
            calHoy.set(Calendar.MINUTE, 0);
            calHoy.set(Calendar.SECOND, 0);

            return !fechaCita.before(calHoy.getTime());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validar que el texto solo contenga letras y espacios
     */
    private boolean validarSoloLetras(String texto) {
        if (texto.isEmpty()) return true;
        for (char c : texto.toCharArray()) {
            if (Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validar hora en formato HH:MM
     */
    private boolean validarHora(String hora) {
        return hora.matches("([01]?[0-9]|2[0-3]):[0-5][0-9]");
    }

    private void mostrarDialogoNuevaCita(Cita citaExistente) {
        AlertDialog.Builder constructor = new AlertDialog.Builder(requireContext());
        View vistaDialogo = getLayoutInflater().inflate(R.layout.dialog_nueva_cita, null);
        constructor.setView(vistaDialogo);

        TextInputEditText editEspecialidad = vistaDialogo.findViewById(R.id.editCitaSpecialty);
        TextInputEditText editDoctor = vistaDialogo.findViewById(R.id.editCitaDoctor);
        TextInputEditText editFecha = vistaDialogo.findViewById(R.id.editCitaDate);
        TextInputEditText editHora = vistaDialogo.findViewById(R.id.editCitaTime);
        TextInputEditText editLugar = vistaDialogo.findViewById(R.id.editCitaLocation);
        Button btnGuardar = vistaDialogo.findViewById(R.id.btnSaveNewCita);

        // Filtro: solo letras para nombre del doctor
        InputFilter filtroSoloLetras = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (Character.isDigit(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };
        editDoctor.setFilters(new InputFilter[]{filtroSoloLetras});

        String[] especialidades = {"Cardiología", "Odontología", "Dermatología", "Medicina General", "Oftalmología", "Ginecología", "Pediatría", "Traumatología", "Otros"};
        editEspecialidad.setFocusable(false);
        editEspecialidad.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext()).setTitle("Seleccione Especialidad").setItems(especialidades, (dialogo, cual) -> editEspecialidad.setText(especialidades[cual])).show();
        });

        if (citaExistente != null) {
            editEspecialidad.setText(citaExistente.getEspecialidad());
            editDoctor.setText(citaExistente.getDoctor());
            editFecha.setText(citaExistente.getFecha());
            editHora.setText(citaExistente.getHora());
            editLugar.setText(citaExistente.getLugar());
            btnGuardar.setText("ACTUALIZAR");
        }

        editFecha.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(getContext(), (view, anio, mes, dia) -> {
                String fechaSel = String.format(Locale.getDefault(), "%04d-%02d-%02d", anio, mes + 1, dia);
                editFecha.setText(fechaSel);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        editHora.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(getContext(), (view, hora, min) -> {
                String horaSel = String.format(Locale.getDefault(), "%02d:%02d", hora, min);
                editHora.setText(horaSel);
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        AlertDialog dialogo = constructor.create();

        btnGuardar.setOnClickListener(v -> {
            String especialidad = editEspecialidad.getText().toString().trim();
            String doctor = editDoctor.getText().toString().trim();
            String fecha = editFecha.getText().toString().trim();
            String hora = editHora.getText().toString().trim();
            String lugar = editLugar.getText().toString().trim();

            // Validar campos obligatorios
            if (especialidad.isEmpty()) {
                Toast.makeText(getContext(), "Seleccione una especialidad", Toast.LENGTH_SHORT).show();
                return;
            }
            if (doctor.isEmpty()) {
                editDoctor.setError("Ingrese el nombre del médico");
                editDoctor.requestFocus();
                return;
            }
            if (!validarSoloLetras(doctor)) {
                editDoctor.setError("El nombre del médico no puede contener números");
                editDoctor.requestFocus();
                return;
            }
            if (fecha.isEmpty()) {
                editFecha.setError("Seleccione una fecha");
                editFecha.requestFocus();
                return;
            }
            if (!validarFechaNoAnterior(fecha)) {
                editFecha.setError("La fecha no puede ser anterior a hoy");
                editFecha.requestFocus();
                return;
            }
            if (hora.isEmpty()) {
                editHora.setError("Seleccione una hora");
                editHora.requestFocus();
                return;
            }
            if (!validarHora(hora)) {
                editHora.setError("Formato de hora inválido");
                editHora.requestFocus();
                return;
            }

            Cita nuevaCita = new Cita(citaExistente != null ? citaExistente.getId() : UUID.randomUUID().toString(), especialidad, doctor, fecha, hora, lugar, false);
            repositorio.guardarCita(nuevaCita);
            refrescarDatos();
            Toast.makeText(getContext(), "Cita guardada correctamente", Toast.LENGTH_SHORT).show();
            dialogo.dismiss();
        });

        vistaDialogo.findViewById(R.id.btnCancelNewCita).setOnClickListener(v -> dialogo.dismiss());
        dialogo.show();
    }

    private void preguntarPorAtencion(int posicion) {
        if (listaCitas == null || posicion >= listaCitas.size()) return;
        Cita cita = listaCitas.get(posicion);
        new AlertDialog.Builder(requireContext())
                .setTitle("Finalizar Cita")
                .setMessage("¿Deseas marcar esta cita como ATENDIDA y moverla al Historial Médico?")
                .setPositiveButton("Sí, Mover", (dialogo, cual) -> {
                    cita.setAtendida(true);
                    repositorio.guardarCita(cita);
                    refrescarDatos();
                    Toast.makeText(getContext(), "Cita movida al historial", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void confirmarCancelacion(int posicion) {
        if (listaCitas == null || posicion >= listaCitas.size()) return;
        Cita cita = listaCitas.get(posicion);
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancelar Cita")
                .setMessage("¿Estás seguro de que deseas cancelar esta cita?")
                .setPositiveButton("Sí, Cancelar", (dialogo, cual) -> {
                    repositorio.eliminarCita(cita.getId());
                    refrescarDatos();
                    Toast.makeText(getContext(), "Cita cancelada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void mostrarDialogoReprogramacion(int posicion) {
        if (listaCitas != null && posicion < listaCitas.size()) {
            mostrarDialogoNuevaCita(listaCitas.get(posicion));
        }
    }

    private static class AdaptadorCitas extends RecyclerView.Adapter<AdaptadorCitas.ViewHolder> {
        private List<Cita> lista;
        private final OnClickAccion cancelador, editor, atendedor;

        public interface OnClickAccion { void onAccion(int posicion); }

        public AdaptadorCitas(List<Cita> lista, OnClickAccion cancelador, OnClickAccion editor, OnClickAccion atendedor) {
            this.lista = lista;
            this.cancelador = cancelador;
            this.editor = editor;
            this.atendedor = atendedor;
        }

        public void actualizarDatos(List<Cita> nuevaLista) {
            this.lista = nuevaLista;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
            return new ViewHolder(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_cita, padre, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            if (lista == null || pos >= lista.size()) return;
            Cita c = lista.get(pos);
            h.tvEspecialidad.setText(c.getEspecialidad());
            h.tvDoctor.setText("Dr. " + c.getDoctor());
            h.tvFechaHora.setText(c.getFecha() + " - " + c.getHora());
            h.tvLugar.setText(c.getLugar());

            h.btnCancelar.setOnClickListener(v -> cancelador.onAccion(pos));
            h.btnEditar.setOnClickListener(v -> editor.onAccion(pos));
            h.btnAtender.setOnClickListener(v -> atendedor.onAccion(pos));
        }

        @Override
        public int getItemCount() {
            return lista == null ? 0 : lista.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEspecialidad, tvDoctor, tvFechaHora, tvLugar;
            Button btnCancelar, btnEditar, btnAtender;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvEspecialidad = v.findViewById(R.id.tvCitaSpecialty);
                tvDoctor = v.findViewById(R.id.tvCitaDoctor);
                tvFechaHora = v.findViewById(R.id.tvCitaDateTime);
                tvLugar = v.findViewById(R.id.tvCitaLocation);
                btnCancelar = v.findViewById(R.id.btnCancelCita);
                btnEditar = v.findViewById(R.id.btnRescheduleCita);
                btnAtender = v.findViewById(R.id.btnAttendCita);
            }
        }
    }
}
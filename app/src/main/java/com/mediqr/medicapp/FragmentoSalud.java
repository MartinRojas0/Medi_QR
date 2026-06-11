package com.mediqr.medicapp;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.mediqr.medicapp.datos.RepositorioMedico;
import com.mediqr.medicapp.modelos.PerfilPaciente;

/**
 * Fragmento de Salud: Expediente clínico digital completo.
 */
public class FragmentoSalud extends Fragment {

    private RepositorioMedico repositorio;
    private View filaSangre, filaAlergias, filaMeds, filaCronicas, filaVacunas, filaCirugias;
    private MaterialCardView tarjetaAlerta;
    private TextView tvTextoAlerta;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, ViewGroup contenedor,
                             Bundle savedInstanceState) {
        return inflador.inflate(R.layout.fragment_salud, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        try {
            repositorio = RepositorioMedico.getInstancia(requireContext());

            tarjetaAlerta = vista.findViewById(R.id.cardCriticalAlert);
            tvTextoAlerta = vista.findViewById(R.id.tvCriticalAllergies);

            filaSangre = vista.findViewById(R.id.rowBlood);
            filaAlergias = vista.findViewById(R.id.rowAllergies);
            filaMeds = vista.findViewById(R.id.rowMeds);
            filaCronicas = vista.findViewById(R.id.rowChronic);
            filaVacunas = vista.findViewById(R.id.rowVaccines);
            filaCirugias = vista.findViewById(R.id.rowSurgeries);

            inicializarInterfaz();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error al cargar el expediente clínico", Toast.LENGTH_SHORT).show();
        }
    }

    private void inicializarInterfaz() {
        configurarFila(filaSangre, "Grupo Sanguíneo", R.drawable.ic_heart, v -> mostrarDialogoSangre());
        configurarFila(filaAlergias, "Alergias", R.drawable.ic_health, v -> mostrarDialogoEdicion("Alergias", "alergias"));
        configurarFila(filaMeds, "Medicamentos", R.drawable.ic_medication, v -> mostrarDialogoEdicion("Medicamentos", "meds"));
        configurarFila(filaCronicas, "Condiciones Crónicas", R.drawable.ic_history, v -> mostrarDialogoEdicion("Condiciones", "cronicas"));
        configurarFila(filaVacunas, "Vacunas", R.drawable.ic_appointments, v -> mostrarDialogoEdicion("Vacunas", "vacunas"));
        configurarFila(filaCirugias, "Cirugías", R.drawable.ic_documents, v -> mostrarDialogoEdicion("Cirugías", "cirugias"));

        cargarDatos();
    }

    private void configurarFila(View fila, String etiqueta, int iconoRes, View.OnClickListener escuchador) {
        if (fila != null) {
            TextView tvEtiqueta = fila.findViewById(R.id.rowLabel);
            ImageView ivIcono = fila.findViewById(R.id.rowIcon);
            if (tvEtiqueta != null) tvEtiqueta.setText(etiqueta);
            if (ivIcono != null) ivIcono.setImageResource(iconoRes);
            fila.setOnClickListener(escuchador);
        }
    }

    private void cargarDatos() {
        if (!isAdded() || repositorio == null) return;

        PerfilPaciente perfil = repositorio.getPerfil();
        if (perfil == null) return;

        actualizarValorFila(filaSangre, perfil.getTipoSangre());
        actualizarValorFila(filaAlergias, perfil.getAlergias());
        actualizarValorFila(filaMeds, perfil.getMedicamentosActuales());
        actualizarValorFila(filaCronicas, perfil.getCondicionesCronicas());
        actualizarValorFila(filaVacunas, perfil.getVacunas());
        actualizarValorFila(filaCirugias, perfil.getCirugias());

        if (tarjetaAlerta != null) {
            String alergias = perfil.getAlergias().toLowerCase();
            if (alergias.contains("grave") || alergias.contains("penicilina") || alergias.contains("anafil")) {
                tarjetaAlerta.setVisibility(View.VISIBLE);
                if (tvTextoAlerta != null) tvTextoAlerta.setText("RIESGO MÉDICO: " + perfil.getAlergias());
            } else {
                tarjetaAlerta.setVisibility(View.GONE);
            }
        }
    }

    private void actualizarValorFila(View fila, String valor) {
        if (fila != null) {
            TextView tvValor = fila.findViewById(R.id.rowValue);
            if (tvValor != null) {
                if (valor != null && !valor.isEmpty()) {
                    tvValor.setText(valor);
                } else {
                    tvValor.setText("No especificado");
                }
            }
        }
    }

    private void mostrarDialogoEdicion(String titulo, String tipoCampo) {
        if (!isAdded()) return;
        AlertDialog.Builder constructor = new AlertDialog.Builder(requireContext());
        constructor.setTitle("Actualizar " + titulo);

        View vista = getLayoutInflater().inflate(R.layout.dialog_edit_simple, null);
        final TextInputEditText entrada = vista.findViewById(R.id.editSimpleValue);

        PerfilPaciente p = repositorio.getPerfil();
        String valorActual = "";
        if (tipoCampo.equals("alergias")) valorActual = p.getAlergias();
        else if (tipoCampo.equals("cronicas")) valorActual = p.getCondicionesCronicas();
        else if (tipoCampo.equals("meds")) valorActual = p.getMedicamentosActuales();
        else if (tipoCampo.equals("vacunas")) valorActual = p.getVacunas();
        else if (tipoCampo.equals("cirugias")) valorActual = p.getCirugias();
        entrada.setText(valorActual);

        // Para alergias: filtrar números
        if (tipoCampo.equals("alergias")) {
            InputFilter filtroSinNumeros = new InputFilter() {
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
            entrada.setFilters(new InputFilter[]{filtroSinNumeros});
        }

        constructor.setView(vista);
        constructor.setPositiveButton("Guardar", (dialogo, cual) -> {
            String val = entrada.getText().toString().trim();
            confirmarGuardado(tipoCampo, val);
        });
        constructor.setNegativeButton("Cerrar", null);
        constructor.show();
    }

    private void mostrarDialogoSangre() {
        if (!isAdded()) return;
        String[] tipos = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar Tipo de Sangre")
                .setItems(tipos, (dialogo, cual) -> confirmarGuardado("sangre", tipos[cual]))
                .show();
    }

    private void confirmarGuardado(String campo, String valor) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Cambios")
                .setMessage("¿Estás seguro de actualizar tu información médica?")
                .setPositiveButton("Confirmar", (dialogo, cual) -> {
                    PerfilPaciente p = repositorio.getPerfil();
                    if (campo.equals("alergias")) p.setAlergias(valor);
                    else if (campo.equals("sangre")) p.setTipoSangre(valor);
                    else if (campo.equals("cronicas")) p.setCondicionesCronicas(valor);
                    else if (campo.equals("meds")) p.setMedicamentosActuales(valor);
                    else if (campo.equals("vacunas")) p.setVacunas(valor);
                    else if (campo.equals("cirugias")) p.setCirugias(valor);

                    repositorio.guardarDatosSalud(p.getTipoSangre(), p.getAlergias(), p.getCondicionesCronicas(),
                            p.getMedicamentosActuales(), p.getVacunas(), p.getCirugias());
                    cargarDatos();
                    Toast.makeText(getContext(), "Información médica actualizada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
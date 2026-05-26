package com.mediqr.android.modelos;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Modelo que representa una cita médica.
 */
public class Cita implements Serializable {
    private String id;
    private String especialidad;
    private String doctor;
    private String fecha; // yyyy-MM-dd
    private String hora;
    private String lugar;
    private boolean atendida;
    
    // Campos médicos para el historial
    private String diagnostico;
    private String tratamiento;
    private String observaciones;

    public Cita(String id, String especialidad, String doctor, String fecha, String hora, String lugar, boolean atendida) {
        this.id = id;
        this.especialidad = especialidad;
        this.doctor = doctor;
        this.fecha = fecha;
        this.hora = hora;
        this.lugar = lugar;
        this.atendida = atendida;
        
        // Contenido médico por defecto para simulación
        this.diagnostico = "Consulta de control rutinario. Paciente presenta estado general estable.";
        this.tratamiento = "Continuar con medicación habitual. Dieta baja en sodio.";
        this.observaciones = "Revisión en 6 meses.";
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public String getDoctor() { return doctor; }
    public void setDoctor(String doctor) { this.doctor = doctor; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public boolean isAtendida() { return atendida; }
    public void setAtendida(boolean atendida) { this.atendida = atendida; }
    
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public String getTratamiento() { return tratamiento; }
    public void setTratamiento(String tratamiento) { this.tratamiento = tratamiento; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    @Override
    public String toString() {
        return id + "|" + especialidad + "|" + doctor + "|" + fecha + "|" + hora + "|" + lugar + "|" + atendida + "|" + diagnostico + "|" + tratamiento + "|" + observaciones;
    }

    public static Cita fromString(String data) {
        String[] parts = data.split("\\|", -1);
        if (parts.length >= 7) {
            boolean atendida = Boolean.parseBoolean(parts[6]);
            Cita c = new Cita(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], atendida);
            if (parts.length > 7) c.setDiagnostico(parts[7]);
            if (parts.length > 8) c.setTratamiento(parts[8]);
            if (parts.length > 9) c.setObservaciones(parts[9]);
            return c;
        }
        return null;
    }

    /**
     * Comparador para ordenar citas por fecha y hora.
     */
    public static Comparator<Cita> ComparadorFecha = (c1, c2) -> {
        int dateCompare = c1.getFecha().compareTo(c2.getFecha());
        if (dateCompare == 0) {
            return c1.getHora().compareTo(c2.getHora());
        }
        return dateCompare;
    };
}
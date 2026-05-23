package com.mediqr.android.models;

import java.io.Serializable;
import java.util.Comparator;

public class Cita implements Serializable {
    private String id;
    private String especialidad;
    private String doctor;
    private String fecha; // yyyy-MM-dd
    private String hora;
    private String lugar;
    private boolean atendida;

    public Cita(String id, String especialidad, String doctor, String fecha, String hora, String lugar, boolean atendida) {
        this.id = id;
        this.especialidad = especialidad;
        this.doctor = doctor;
        this.fecha = fecha;
        this.hora = hora;
        this.lugar = lugar;
        this.atendida = atendida;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getEspecialidad() { return especialidad; }
    public String getDoctor() { return doctor; }
    public String getFecha() { return fecha; }
    public String getHora() { return hora; }
    public String getLugar() { return lugar; }
    public boolean isAtendida() { return atendida; }
    public void setAtendida(boolean atendida) { this.atendida = atendida; }

    @Override
    public String toString() {
        return id + "|" + especialidad + "|" + doctor + "|" + fecha + "|" + hora + "|" + lugar + "|" + atendida;
    }

    public static Cita fromString(String data) {
        String[] parts = data.split("\\|", -1);
        if (parts.length >= 6) {
            boolean atendida = parts.length > 6 && Boolean.parseBoolean(parts[6]);
            return new Cita(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], atendida);
        }
        return null;
    }

    public static Comparator<Cita> DateComparator = (c1, c2) -> {
        int dateCompare = c1.getFecha().compareTo(c2.getFecha());
        if (dateCompare == 0) {
            return c1.getHora().compareTo(c2.getHora());
        }
        return dateCompare;
    };
}
package com.mediqr.android.models;

import java.io.Serializable;

public class Documento implements Serializable {
    private String id;
    private String nombre;
    private String fecha;
    private String ruta;
    private String especialidad;
    private String doctor;
    private String lugar;
    private String tipo;

    public Documento(String id, String nombre, String fecha, String ruta, String especialidad, String doctor, String lugar, String tipo) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.ruta = ruta;
        this.especialidad = especialidad;
        this.doctor = doctor;
        this.lugar = lugar;
        this.tipo = tipo;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getFecha() { return fecha; }
    public String getRuta() { return ruta; }
    public String getEspecialidad() { return especialidad; }
    public String getDoctor() { return doctor; }
    public String getLugar() { return lugar; }
    public String getTipo() { return tipo; }

    @Override
    public String toString() {
        return id + "|" + nombre + "|" + fecha + "|" + ruta + "|" + especialidad + "|" + doctor + "|" + lugar + "|" + tipo;
    }

    public static Documento fromString(String data) {
        String[] parts = data.split("\\|", -1);
        if (parts.length >= 4) {
            String id = parts[0];
            String nombre = parts[1];
            String fecha = parts[2];
            String ruta = parts[3];
            String especialidad = parts.length > 4 ? parts[4] : "General";
            String doctor = parts.length > 5 ? parts[5] : "No especificado";
            String lugar = parts.length > 6 ? parts[6] : "No especificado";
            String tipo = parts.length > 7 ? parts[7] : "Otro";
            return new Documento(id, nombre, fecha, ruta, especialidad, doctor, lugar, tipo);
        }
        return null;
    }
}
package com.mediqr.medicapp.modelos;

import java.io.Serializable;

/**
 * Modelo que representa un documento médico (PDF, Imagen, etc.).
 */
public class Documento implements Serializable {
    private String id;
    private String nombre;
    private String fecha;
    private String ruta;
    private String especialidad;
    private String doctor;
    private String lugar;
    private String tipo;
    private String idCitaVinculada; // Relación directa con una consulta

    public Documento(String id, String nombre, String fecha, String ruta, String especialidad, String doctor, String lugar, String tipo) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.ruta = ruta;
        this.especialidad = especialidad;
        this.doctor = doctor;
        this.lugar = lugar;
        this.tipo = tipo;
        this.idCitaVinculada = "";
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getFecha() { return fecha; }
    public String getRuta() { return ruta; }
    public String getEspecialidad() { return especialidad; }
    public String getDoctor() { return doctor; }
    public String getLugar() { return lugar; }
    public String getTipo() { return tipo; }
    public String getIdCitaVinculada() { return idCitaVinculada; }
    public void setIdCitaVinculada(String idCitaVinculada) { this.idCitaVinculada = idCitaVinculada; }

    @Override
    public String toString() {
        return id + "|" + nombre + "|" + fecha + "|" + ruta + "|" + especialidad + "|" + doctor + "|" + lugar + "|" + tipo + "|" + idCitaVinculada;
    }

    public static Documento fromString(String data) {
        String[] parts = data.split("\\|", -1);
        if (parts.length >= 8) {
            Documento d = new Documento(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]);
            if (parts.length > 8) d.setIdCitaVinculada(parts[8]);
            return d;
        }
        return null;
    }
}
package com.mediqr.medicapp.modelos;

import java.io.Serializable;

/**
 * Modelo que representa el perfil completo del paciente.
 */
public class PerfilPaciente implements Serializable {
    private String nombre;
    private String apellido;
    private String dni;
    private int edad;
    private String correo;
    private String telefono;
    private String calle;
    private String distrito;
    private String numeroCasa;
    
    // Datos de Salud
    private String tipoSangre;
    private String alergias;
    private String condicionesCronicas;
    private String medicamentosActuales;
    private String vacunas;
    private String cirugias;

    public PerfilPaciente() {
        this.nombre = "";
        this.apellido = "";
        this.dni = "";
        this.edad = 0;
        this.correo = "";
        this.telefono = "";
        this.calle = "";
        this.distrito = "";
        this.numeroCasa = "";
        this.tipoSangre = "";
        this.alergias = "";
        this.condicionesCronicas = "";
        this.medicamentosActuales = "";
        this.vacunas = "";
        this.cirugias = "";
    }

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getNombreCompleto() { return nombre + " " + apellido; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }

    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }

    public String getNumeroCasa() { return numeroCasa; }
    public void setNumeroCasa(String numeroCasa) { this.numeroCasa = numeroCasa; }

    public String getDireccionCompleta() { return calle + " " + numeroCasa + ", " + distrito; }

    public String getTipoSangre() { return tipoSangre; }
    public void setTipoSangre(String tipoSangre) { this.tipoSangre = tipoSangre; }

    public String getAlergias() { return alergias; }
    public void setAlergias(String alergias) { this.alergias = alergias; }

    public String getCondicionesCronicas() { return condicionesCronicas; }
    public void setCondicionesCronicas(String condicionesCronicas) { this.condicionesCronicas = condicionesCronicas; }

    public String getMedicamentosActuales() { return medicamentosActuales; }
    public void setMedicamentosActuales(String medicamentosActuales) { this.medicamentosActuales = medicamentosActuales; }

    public String getVacunas() { return vacunas; }
    public void setVacunas(String vacunas) { this.vacunas = vacunas; }

    public String getCirugias() { return cirugias; }
    public void setCirugias(String cirugias) { this.cirugias = cirugias; }
}
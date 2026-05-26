package com.mediqr.android.datos;

import android.content.Context;
import android.content.SharedPreferences;
import com.mediqr.android.modelos.Cita;
import com.mediqr.android.modelos.Documento;
import com.mediqr.android.modelos.PerfilPaciente;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Repositorio Médico: El cerebro de los datos de la aplicación.
 * Gestiona de forma centralizada las preferencias compartidas.
 */
public class RepositorioMedico {
    private static RepositorioMedico instancia;
    private final SharedPreferences prefUsuario;
    private final SharedPreferences prefSalud;
    private final SharedPreferences prefDocs;
    private final SharedPreferences prefCitas;

    private RepositorioMedico(Context contexto) {
        prefUsuario = contexto.getSharedPreferences("MediQR_Prefs", Context.MODE_PRIVATE);
        prefSalud = contexto.getSharedPreferences("MediQR_Salud", Context.MODE_PRIVATE);
        prefDocs = contexto.getSharedPreferences("MediQR_Docs", Context.MODE_PRIVATE);
        prefCitas = contexto.getSharedPreferences("MediQR_Citas", Context.MODE_PRIVATE);
    }

    public static synchronized RepositorioMedico getInstancia(Context contexto) {
        if (instancia == null) {
            instancia = new RepositorioMedico(contexto.getApplicationContext());
        }
        return instancia;
    }

    // --- GESTIÓN DEL PERFIL ---
    public PerfilPaciente getPerfil() {
        PerfilPaciente perfil = new PerfilPaciente();
        String nombreComp = prefUsuario.getString("user_name", "Martin Rojas");
        
        if (nombreComp != null && nombreComp.contains(" ")) {
            String[] partes = nombreComp.split(" ", 2);
            perfil.setNombre(partes[0]);
            perfil.setApellido(partes[1]);
        } else {
            perfil.setNombre(nombreComp);
            perfil.setApellido("");
        }

        perfil.setDni(prefUsuario.getString("user_dni", "12.345.678"));
        
        String edadStr = prefUsuario.getString("user_age", "20").replace(" años", "").trim();
        try {
            perfil.setEdad(Integer.parseInt(edadStr));
        } catch (NumberFormatException e) {
            perfil.setEdad(20);
        }

        perfil.setCorreo(prefUsuario.getString("user_email", "martin.rojas@email.com"));
        perfil.setTelefono(prefUsuario.getString("user_phone", "987654321"));
        perfil.setCalle(prefUsuario.getString("user_street", "Av. Principal"));
        perfil.setDistrito(prefUsuario.getString("user_district", "Lima"));
        perfil.setNumeroCasa(prefUsuario.getString("user_houseNum", "123"));
        
        perfil.setTipoSangre(prefSalud.getString("user_blood_type", "O+"));
        perfil.setAlergias(prefSalud.getString("user_allergies", "Ninguna conocida"));
        perfil.setCondicionesCronicas(prefSalud.getString("user_chronic", "Ninguna"));
        perfil.setMedicamentosActuales(prefSalud.getString("user_meds", "Ninguna"));
        perfil.setVacunas(prefSalud.getString("user_vaccines", "Esquema completo"));
        perfil.setCirugias(prefSalud.getString("user_surgeries", "Ninguna"));
        
        return perfil;
    }

    public void guardarPerfil(PerfilPaciente perfil) {
        prefUsuario.edit()
            .putString("user_name", perfil.getNombreCompleto())
            .putString("user_dni", perfil.getDni())
            .putString("user_age", perfil.getEdad() + " años")
            .putString("user_email", perfil.getCorreo())
            .putString("user_phone", perfil.getTelefono())
            .putString("user_street", perfil.getCalle())
            .putString("user_district", perfil.getDistrito())
            .putString("user_houseNum", perfil.getNumeroCasa())
            .apply();
    }

    public void guardarDatosSalud(String sangre, String alergias, String cronicas, String meds, String vacunas, String cirugias) {
        prefSalud.edit()
            .putString("user_blood_type", sangre)
            .putString("user_allergies", alergias)
            .putString("user_chronic", cronicas)
            .putString("user_meds", meds)
            .putString("user_vaccines", vacunas)
            .putString("user_surgeries", cirugias)
            .apply();
    }

    // --- GESTIÓN DE DOCUMENTOS ---
    public List<Documento> getDocumentos() {
        Set<String> set = prefDocs.getStringSet("docs_list", new HashSet<>());
        List<Documento> lista = new ArrayList<>();
        if (set != null) {
            for (String s : set) {
                Documento d = Documento.fromString(s);
                if (d != null) lista.add(d);
            }
        }
        Collections.sort(lista, (d1, d2) -> {
            try {
                String[] p1 = d1.getFecha().split("/");
                String[] p2 = d2.getFecha().split("/");
                String s1 = p1[2] + p1[1] + p1[0];
                String s2 = p2[2] + p2[1] + p2[0];
                return s2.compareTo(s1);
            } catch (Exception e) { return 0; }
        });
        return lista;
    }

    public void agregarDocumento(Documento doc) {
        List<Documento> lista = getDocumentos();
        lista.add(0, doc);
        guardarDocumentos(lista);
    }

    public void eliminarDocumento(Documento doc) {
        List<Documento> lista = getDocumentos();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equals(doc.getId())) {
                lista.remove(i);
                break;
            }
        }
        guardarDocumentos(lista);
    }

    private void guardarDocumentos(List<Documento> lista) {
        Set<String> set = new HashSet<>();
        for (Documento d : lista) set.add(d.toString());
        prefDocs.edit().putStringSet("docs_list", set).apply();
    }

    // --- GESTIÓN DE CITAS ---
    public List<Cita> getCitas(boolean soloPendientes) {
        Set<String> set = prefCitas.getStringSet("citas_set", new HashSet<>());
        List<Cita> lista = new ArrayList<>();
        if (set != null) {
            for (String s : set) {
                Cita c = Cita.fromString(s);
                if (c != null) {
                    if (soloPendientes && !c.isAtendida()) lista.add(c);
                    else if (!soloPendientes) lista.add(c);
                }
            }
        }
        Collections.sort(lista, Cita.ComparadorFecha);
        return lista;
    }

    public void guardarCita(Cita cita) {
        Set<String> set = prefCitas.getStringSet("citas_set", new HashSet<>());
        Set<String> nuevoSet = new HashSet<>();
        boolean encontrada = false;
        if (set != null) {
            for (String s : set) {
                Cita existente = Cita.fromString(s);
                if (existente != null && existente.getId().equals(cita.getId())) {
                    nuevoSet.add(cita.toString());
                    encontrada = true;
                } else {
                    nuevoSet.add(s);
                }
            }
        }
        if (!encontrada) nuevoSet.add(cita.toString());
        prefCitas.edit().putStringSet("citas_set", nuevoSet).apply();
    }

    public void eliminarCita(String id) {
        Set<String> set = prefCitas.getStringSet("citas_set", new HashSet<>());
        Set<String> nuevoSet = new HashSet<>();
        if (set != null) {
            for (String s : set) {
                Cita existente = Cita.fromString(s);
                if (existente != null && !existente.getId().equals(id)) {
                    nuevoSet.add(s);
                }
            }
        }
        prefCitas.edit().putStringSet("citas_set", nuevoSet).apply();
    }
}
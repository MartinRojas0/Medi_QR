package com.mediqr.medicapp.datos;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mediqr.medicapp.modelos.Cita;
import com.mediqr.medicapp.modelos.Documento;
import com.mediqr.medicapp.modelos.PerfilPaciente;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    // Firebase
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String currentUserId;

    private RepositorioMedico(Context contexto) {
        prefUsuario = contexto.getSharedPreferences("MediQR_Prefs", Context.MODE_PRIVATE);
        prefSalud = contexto.getSharedPreferences("MediQR_Salud", Context.MODE_PRIVATE);
        prefDocs = contexto.getSharedPreferences("MediQR_Docs", Context.MODE_PRIVATE);
        prefCitas = contexto.getSharedPreferences("MediQR_Citas", Context.MODE_PRIVATE);

        // Inicializar Firestore
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
    }

    public static synchronized RepositorioMedico getInstancia(Context contexto) {
        if (instancia == null) {
            instancia = new RepositorioMedico(contexto.getApplicationContext());
        }
        return instancia;
    }

    // Método para establecer el usuario actual
    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    // Método para cargar perfil desde Firestore
    public void cargarPerfilDesdeFirestore(OnPerfilCargadoListener listener) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            listener.onError("Usuario no autenticado");
            return;
        }

        firestore.collection("usuarios")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Descarga de datos demográficos básicos
                        String nombre = document.getString("nombre");
                        String email = document.getString("email");
                        if (nombre != null) prefUsuario.edit().putString("user_name", nombre).apply();
                        if (email != null) prefUsuario.edit().putString("user_email", email).apply();
                        if (document.contains("dni")) prefUsuario.edit().putString("user_dni", document.getString("dni")).apply();
                        if (document.contains("telefono")) prefUsuario.edit().putString("user_phone", document.getString("telefono")).apply();

                        if (document.contains("edad")) {
                            Object edadObj = document.get("edad");
                            if (edadObj != null) {
                                String edadStr = edadObj.toString();
                                if (!edadStr.contains(" años")) edadStr += " años";
                                prefUsuario.edit().putString("user_age", edadStr).apply();
                            }
                        }

                        // ⚡ NUEVO: Descarga y sincronización de datos médicos / de salud
                        if (document.contains("tipoSangre")) prefSalud.edit().putString("user_blood_type", document.getString("tipoSangre")).apply();
                        if (document.contains("alergias")) prefSalud.edit().putString("user_allergies", document.getString("alergias")).apply();
                        if (document.contains("condicionesCronicas")) prefSalud.edit().putString("user_chronic", document.getString("condicionesCronicas")).apply();
                        if (document.contains("medicamentos")) prefSalud.edit().putString("user_meds", document.getString("medicamentos")).apply();
                        if (document.contains("vacunas")) prefSalud.edit().putString("user_vaccines", document.getString("vacunas")).apply();
                        if (document.contains("cirugias")) prefSalud.edit().putString("user_surgeries", document.getString("cirugias")).apply();

                        listener.onSuccess();
                    } else {
                        listener.onError("No se encontraron datos en el servidor");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // Método para guardar perfil en Firestore
    public void guardarPerfilEnFirestore(PerfilPaciente perfil, OnPerfilGuardadoListener listener) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            listener.onError("Usuario no autenticado");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", perfil.getNombreCompleto());
        userData.put("email", perfil.getCorreo());
        userData.put("dni", perfil.getDni());
        userData.put("telefono", perfil.getTelefono());
        userData.put("edad", perfil.getEdad());
        userData.put("direccion", perfil.getDireccionCompleta());
        userData.put("tipoSangre", perfil.getTipoSangre());
        userData.put("alergias", perfil.getAlergias());
        userData.put("condicionesCronicas", perfil.getCondicionesCronicas());
        userData.put("medicamentos", perfil.getMedicamentosActuales());

        firestore.collection("usuarios")
                .document(currentUserId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                });
    }

    // --- GESTIÓN DEL PERFIL (LOCAL) ---
    public PerfilPaciente getPerfil() {
        PerfilPaciente perfil = new PerfilPaciente();

        String nombreComp = prefUsuario.getString("user_name", "");

        if (nombreComp != null && !nombreComp.isEmpty() && nombreComp.contains(" ")) {
            String[] partes = nombreComp.split(" ", 2);
            perfil.setNombre(partes[0]);
            perfil.setApellido(partes[1]);
        } else if (nombreComp != null && !nombreComp.isEmpty()) {
            perfil.setNombre(nombreComp);
            perfil.setApellido("");
        } else {
            perfil.setNombre("");
            perfil.setApellido("");
        }

        // Valores vacíos en lugar de datos ficticios
        perfil.setDni(prefUsuario.getString("user_dni", ""));

        String edadStr = prefUsuario.getString("user_age", "").replace(" años", "").trim();
        if (!edadStr.isEmpty()) {
            try {
                perfil.setEdad(Integer.parseInt(edadStr));
            } catch (NumberFormatException e) {
                perfil.setEdad(0);
            }
        } else {
            perfil.setEdad(0);
        }

        perfil.setCorreo(prefUsuario.getString("user_email", ""));
        perfil.setTelefono(prefUsuario.getString("user_phone", ""));
        perfil.setCalle(prefUsuario.getString("user_street", ""));
        perfil.setDistrito(prefUsuario.getString("user_district", ""));
        perfil.setNumeroCasa(prefUsuario.getString("user_houseNum", ""));

        // Datos de salud vacíos por defecto
        perfil.setTipoSangre(prefSalud.getString("user_blood_type", ""));
        perfil.setAlergias(prefSalud.getString("user_allergies", ""));
        perfil.setCondicionesCronicas(prefSalud.getString("user_chronic", ""));
        perfil.setMedicamentosActuales(prefSalud.getString("user_meds", ""));
        perfil.setVacunas(prefSalud.getString("user_vaccines", ""));
        perfil.setCirugias(prefSalud.getString("user_surgeries", ""));

        return perfil;
    }

    public void guardarPerfil(PerfilPaciente perfil) {
        // 1. Persistencia Inmediata en Caché Local (SharedPreferences)
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

        // 2. Réplica Automática y Asíncrona en la Nube (Firestore)
        if (currentUserId != null && !currentUserId.isEmpty()) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("nombre", perfil.getNombreCompleto());
            userData.put("email", perfil.getCorreo());
            userData.put("dni", perfil.getDni());
            userData.put("telefono", perfil.getTelefono());
            userData.put("edad", perfil.getEdad());
            userData.put("direccion", perfil.getDireccionCompleta());
            userData.put("tipoSangre", perfil.getTipoSangre());
            userData.put("alergias", perfil.getAlergias());
            userData.put("condicionesCronicas", perfil.getCondicionesCronicas());
            userData.put("medicamentos", perfil.getMedicamentosActuales());

            firestore.collection("usuarios")
                    .document(currentUserId)
                    .set(userData, com.google.firebase.firestore.SetOptions.merge()) // Evita sobreescribir otros campos como fechaRegistro
                    .addOnSuccessListener(aVoid -> Log.d("MediQR_Sync", "Sincronización en la nube exitosa."))
                    .addOnFailureListener(e -> Log.e("MediQR_Sync", "Error de réplica remota: " + e.getMessage()));
        }
    }

    public void guardarDatosSalud(String sangre, String alergias, String cronicas, String meds, String vacunas, String cirugias) {
        // Guardado local
        prefSalud.edit()
                .putString("user_blood_type", sangre)
                .putString("user_allergies", alergias)
                .putString("user_chronic", cronicas)
                .putString("user_meds", meds)
                .putString("user_vaccines", vacunas)
                .putString("user_surgeries", cirugias)
                .apply();

        // Sincronización incremental en Firestore
        if (currentUserId != null && !currentUserId.isEmpty()) {
            Map<String, Object> healthData = new HashMap<>();
            healthData.put("tipoSangre", sangre);
            healthData.put("alergias", alergias);
            healthData.put("condicionesCronicas", cronicas);
            healthData.put("medicamentos", meds);

            firestore.collection("usuarios")
                    .document(currentUserId)
                    .set(healthData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("MediQR_Sync", "Datos de salud sincronizados."))
                    .addOnFailureListener(e -> Log.e("MediQR_Sync", "Error en datos de salud: " + e.getMessage()));
        }
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

    // --- INTERFACES PARA CALLBACKS ---
    public interface OnPerfilCargadoListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnPerfilGuardadoListener {
        void onSuccess();
        void onError(String error);
    }
}
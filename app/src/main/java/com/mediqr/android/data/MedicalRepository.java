package com.mediqr.android.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.mediqr.android.models.Cita;
import com.mediqr.android.models.Documento;
import com.mediqr.android.models.PatientProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MedicalRepository: Sube de nivel la gestión de datos.
 * Actualmente usa SharedPreferences para simular persistencia,
 * pero está diseñada para integrarse con Retrofit/Firebase fácilmente.
 */
public class MedicalRepository {
    private static MedicalRepository instance;
    private final SharedPreferences userPrefs;
    private final SharedPreferences saludPrefs;
    private final SharedPreferences docsPrefs;
    private final SharedPreferences citasPrefs;

    private MedicalRepository(Context context) {
        userPrefs = context.getSharedPreferences("MediQR_Prefs", Context.MODE_PRIVATE);
        saludPrefs = context.getSharedPreferences("MediQR_Salud", Context.MODE_PRIVATE);
        docsPrefs = context.getSharedPreferences("MediQR_Docs", Context.MODE_PRIVATE);
        citasPrefs = context.getSharedPreferences("MediQR_Citas", Context.MODE_PRIVATE);
    }

    public static synchronized MedicalRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MedicalRepository(context.getApplicationContext());
        }
        return instance;
    }

    // --- PATIENT PROFILE ---
    public PatientProfile getProfile() {
        PatientProfile profile = new PatientProfile();
        profile.setFirstName(userPrefs.getString("user_name", "Martin Rojas").split(" ")[0]);
        profile.setLastName(userPrefs.getString("user_name", "Martin Rojas").contains(" ") ? 
                userPrefs.getString("user_name", "Martin Rojas").split(" ", 2)[1] : "");
        profile.setDni(userPrefs.getString("user_dni", "12.345.678"));
        profile.setAge(Integer.parseInt(userPrefs.getString("user_age", "20").replace(" años", "")));
        profile.setEmail(userPrefs.getString("user_email", "martin.rojas@email.com"));
        profile.setPhone(userPrefs.getString("user_phone", "987654321"));
        profile.setStreet(userPrefs.getString("user_street", "Av. Principal"));
        profile.setDistrict(userPrefs.getString("user_district", "Lima"));
        profile.setHouseNum(userPrefs.getString("user_houseNum", "123"));
        
        // Health
        profile.setBloodType(saludPrefs.getString("user_blood_type", "O+"));
        profile.setAllergies(saludPrefs.getString("user_allergies", "Ninguna conocida"));
        profile.setChronicConditions(saludPrefs.getString("user_chronic", "Ninguna"));
        profile.setCurrentMedications(saludPrefs.getString("user_meds", "Ninguna"));
        
        return profile;
    }

    public void saveProfile(PatientProfile profile) {
        userPrefs.edit()
            .putString("user_name", profile.getFullName())
            .putString("user_dni", profile.getDni())
            .putString("user_age", profile.getAge() + " años")
            .putString("user_email", profile.getEmail())
            .putString("user_phone", profile.getPhone())
            .putString("user_street", profile.getStreet())
            .putString("user_district", profile.getDistrict())
            .putString("user_houseNum", profile.getHouseNum())
            .apply();
    }

    public void saveHealthData(String blood, String allergies, String chronic, String meds) {
        saludPrefs.edit()
            .putString("user_blood_type", blood)
            .putString("user_allergies", allergies)
            .putString("user_chronic", chronic)
            .putString("user_meds", meds)
            .apply();
    }

    // --- DOCUMENTS ---
    public List<Documento> getDocuments() {
        Set<String> set = docsPrefs.getStringSet("docs_list", new HashSet<>());
        List<Documento> list = new ArrayList<>();
        for (String s : set) {
            Documento d = Documento.fromString(s);
            if (d != null) list.add(d);
        }
        Collections.sort(list, (d1, d2) -> {
            try {
                String[] p1 = d1.getFecha().split("/");
                String[] p2 = d2.getFecha().split("/");
                String s1 = p1[2] + p1[1] + p1[0];
                String s2 = p2[2] + p2[1] + p2[0];
                return s2.compareTo(s1);
            } catch (Exception e) { return 0; }
        });
        return list;
    }

    public void addDocument(Documento doc) {
        List<Documento> list = getDocuments();
        list.add(0, doc);
        saveDocuments(list);
    }

    public void deleteDocument(Documento doc) {
        List<Documento> list = getDocuments();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(doc.getId())) {
                list.remove(i);
                break;
            }
        }
        saveDocuments(list);
    }

    private void saveDocuments(List<Documento> list) {
        Set<String> set = new HashSet<>();
        for (Documento d : list) set.add(d.toString());
        docsPrefs.edit().putStringSet("docs_list", set).apply();
    }

    // --- APPOINTMENTS (CITAS) ---
    public List<Cita> getAppointments(boolean onlyUpcoming) {
        Set<String> set = citasPrefs.getStringSet("citas_set", new HashSet<>());
        List<Cita> list = new ArrayList<>();
        for (String s : set) {
            Cita c = Cita.fromString(s);
            if (c != null) {
                if (onlyUpcoming && !c.isAtendida()) list.add(c);
                else if (!onlyUpcoming) list.add(c);
            }
        }
        Collections.sort(list, Cita.DateComparator);
        return list;
    }

    public void saveAppointment(Cita cita) {
        Set<String> set = citasPrefs.getStringSet("citas_set", new HashSet<>());
        Set<String> newSet = new HashSet<>();
        boolean found = false;
        for (String s : set) {
            Cita existing = Cita.fromString(s);
            if (existing != null && existing.getId().equals(cita.getId())) {
                newSet.add(cita.toString());
                found = true;
            } else {
                newSet.add(s);
            }
        }
        if (!found) newSet.add(cita.toString());
        citasPrefs.edit().putStringSet("citas_set", newSet).apply();
    }

    public void deleteAppointment(String id) {
        Set<String> set = citasPrefs.getStringSet("citas_set", new HashSet<>());
        Set<String> newSet = new HashSet<>();
        for (String s : set) {
            Cita existing = Cita.fromString(s);
            if (existing != null && !existing.getId().equals(id)) {
                newSet.add(s);
            }
        }
        citasPrefs.edit().putStringSet("citas_set", newSet).apply();
    }
}
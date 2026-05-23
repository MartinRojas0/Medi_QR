package com.mediqr.android.models;

import java.io.Serializable;

public class PatientProfile implements Serializable {
    private String firstName;
    private String lastName;
    private String dni;
    private int age;
    private String email;
    private String phone;
    private String street;
    private String district;
    private String houseNum;
    
    // Health Data
    private String bloodType;
    private String allergies;
    private String chronicConditions;
    private String currentMedications;

    public PatientProfile() {
        // Default values for new profile
        this.firstName = "Martin";
        this.lastName = "Rojas";
        this.dni = "12.345.678";
        this.age = 20;
        this.email = "martin.rojas@email.com";
        this.phone = "987654321";
        this.street = "Av. Principal";
        this.district = "Lima";
        this.houseNum = "123";
        this.bloodType = "O+";
        this.allergies = "Ninguna conocida";
        this.chronicConditions = "Ninguna";
        this.currentMedications = "Ninguna";
    }

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return firstName + " " + lastName; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getHouseNum() { return houseNum; }
    public void setHouseNum(String houseNum) { this.houseNum = houseNum; }

    public String getFullAddress() { return street + " " + houseNum + ", " + district; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getChronicConditions() { return chronicConditions; }
    public void setChronicConditions(String chronicConditions) { this.chronicConditions = chronicConditions; }

    public String getCurrentMedications() { return currentMedications; }
    public void setCurrentMedications(String currentMedications) { this.currentMedications = currentMedications; }
}
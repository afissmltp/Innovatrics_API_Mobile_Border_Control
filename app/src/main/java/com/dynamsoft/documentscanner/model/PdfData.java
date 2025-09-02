package com.dynamsoft.documentscanner.model;

import android.graphics.Bitmap;

public class PdfData {
    private String name;
    private String surname;
    private String fullName;
    private String gender;
    private String dob; // date de naissance
    private String nationality;
    private String documentNumber;
    private String dateOfExpiry;
    private String issuingAuthority;
    private String docType;
    private String customerId;

    private Bitmap portraitImage;   // photo du visage
    private Bitmap documentImage;   // image du document (scan)

    // --- Constructeur vide ---
    public PdfData() {
    }

    // --- Getters et Setters ---

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
        updateFullName();
    }

    public String getSurname() {
        return surname;
    }
    public void setSurname(String surname) {
        this.surname = surname;
        updateFullName();
    }

    public String getFullName() {
        return fullName;
    }
    private void updateFullName() {
        if (name != null && surname != null) {
            this.fullName = name + " " + surname;
        } else if (name != null) {
            this.fullName = name;
        } else if (surname != null) {
            this.fullName = surname;
        } else {
            this.fullName = null;
        }
    }

    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDob() {
        return dob;
    }
    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getNationality() {
        return nationality;
    }
    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }
    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDateOfExpiry() {
        return dateOfExpiry;
    }
    public void setDateOfExpiry(String dateOfExpiry) {
        this.dateOfExpiry = dateOfExpiry;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }
    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public String getDocType() {
        return docType;
    }
    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getCustomerId() {
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Bitmap getPortraitImage() {
        return portraitImage;
    }
    public void setPortraitImage(Bitmap portraitImage) {
        this.portraitImage = portraitImage;
    }

    public Bitmap getDocumentImage() {
        return documentImage;
    }
    public void setDocumentImage(Bitmap documentImage) {
        this.documentImage = documentImage;
    }
}

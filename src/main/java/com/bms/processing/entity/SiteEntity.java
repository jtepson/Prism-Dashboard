package com.bms.processing.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "site")
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "facility_name")
    private String facilityName;

    private String address;

    @Column(name = "primary_contact")
    private String primaryContact;

    @Column(name = "transfer_method")
    private String transferMethod;

    @Column(name = "imeka_certified")
    private Boolean imekaCertified;

    @Column(name = "scanner_brand")
    private String scannerBrand;

    @Column(name = "magnet_strength")
    private String magnetStrength;

    public Long getId() {
        return id;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(String primaryContact) {
        this.primaryContact = primaryContact;
    }

    public String getTransferMethod() {
        return transferMethod;
    }

    public void setTransferMethod(String transferMethod) {
        this.transferMethod = transferMethod;
    }

    public Boolean getImekaCertified() {
        return imekaCertified;
    }

    public void setImekaCertified(Boolean imekaCertified) {
        this.imekaCertified = imekaCertified;
    }

    public String getScannerBrand() {
        return scannerBrand;
    }

    public void setScannerBrand(String scannerBrand) {
        this.scannerBrand = scannerBrand;
    }

    public String getMagnetStrength() {
        return magnetStrength;
    }

    public void setMagnetStrength(String magnetStrength) {
        this.magnetStrength = magnetStrength;
    }

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
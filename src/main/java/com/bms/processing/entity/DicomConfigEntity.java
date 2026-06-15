package com.bms.processing.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dicom_config")
public class DicomConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_name")
    private String configName;

    @Column(name = "remote_ae_title")
    private String remoteAeTitle;

    @Column(name = "remote_host")
    private String remoteHost;

    @Column(name = "remote_port")
    private Integer remotePort;

    @Column(name = "local_ae_title")
    private String localAeTitle;

    @Column(name = "retrieve_ae_title")
    private String retrieveAeTitle;

    @Column(name = "retrieve_port")
    private Integer retrievePort = 11113;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getRemoteAeTitle() {
        return remoteAeTitle;
    }

    public void setRemoteAeTitle(String remoteAeTitle) {
        this.remoteAeTitle = remoteAeTitle;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public Integer getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(Integer remotePort) {
        this.remotePort = remotePort;
    }

    public String getLocalAeTitle() {
        return localAeTitle;
    }

    public void setLocalAeTitle(String localAeTitle) {
        this.localAeTitle = localAeTitle;
    }

    public String getRetrieveAeTitle() {
        return retrieveAeTitle;
    }

    public void setRetrieveAeTitle(String retrieveAeTitle) {
        this.retrieveAeTitle = retrieveAeTitle;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getRetrievePort() {
        return retrievePort;
    }

    public void setRetrievePort(Integer retrievePort) {
        this.retrievePort = retrievePort;
    }
}
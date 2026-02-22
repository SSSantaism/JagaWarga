package com.example.jagawarga.data.model;

import com.google.firebase.Timestamp;

/**
 * POJO untuk data permintaan tukar jadwal dari Firestore collection
 * "tukar_jadwal".
 */
public class TukarJadwalRequest {
    private String id;
    private String dariId;
    private String dariNama;
    private String dariJadwalId;
    private String hariDari;
    private String kepadaId;
    private String kepadaNama;
    private String kepadaJadwalId;
    private String hariKepada;
    private String idRt;
    private String status;
    private Timestamp createdAt;

    public TukarJadwalRequest() {
    }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public String getDariId() {
        return dariId;
    }

    public String getDariNama() {
        return dariNama;
    }

    public String getDariJadwalId() {
        return dariJadwalId;
    }

    public String getHariDari() {
        return hariDari;
    }

    public String getKepadaId() {
        return kepadaId;
    }

    public String getKepadaNama() {
        return kepadaNama;
    }

    public String getKepadaJadwalId() {
        return kepadaJadwalId;
    }

    public String getHariKepada() {
        return hariKepada;
    }

    public String getIdRt() {
        return idRt;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    // --- Setters ---
    public void setId(String id) {
        this.id = id;
    }

    public void setDariId(String dariId) {
        this.dariId = dariId;
    }

    public void setDariNama(String dariNama) {
        this.dariNama = dariNama;
    }

    public void setDariJadwalId(String dariJadwalId) {
        this.dariJadwalId = dariJadwalId;
    }

    public void setHariDari(String hariDari) {
        this.hariDari = hariDari;
    }

    public void setKepadaId(String kepadaId) {
        this.kepadaId = kepadaId;
    }

    public void setKepadaNama(String kepadaNama) {
        this.kepadaNama = kepadaNama;
    }

    public void setKepadaJadwalId(String kepadaJadwalId) {
        this.kepadaJadwalId = kepadaJadwalId;
    }

    public void setHariKepada(String hariKepada) {
        this.hariKepada = hariKepada;
    }

    public void setIdRt(String idRt) {
        this.idRt = idRt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

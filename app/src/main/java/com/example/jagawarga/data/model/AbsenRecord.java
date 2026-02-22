package com.example.jagawarga.data.model;

/**
 * POJO untuk data absensi ronda dari Firestore collection "absensi".
 */
public class AbsenRecord {
    private String id;
    private String nama;
    private String idWarga;
    private String idRt;
    private String jadwalId;
    private String tanggal;
    private Object waktu;
    private String status;
    private String catatan;

    public AbsenRecord() {
    }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public String getIdWarga() {
        return idWarga;
    }

    public String getIdRt() {
        return idRt;
    }

    public String getJadwalId() {
        return jadwalId;
    }

    public String getTanggal() {
        return tanggal;
    }

    public Object getWaktu() {
        return waktu;
    }

    public String getStatus() {
        return status;
    }

    public String getCatatan() {
        return catatan;
    }

    // --- Setters ---
    public void setId(String id) {
        this.id = id;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public void setIdWarga(String idWarga) {
        this.idWarga = idWarga;
    }

    public void setIdRt(String idRt) {
        this.idRt = idRt;
    }

    public void setJadwalId(String jadwalId) {
        this.jadwalId = jadwalId;
    }

    public void setTanggal(String tanggal) {
        this.tanggal = tanggal;
    }

    public void setWaktu(Object waktu) {
        this.waktu = waktu;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }
}

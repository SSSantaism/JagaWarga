package com.example.jagawarga.data.model;

/**
 * POJO untuk data User dari Firestore collection "users".
 */
public class User {
    private String id;
    private String nama;
    private String telepon;
    private String idRt;
    private String role;
    private String statusWarga;
    private String jadwalHari;
    private String jadwalId;

    public User() {
    }

    public User(String id, String nama, String telepon, String idRt, String role,
            String statusWarga, String jadwalHari, String jadwalId) {
        this.id = id;
        this.nama = nama;
        this.telepon = telepon;
        this.idRt = idRt;
        this.role = role;
        this.statusWarga = statusWarga;
        this.jadwalHari = jadwalHari;
        this.jadwalId = jadwalId;
    }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public String getTelepon() {
        return telepon;
    }

    public String getIdRt() {
        return idRt;
    }

    public String getRole() {
        return role;
    }

    public String getStatusWarga() {
        return statusWarga;
    }

    public String getJadwalHari() {
        return jadwalHari;
    }

    public String getJadwalId() {
        return jadwalId;
    }

    // --- Setters ---
    public void setId(String id) {
        this.id = id;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public void setTelepon(String telepon) {
        this.telepon = telepon;
    }

    public void setIdRt(String idRt) {
        this.idRt = idRt;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setStatusWarga(String statusWarga) {
        this.statusWarga = statusWarga;
    }

    public void setJadwalHari(String jadwalHari) {
        this.jadwalHari = jadwalHari;
    }

    public void setJadwalId(String jadwalId) {
        this.jadwalId = jadwalId;
    }
}

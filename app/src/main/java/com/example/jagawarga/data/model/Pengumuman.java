package com.example.jagawarga.data.model;

import java.util.Date;

/**
 * POJO untuk data Pengumuman dari Firestore collection "pengumuman".
 */
public class Pengumuman {
    private String judul;
    private String isi;
    private Date tanggal;
    private String idRt;

    public Pengumuman() {
    }

    public Pengumuman(String judul, String isi, Date tanggal, String idRt) {
        this.judul = judul;
        this.isi = isi;
        this.tanggal = tanggal;
        this.idRt = idRt;
    }

    public String getJudul() {
        return judul;
    }

    public String getIsi() {
        return isi;
    }

    public Date getTanggal() {
        return tanggal;
    }

    public String getIdRt() {
        return idRt;
    }

    public void setJudul(String judul) {
        this.judul = judul;
    }

    public void setIsi(String isi) {
        this.isi = isi;
    }

    public void setTanggal(Date tanggal) {
        this.tanggal = tanggal;
    }

    public void setIdRt(String idRt) {
        this.idRt = idRt;
    }
}

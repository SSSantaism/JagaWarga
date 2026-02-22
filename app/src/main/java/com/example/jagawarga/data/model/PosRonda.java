package com.example.jagawarga.data.model;

/**
 * POJO untuk data Pos Ronda dari Firestore collection "data_rt".
 */
public class PosRonda {
    private String posPhone;
    private String lokasi;

    public PosRonda() {
    }

    public PosRonda(String posPhone, String lokasi) {
        this.posPhone = posPhone;
        this.lokasi = lokasi;
    }

    public String getPosPhone() {
        return posPhone;
    }

    public String getLokasi() {
        return lokasi;
    }

    public void setPosPhone(String posPhone) {
        this.posPhone = posPhone;
    }

    public void setLokasi(String lokasi) {
        this.lokasi = lokasi;
    }
}

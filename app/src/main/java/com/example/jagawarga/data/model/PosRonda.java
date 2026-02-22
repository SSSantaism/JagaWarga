package com.example.jagawarga.data.model;

/**
 * POJO untuk data Pos Ronda dari Firestore collection "data_rt".
 */
public class PosRonda {
    private String idRt;
    private String posPhone;
    private String lokasi;

    public PosRonda() {
    }

    public PosRonda(String posPhone, String lokasi) {
        this.posPhone = posPhone;
        this.lokasi = lokasi;
    }

    public PosRonda(String idRt, String posPhone, String lokasi) {
        this.idRt = idRt;
        this.posPhone = posPhone;
        this.lokasi = lokasi;
    }

    public String getIdRt() {
        return idRt;
    }

    public String getPosPhone() {
        return posPhone;
    }

    /** Alias untuk getPosPhone(), kompatibel dengan nama field Firestore. */
    public String getTelepon() {
        return posPhone;
    }

    public String getLokasi() {
        return lokasi;
    }

    public void setIdRt(String idRt) {
        this.idRt = idRt;
    }

    public void setPosPhone(String posPhone) {
        this.posPhone = posPhone;
    }

    public void setLokasi(String lokasi) {
        this.lokasi = lokasi;
    }
}

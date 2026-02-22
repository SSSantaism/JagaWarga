package com.example.jagawarga.data.model;

/**
 * POJO untuk data Laporan Keamanan dari Firestore collection "laporan".
 */
public class LaporanKeamanan {
    private String jenisLaporan;
    private String isiLaporan;
    private String namaPelapor;
    private String tanggal;
    private String idWarga;
    private String idRt;

    public LaporanKeamanan() {
    }

    public LaporanKeamanan(String jenisLaporan, String isiLaporan, String namaPelapor,
            String tanggal, String idWarga, String idRt) {
        this.jenisLaporan = jenisLaporan;
        this.isiLaporan = isiLaporan;
        this.namaPelapor = namaPelapor;
        this.tanggal = tanggal;
        this.idWarga = idWarga;
        this.idRt = idRt;
    }

    public String getJenisLaporan() {
        return jenisLaporan;
    }

    public String getIsiLaporan() {
        return isiLaporan;
    }

    public String getNamaPelapor() {
        return namaPelapor;
    }

    public String getTanggal() {
        return tanggal;
    }

    public String getIdWarga() {
        return idWarga;
    }

    public String getIdRt() {
        return idRt;
    }

    public void setJenisLaporan(String jenisLaporan) {
        this.jenisLaporan = jenisLaporan;
    }

    public void setIsiLaporan(String isiLaporan) {
        this.isiLaporan = isiLaporan;
    }

    public void setNamaPelapor(String namaPelapor) {
        this.namaPelapor = namaPelapor;
    }

    public void setTanggal(String tanggal) {
        this.tanggal = tanggal;
    }

    public void setIdWarga(String idWarga) {
        this.idWarga = idWarga;
    }

    public void setIdRt(String idRt) {
        this.idRt = idRt;
    }
}

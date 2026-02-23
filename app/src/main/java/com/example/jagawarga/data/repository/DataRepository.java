package com.example.jagawarga.data.repository;

import android.util.Log;

import com.example.jagawarga.data.model.PosRonda;
import com.example.jagawarga.utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository untuk data umum (Pengumuman, Pos Ronda, Absensi, Laporan, Jadwal).
 * Mengenkapsulasi operasi Firestore yang dipakai di Dashboard dan fitur warga.
 */
public class DataRepository {

    private static final String TAG = "DataRepository";
    private final FirebaseFirestore db;

    // ========================================================================
    // Callback Interfaces
    // ========================================================================

    public interface PosRondaCallback {
        void onSuccess(PosRonda posRonda);

        void onNotFound();

        void onError(String errorMessage);
    }

    public interface PengumumanCallback {
        void onSuccess(List<Map<String, Object>> dataList);

        void onError(String errorMessage);
    }

    public interface SimpleCallback {
        void onSuccess();

        void onError(String errorMessage);
    }

    public interface DayCheckCallback {
        void onMatch();

        void onMismatch(String hariIni, String jadwalHari);

        void onError(String errorMessage);
    }

    public interface JadwalListCallback {
        void onSuccess(List<JadwalItem> items);

        void onError(String errorMessage);
    }

    /** Data item untuk jadwal ronda. */
    public static class JadwalItem {
        public final String docId;
        public final String nama;
        public final String jadwalId;

        public JadwalItem(String docId, String nama, String jadwalId) {
            this.docId = docId;
            this.nama = nama;
            this.jadwalId = jadwalId;
        }
    }

    public DataRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ========================================================================
    // Pos Ronda
    // ========================================================================

    /**
     * Fetch data Pos Ronda dari collection data_rt berdasarkan idRt.
     */
    public void fetchPosRonda(String idRt, PosRondaCallback callback) {
        if (idRt == null || idRt.isEmpty()) {
            Log.w(TAG, "fetchPosRonda: idRt is null or empty!");
            callback.onNotFound();
            return;
        }

        Log.d(TAG, "fetchPosRonda: querying data_rt with idRt = '" + idRt + "'");

        db.collection(Constants.COLLECTION_DATA_RT).document(idRt)
                .get()
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "fetchPosRonda: doc.exists() = " + doc.exists());
                    if (doc.exists()) {
                        String phone = doc.getString(Constants.FIELD_POS_PHONE);
                        String lokasi = doc.getString(Constants.FIELD_LOKASI);
                        Log.d(TAG, "fetchPosRonda: phone = " + phone + ", lokasi = " + lokasi);
                        PosRonda posRonda = new PosRonda(idRt, phone, lokasi);
                        callback.onSuccess(posRonda);
                    } else {
                        Log.w(TAG, "fetchPosRonda: document NOT FOUND for idRt = '" + idRt + "'");
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching pos ronda: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ========================================================================
    // Pengumuman
    // ========================================================================

    /**
     * Listen daftar pengumuman terbaru (max 3), realtime.
     * Returns ListenerRegistration agar caller bisa detach.
     */
    public ListenerRegistration listenPengumuman(PengumumanCallback callback) {
        return db.collection(Constants.COLLECTION_PENGUMUMAN)
                .orderBy(Constants.FIELD_TANGGAL, Query.Direction.DESCENDING)
                .limit(3)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening pengumuman: " + error.getMessage());
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (snapshots == null)
                        return;

                    Log.d(TAG, "Pengumuman snapshot: " + snapshots.size() + " docs");
                    List<Map<String, Object>> dataList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        dataList.add(doc.getData());
                    }
                    callback.onSuccess(dataList);
                });
    }

    // ========================================================================
    // Absensi
    // ========================================================================

    /**
     * Verifikasi apakah hari ini adalah jadwal user.
     */
    public void verifyScheduleDay(String userId, String hariIni, DayCheckCallback callback) {
        db.collection(Constants.COLLECTION_USERS).document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String jadwalHari = doc.getString(Constants.FIELD_JADWAL_HARI);
                    if (jadwalHari != null && jadwalHari.equalsIgnoreCase(hariIni)) {
                        callback.onMatch();
                    } else {
                        callback.onMismatch(hariIni, jadwalHari);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error verifying schedule: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Submit data absensi ke Firestore.
     */
    public void submitAbsensi(Map<String, Object> absenData, SimpleCallback callback) {
        db.collection(Constants.COLLECTION_ABSENSI).add(absenData)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Absensi submitted: " + ref.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error submitting absensi: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ========================================================================
    // Laporan Keamanan
    // ========================================================================

    /**
     * Submit laporan keamanan ke Firestore.
     */
    public void submitLaporan(Map<String, Object> laporanData, SimpleCallback callback) {
        db.collection(Constants.COLLECTION_LAPORAN).add(laporanData)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Laporan submitted: " + ref.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error submitting laporan: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ========================================================================
    // Jadwal Ronda
    // ========================================================================

    /**
     * Ambil daftar warga yang bertugas ronda pada hari tertentu di RT tertentu.
     */
    public void getJadwalByDay(String idRt, String hari, JadwalListCallback callback) {
        if (idRt == null) {
            callback.onError("ID RT is null");
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo(Constants.FIELD_ID_RT, idRt)
                .whereEqualTo(Constants.FIELD_JADWAL_HARI, hari)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Jadwal query returned " + queryDocumentSnapshots.size() + " docs");
                    List<JadwalItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        items.add(new JadwalItem(
                                doc.getId(),
                                doc.getString(Constants.FIELD_NAMA),
                                doc.getString(Constants.FIELD_JADWAL_ID)));
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading jadwal: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }
}

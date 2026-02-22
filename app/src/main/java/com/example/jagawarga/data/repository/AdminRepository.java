package com.example.jagawarga.data.repository;

import android.util.Log;

import com.example.jagawarga.utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository untuk operasi admin RT/RW:
 * promote/revoke, buat pengumuman, terima laporan, list permintaan.
 */
public class AdminRepository {

    private static final String TAG = "AdminRepository";
    private final FirebaseFirestore db;

    // ========================================================================
    // Callback Interfaces
    // ========================================================================

    public interface SimpleCallback {
        void onSuccess();

        void onError(String errorMessage);
    }

    public interface FindUserCallback {
        void onFound(String uid);

        void onNotFound();

        void onError(String errorMessage);
    }

    public interface LaporanListCallback {
        void onSuccess(List<Map<String, Object>> dataList);

        void onEmpty();

        void onError(String errorMessage);
    }

    public interface PendingListCallback {
        void onSuccess(List<PendingItem> items);

        void onEmpty();

        void onError(String errorMessage);
    }

    public interface BalancedDayCallback {
        void onResult(String day);
    }

    /** Data item untuk pending register. */
    public static class PendingItem {
        public final String docId;
        public final String nama;
        public final String telepon;

        public PendingItem(String docId, String nama, String telepon) {
            this.docId = docId;
            this.nama = nama;
            this.telepon = telepon;
        }
    }

    /** Data item untuk pending absen. */
    public static class AbsenPendingItem {
        public final String docId;
        public final String nama;
        public final String waktu;

        public AbsenPendingItem(String docId, String nama, String waktu) {
            this.docId = docId;
            this.nama = nama;
            this.waktu = waktu;
        }
    }

    public interface AbsenPendingListCallback {
        void onSuccess(List<AbsenPendingItem> items);

        void onEmpty();

        void onError(String errorMessage);
    }

    public AdminRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ========================================================================
    // Role Management (Promote / Revoke)
    // ========================================================================

    /**
     * Cari user berdasarkan nomor telepon dan ubah role-nya.
     */
    public void changeUserRole(String phone, String newRole, FindUserCallback findCallback,
            SimpleCallback updateCallback) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo(Constants.FIELD_TELEPON, phone)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        findCallback.onNotFound();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snap) {
                        String uid = doc.getId();
                        findCallback.onFound(uid);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put(Constants.FIELD_ROLE, newRole);

                        db.collection(Constants.COLLECTION_USERS).document(uid)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> updateCallback.onSuccess())
                                .addOnFailureListener(e -> updateCallback.onError(e.getMessage()));
                        break;
                    }
                })
                .addOnFailureListener(e -> findCallback.onError(e.getMessage()));
    }

    // ========================================================================
    // Buat Pengumuman
    // ========================================================================

    /**
     * Submit pengumuman baru ke Firestore.
     */
    public void submitPengumuman(String judul, String isi, String idRt, SimpleCallback callback) {
        Map<String, Object> pengumuman = new HashMap<>();
        pengumuman.put(Constants.FIELD_JUDUL, judul);
        pengumuman.put(Constants.FIELD_ISI, isi);
        pengumuman.put(Constants.FIELD_ID_RT, idRt);
        pengumuman.put(Constants.FIELD_TANGGAL, Timestamp.now());

        db.collection(Constants.COLLECTION_PENGUMUMAN)
                .add(pengumuman)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Pengumuman submitted: " + ref.getId());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error submitting pengumuman: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ========================================================================
    // Terima Laporan
    // ========================================================================

    /**
     * Load laporan masuk untuk RT tertentu, urut tanggal descending.
     */
    public void loadLaporan(String idRt, LaporanListCallback callback) {
        db.collection(Constants.COLLECTION_LAPORAN)
                .whereEqualTo(Constants.FIELD_ID_RT, idRt)
                .orderBy(Constants.FIELD_TANGGAL, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        callback.onEmpty();
                        return;
                    }
                    List<Map<String, Object>> dataList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        dataList.add(doc.getData());
                    }
                    callback.onSuccess(dataList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading laporan: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    // ========================================================================
    // List Permintaan — Register
    // ========================================================================

    /**
     * Load pending register untuk RT tertentu.
     */
    public void loadPendingRegister(String idRt, PendingListCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo(Constants.FIELD_ID_RT, idRt)
                .whereEqualTo(Constants.FIELD_STATUS_WARGA, Constants.STATUS_PENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        callback.onEmpty();
                        return;
                    }
                    List<PendingItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        items.add(new PendingItem(
                                doc.getId(),
                                doc.getString(Constants.FIELD_NAMA),
                                doc.getString(Constants.FIELD_TELEPON)));
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Accept pending register: set status verified + jadwal.
     */
    public void acceptRegister(String idWarga, String idRt, SimpleCallback callback) {
        getBalancedDayForRt(idRt, balancedDay -> {
            String jadwalId = generateJadwalId(idRt);
            Map<String, Object> updates = new HashMap<>();
            updates.put(Constants.FIELD_STATUS_WARGA, Constants.STATUS_VERIFIED);
            updates.put(Constants.FIELD_JADWAL_HARI, balancedDay);
            updates.put(Constants.FIELD_JADWAL_ID, jadwalId);

            db.collection(Constants.COLLECTION_USERS).document(idWarga)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        });
    }

    /**
     * Reject pending register: delete user document.
     */
    public void rejectRegister(String idWarga, SimpleCallback callback) {
        db.collection(Constants.COLLECTION_USERS).document(idWarga)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ========================================================================
    // List Permintaan — Absen
    // ========================================================================

    /**
     * Load pending absen untuk RT tertentu.
     */
    public void loadPendingAbsen(String idRt, AbsenPendingListCallback callback) {
        db.collection(Constants.COLLECTION_ABSENSI)
                .whereEqualTo(Constants.FIELD_ID_RT, idRt)
                .whereEqualTo(Constants.FIELD_STATUS, Constants.STATUS_PENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        callback.onEmpty();
                        return;
                    }
                    List<AbsenPendingItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Object waktuObj = doc.get(Constants.FIELD_WAKTU);
                        items.add(new AbsenPendingItem(
                                doc.getId(),
                                doc.getString(Constants.FIELD_NAMA),
                                waktuObj != null ? waktuObj.toString() : "-"));
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Accept absen: set status verified.
     */
    public void acceptAbsen(String idAbsen, SimpleCallback callback) {
        db.collection(Constants.COLLECTION_ABSENSI).document(idAbsen)
                .update(Constants.FIELD_STATUS, Constants.STATUS_VERIFIED)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Reject absen: set status rejected.
     */
    public void rejectAbsen(String idAbsen, SimpleCallback callback) {
        db.collection(Constants.COLLECTION_ABSENSI).document(idAbsen)
                .update(Constants.FIELD_STATUS, Constants.STATUS_REJECTED)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private String generateJadwalId(String rt) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            randomPart.append(chars.charAt(idx));
        }
        return "JDW-" + rt + "-" + randomPart.toString();
    }

    private void getBalancedDayForRt(String rt, BalancedDayCallback callback) {
        String[] days = { "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu" };
        int[] counts = new int[7];
        AtomicInteger completed = new AtomicInteger(0);

        for (int i = 0; i < days.length; i++) {
            final int idx = i;
            db.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo(Constants.FIELD_ID_RT, rt)
                    .whereEqualTo(Constants.FIELD_JADWAL_HARI, days[idx])
                    .get()
                    .addOnSuccessListener(s -> {
                        counts[idx] = s.size();
                        if (completed.incrementAndGet() == 7) {
                            int min = 0;
                            for (int j = 1; j < 7; j++)
                                if (counts[j] < counts[min])
                                    min = j;
                            callback.onResult(days[min]);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (completed.incrementAndGet() == 7)
                            callback.onResult(days[(int) (Math.random() * days.length)]);
                    });
        }
    }

    /**
     * Format phone number ke +62.
     */
    public static String formatPhoneNumber(String phone) {
        if (phone.startsWith("0")) {
            return "+62" + phone.substring(1);
        } else if (!phone.startsWith("+")) {
            return "+62" + phone;
        }
        return phone;
    }
}

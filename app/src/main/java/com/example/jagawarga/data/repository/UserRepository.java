package com.example.jagawarga.data.repository;

import com.example.jagawarga.data.model.PosRonda;
import com.example.jagawarga.data.model.User;
import com.example.jagawarga.utils.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository untuk semua operasi Firestore terkait data User dan data_rt.
 * Mengenkapsulasi query user, CRUD profil, balanced day assignment, dan
 * PosRonda.
 */
public class UserRepository {

    private final FirebaseFirestore db;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ========================================================================
    // Callback Interfaces
    // ========================================================================

    public interface OnUserCallback {
        void onSuccess(User user);

        void onNotFound();

        void onError(Exception e);
    }

    public interface OnCompleteCallback {
        void onSuccess();

        void onError(Exception e);
    }

    public interface OnBalancedDayCallback {
        void onResult(String day);
    }

    public interface OnPosRondaCallback {
        void onSuccess(PosRonda posRonda);

        void onNotFound();

        void onError(Exception e);
    }

    // ========================================================================
    // User Operations
    // ========================================================================

    /**
     * Fetch user data dari Firestore berdasarkan UID.
     */
    public void getUserById(String uid, OnUserCallback callback) {
        db.collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            User user = new User(
                                    doc.getId(),
                                    doc.getString(Constants.FIELD_NAMA),
                                    doc.getString(Constants.FIELD_TELEPON),
                                    doc.getString(Constants.FIELD_ID_RT),
                                    doc.getString(Constants.FIELD_ROLE),
                                    doc.getString(Constants.FIELD_STATUS_WARGA),
                                    doc.getString(Constants.FIELD_JADWAL_HARI),
                                    doc.getString(Constants.FIELD_JADWAL_ID));
                            callback.onSuccess(user);
                        } else {
                            callback.onNotFound();
                        }
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    /**
     * Simpan data user baru ke Firestore (saat registrasi).
     * Status = pending, role = Warga, tanpa jadwal_hari/jadwal_id.
     */
    public void saveNewUser(String uid, String nama, String telepon, String idRt,
            OnCompleteCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put(Constants.FIELD_NAMA, nama);
        data.put(Constants.FIELD_TELEPON, telepon);
        data.put(Constants.FIELD_ID_RT, idRt);
        data.put(Constants.FIELD_ROLE, Constants.ROLE_WARGA);
        data.put(Constants.FIELD_STATUS_WARGA, Constants.STATUS_PENDING);
        data.put(Constants.FIELD_CREATED_AT, Timestamp.now());

        db.collection(Constants.COLLECTION_USERS).document(uid)
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Update fields tertentu pada document user.
     */
    public void updateUserFields(String uid, Map<String, Object> fields,
            OnCompleteCallback callback) {
        db.collection(Constants.COLLECTION_USERS).document(uid)
                .update(fields)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    // ========================================================================
    // Jadwal Assignment Helpers
    // ========================================================================

    /**
     * Cari hari dengan jumlah anggota ronda paling sedikit untuk RT tertentu.
     * Digunakan untuk balanced assignment jadwal warga baru.
     */
    public void getBalancedDayForRt(String rt, OnBalancedDayCallback callback) {
        String[] days = { "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu" };
        int[] counts = new int[7];
        AtomicInteger completed = new AtomicInteger(0);

        for (int i = 0; i < days.length; i++) {
            final int idx = i;
            db.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo(Constants.FIELD_ID_RT, rt)
                    .whereEqualTo(Constants.FIELD_JADWAL_HARI, days[idx])
                    .get()
                    .addOnSuccessListener(snap -> {
                        counts[idx] = snap.size();
                        if (completed.incrementAndGet() == 7) {
                            int min = 0;
                            for (int j = 1; j < 7; j++) {
                                if (counts[j] < counts[min])
                                    min = j;
                            }
                            callback.onResult(days[min]);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (completed.incrementAndGet() == 7) {
                            callback.onResult(days[(int) (Math.random() * days.length)]);
                        }
                    });
        }
    }

    /**
     * Generate unique jadwal ID: JDW-{RT}-{6 random alphanumeric chars}.
     */
    public String generateJadwalId(String rt) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            randomPart.append(chars.charAt(idx));
        }
        return "JDW-" + rt + "-" + randomPart.toString();
    }

    // ========================================================================
    // Pos Ronda (data_rt collection)
    // ========================================================================

    /**
     * Ambil data Pos Ronda untuk RT tertentu.
     */
    public void getPosRondaData(String idRt, OnPosRondaCallback callback) {
        db.collection(Constants.COLLECTION_DATA_RT).document(idRt)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        PosRonda posRonda = new PosRonda(
                                doc.getString(Constants.FIELD_POS_PHONE),
                                doc.getString(Constants.FIELD_LOKASI));
                        callback.onSuccess(posRonda);
                    } else {
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}

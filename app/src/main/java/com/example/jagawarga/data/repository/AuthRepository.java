package com.example.jagawarga.data.repository;

import androidx.annotation.NonNull;

import com.example.jagawarga.data.model.User;
import com.example.jagawarga.utils.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository untuk semua operasi Firebase Authentication dan login-related
 * Firestore.
 * Mengenkapsulasi login, register, role check, user profile save, dan jadwal
 * assignment.
 */
public class AuthRepository {

    private static final String EMAIL_DOMAIN = "@jagawarga.app";
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    // ========================================================================
    // Callback Interfaces
    // ========================================================================

    public interface AuthCallback {
        void onSuccess(String uid);

        void onError(String errorMessage);
    }

    public interface UserDataCallback {
        void onSuccess(User user);

        void onPending();

        void onRejected();

        void onNotFound();

        void onError(String errorMessage);
    }

    public interface SaveCallback {
        void onSuccess();

        void onError(String errorMessage);
    }

    private interface OnBalancedDayCallback {
        void onResult(String day);
    }

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    // ========================================================================
    // Auth Operations
    // ========================================================================

    /**
     * Login user dengan phone (dikonversi ke fake email) dan password.
     */
    public void login(String rawPhone, String password, AuthCallback callback) {
        String fakeEmail = createFakeEmail(rawPhone);
        auth.signInWithEmailAndPassword(fakeEmail, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        callback.onSuccess(auth.getCurrentUser().getUid());
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login gagal";
                        callback.onError(error);
                    }
                });
    }

    /**
     * Register user baru dengan phone (dikonversi ke fake email) dan password.
     */
    public void register(String rawPhone, String password, AuthCallback callback) {
        String fakeEmail = createFakeEmail(rawPhone);
        auth.createUserWithEmailAndPassword(fakeEmail, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        callback.onSuccess(auth.getCurrentUser().getUid());
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registrasi gagal";
                        callback.onError(error);
                    }
                });
    }

    /**
     * Logout user dari Firebase Auth.
     */
    public void logout() {
        auth.signOut();
    }

    /**
     * Get current Firebase user (null jika belum login).
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // ========================================================================
    // Firestore: Check User Role & Status
    // ========================================================================

    /**
     * Cek role & status user di Firestore saat login.
     * Juga auto-assign jadwal untuk user lama yang belum punya jadwal.
     */
    public void checkUserRole(String uid, UserDataCallback callback) {
        db.collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError("Gagal mengambil data pengguna");
                        return;
                    }

                    DocumentSnapshot doc = task.getResult();
                    if (!doc.exists()) {
                        callback.onNotFound();
                        return;
                    }

                    String role = doc.getString(Constants.FIELD_ROLE);
                    String nama = doc.getString(Constants.FIELD_NAMA);
                    String idRt = doc.getString(Constants.FIELD_ID_RT);
                    String jadwalHari = doc.getString(Constants.FIELD_JADWAL_HARI);
                    String jadwalId = doc.getString(Constants.FIELD_JADWAL_ID);
                    String statusWarga = doc.getString(Constants.FIELD_STATUS_WARGA);

                    // === CEK STATUS AKUN ===
                    if (Constants.ROLE_WARGA.equals(role)) {
                        if (statusWarga == null || Constants.STATUS_PENDING.equals(statusWarga)) {
                            auth.signOut();
                            callback.onPending();
                            return;
                        } else if (Constants.STATUS_REJECTED.equals(statusWarga)) {
                            auth.signOut();
                            callback.onRejected();
                            return;
                        }
                    }

                    // Cek apakah user lama tanpa jadwal
                    boolean needsJadwalHari = (jadwalHari == null || jadwalHari.isEmpty());
                    boolean needsJadwalId = (jadwalId == null || jadwalId.isEmpty());

                    if (needsJadwalHari || needsJadwalId) {
                        // Auto-assign jadwal untuk user lama
                        autoAssignJadwal(uid, idRt, jadwalHari, jadwalId,
                                needsJadwalHari, needsJadwalId,
                                (finalJadwalId, finalJadwalHari) -> {
                                    User user = new User(uid, nama, null, idRt, role,
                                            statusWarga, finalJadwalHari, finalJadwalId);
                                    callback.onSuccess(user);
                                });
                    } else {
                        User user = new User(uid, nama, null, idRt, role,
                                statusWarga, jadwalHari, jadwalId);
                        callback.onSuccess(user);
                    }
                });
    }

    // ========================================================================
    // Firestore: Save New User (Registration)
    // ========================================================================

    /**
     * Simpan data user baru ke Firestore setelah register Auth berhasil.
     * Status = pending, role = Warga, tanpa jadwal.
     */
    public void saveNewUser(String uid, String phone, String nama, String rt,
            SaveCallback callback) {
        String formattedPhone = formatPhoneNumber(phone);

        Map<String, Object> data = new HashMap<>();
        data.put(Constants.FIELD_NAMA, nama);
        data.put(Constants.FIELD_TELEPON, formattedPhone);
        data.put(Constants.FIELD_ID_RT, rt);
        data.put(Constants.FIELD_ROLE, Constants.ROLE_WARGA);
        data.put(Constants.FIELD_STATUS_WARGA, Constants.STATUS_PENDING);
        data.put(Constants.FIELD_CREATED_AT, Timestamp.now());

        db.collection(Constants.COLLECTION_USERS).document(uid)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    // Logout dari sesi register (karena createUser otomatis login)
                    auth.signOut();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ========================================================================
    // Phone/Email Helpers
    // ========================================================================

    public String createFakeEmail(String rawPhone) {
        String formatted = formatPhoneNumber(rawPhone);
        return formatted + EMAIL_DOMAIN;
    }

    public String formatPhoneNumber(String phone) {
        if (phone.startsWith("0")) {
            return "+62" + phone.substring(1);
        } else if (!phone.startsWith("+")) {
            return "+62" + phone;
        }
        return phone;
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    private interface OnJadwalAssignedCallback {
        void onAssigned(String jadwalId, String jadwalHari);
    }

    /**
     * Auto-assign jadwal untuk user lama yang belum punya jadwal.
     */
    private void autoAssignJadwal(String uid, String idRt,
            String existingJadwalHari, String existingJadwalId,
            boolean needsHari, boolean needsId,
            OnJadwalAssignedCallback callback) {
        getBalancedDayForRt(idRt, day -> {
            Map<String, Object> updates = new HashMap<>();
            String finalHari = existingJadwalHari;
            String finalId = existingJadwalId;

            if (needsHari) {
                updates.put(Constants.FIELD_JADWAL_HARI, day);
                finalHari = day;
            }
            if (needsId) {
                String newId = generateJadwalId(idRt);
                updates.put(Constants.FIELD_JADWAL_ID, newId);
                finalId = newId;
            }

            String resultHari = finalHari;
            String resultId = finalId;

            db.collection(Constants.COLLECTION_USERS).document(uid)
                    .update(updates)
                    .addOnSuccessListener(v -> callback.onAssigned(resultId, resultHari))
                    .addOnFailureListener(e -> {
                        // Even if update fails, continue with whatever we have
                        callback.onAssigned(resultId, resultHari);
                    });
        });
    }

    private void getBalancedDayForRt(String rt, OnBalancedDayCallback callback) {
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

    private String generateJadwalId(String rt) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            randomPart.append(chars.charAt(idx));
        }
        return "JDW-" + rt + "-" + randomPart.toString();
    }
}
